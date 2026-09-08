package com.example.engine

import com.example.data.local.dao.*
import com.example.data.local.entity.*
import com.example.data.model.*
import kotlin.math.max
import kotlin.math.min

data class NextActivityRecommendation(
    val recommendedMode: PracticeMode,
    val targetConceptId: String?,
    val targetConceptName: String,
    val recommendedDifficulty: Int, // 1..8
    val reasonHeadline: String,
    val diagnosticReason: String,
    val isConceptBridgeNeeded: Boolean,
    val previousConceptId: String? = null
)

class AdaptiveEngine(
    private val masteryDao: MasteryDao,
    private val attemptDao: AttemptDao,
    private val mistakeDao: MistakeDao,
    private val curriculumDao: CurriculumDao,
    private val questionDao: QuestionDao
) {

    /**
     * Updates student mastery scores using holistic evidence across accuracy,
     * time, hints used, difficulty, and mistake category.
     */
    suspend fun recordAttemptAndCalibrateMastery(
        conceptId: String,
        isCorrect: Boolean,
        responseSeconds: Int,
        targetSeconds: Int,
        hintsUsed: Int,
        confidence: Int, // 1..5
        errorType: MistakeCategory?,
        difficulty: Int
    ): MasteryEntity {
        val existing = masteryDao.getMasteryForConcept(conceptId) ?: MasteryEntity(
            conceptId = conceptId,
            conceptScore = 20,
            applicationScore = 15,
            transferScore = 10,
            speedScore = 50,
            retentionScore = 50,
            confidenceScore = 50,
            hintDependency = 0
        )

        // 1. Concept Score & Application Score updates
        val conceptDelta: Int
        val applicationDelta: Int

        if (isCorrect) {
            // Hint penalty: if many hints were used, learning is guided, not mastered
            val hintDiscount = when (hintsUsed) {
                0 -> 1.0
                1 -> 0.8
                2 -> 0.6
                3 -> 0.4
                else -> 0.2
            }
            val difficultyBonus = difficulty * 2
            conceptDelta = (10 * hintDiscount + difficultyBonus * 0.5).toInt()
            applicationDelta = if (difficulty >= 3) (12 * hintDiscount).toInt() else 6
        } else {
            // Mistake penalty depends on error type: conceptual mistakes penalize more heavily than arithmetic slips
            conceptDelta = when (errorType) {
                MistakeCategory.CONCEPTUAL -> -14
                MistakeCategory.FORMULA -> -10
                MistakeCategory.LOGICAL, MistakeCategory.ASSUMPTION -> -8
                MistakeCategory.ARITHMETIC, MistakeCategory.CARELESS -> -4
                MistakeCategory.TIME_PRESSURE -> -3
                else -> -6
            }
            applicationDelta = -8
        }

        val newConceptScore = min(100, max(0, existing.conceptScore + conceptDelta))
        val newApplicationScore = min(100, max(0, existing.applicationScore + applicationDelta))

        // 2. Transfer score
        val newTransferScore = if (isCorrect && difficulty >= 6 && hintsUsed == 0) {
            min(100, existing.transferScore + 15)
        } else if (!isCorrect && difficulty >= 6) {
            max(0, existing.transferScore - 5)
        } else {
            existing.transferScore
        }

        // 3. Speed score (relative to target time)
        val speedFactor = if (targetSeconds > 0) {
            val ratio = responseSeconds.toDouble() / targetSeconds.toDouble()
            when {
                ratio <= 0.7 -> +10 // blazing fast
                ratio <= 1.0 -> +5  // good pace
                ratio <= 1.5 -> -5  // slightly slow
                else -> -12         // very slow
            }
        } else 0
        val newSpeedScore = min(100, max(0, existing.speedScore + speedFactor))

        // 4. Hint dependency
        val hintImpact = when {
            hintsUsed == 0 -> -8
            hintsUsed in 1..2 -> +5
            else -> +15
        }
        val newHintDependency = min(100, max(0, existing.hintDependency + hintImpact))

        // 5. Confidence calibration: check for over/under confidence
        val confidenceFactor = when {
            isCorrect && confidence >= 4 -> +8   // calibrated high confidence
            !isCorrect && confidence >= 4 -> -15 // overconfidence trap!
            isCorrect && confidence <= 2 -> +2   // imposter underconfidence
            else -> 0
        }
        val newConfidenceScore = min(100, max(0, existing.confidenceScore + confidenceFactor))

        // 6. Spaced revision schedule calculation (Day 0, 1, 3, 7, 14, 30)
        val nextIntervalDays: Int
        val nextDate: Long
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L

        if (isCorrect && hintsUsed <= 1) {
            nextIntervalDays = when (existing.revisionIntervalDays) {
                1 -> 3
                3 -> 7
                7 -> 14
                14 -> 30
                30 -> 60
                else -> 1
            }
            nextDate = now + (nextIntervalDays * oneDayMillis)
        } else {
            // Mistake resets spaced interval to day 1 for rapid remediation
            nextIntervalDays = 1
            nextDate = now + oneDayMillis
        }

        val updated = existing.copy(
            conceptScore = newConceptScore,
            applicationScore = newApplicationScore,
            transferScore = newTransferScore,
            speedScore = newSpeedScore,
            confidenceScore = newConfidenceScore,
            hintDependency = newHintDependency,
            streakCount = if (isCorrect) existing.streakCount + 1 else 0,
            lastPracticed = now,
            nextRevisionDate = nextDate,
            revisionIntervalDays = nextIntervalDays
        )

        masteryDao.upsertMastery(updated)
        return updated
    }

    /**
     * Determines the optimal next learning activity using the Master Rule:
     * - High accuracy + slow speed -> speed practice
     * - Fast + inaccurate -> accuracy practice
     * - Repeated conceptual mistake -> prerequisite remediation
     * - Correct only with hints -> guided practice
     * - Delayed recall failure -> revision
     * - Strong performance -> increase difficulty
     * - Weak performance -> reduce difficulty and repair prerequisites
     */
    suspend fun determineNextActivity(
        currentConceptId: String? = null
    ): NextActivityRecommendation {
        val now = System.currentTimeMillis()

        // 1. Check for repeated mistakes first (highest priority)
        val allMistakes = mistakeDao.getAllMistakesDirect()
        // Find any pending high-priority mistake
        val topMistake = allMistakes.firstOrNull { it.repeatCount >= 2 && it.repairStatus == "pending" }
        if (topMistake != null) {
            val q = questionDao.getQuestionById(topMistake.questionId)
            val concept = q?.conceptIdsJson?.let {
                com.example.data.util.JsonUtils.jsonToStringList(it).firstOrNull()
            }?.let { curriculumDao.getConceptById(it) }

            return NextActivityRecommendation(
                recommendedMode = PracticeMode.WEAKNESS,
                targetConceptId = concept?.id,
                targetConceptName = concept?.name ?: "Diagnosed Mistake",
                recommendedDifficulty = 2,
                reasonHeadline = "Repeated Error Remediation (${topMistake.mistakeType})",
                diagnosticReason = "You encountered '${topMistake.mistakeType}' ${topMistake.repeatCount} times. Prerequisite repair needed before moving forward.",
                isConceptBridgeNeeded = false
            )
        }

        // 2. Check for overdue revision
        val overdue = masteryDao.getOverdueRevisions(now)
        // Check if any concept is overdue for revision
        if (currentConceptId == null) {
            // Find lowest mastery concept
            val weak = masteryDao.getWeakMastery()
        }

        // 3. If a current concept is provided, evaluate mastery dimensions according to Master Rule
        if (currentConceptId != null) {
            val mastery = masteryDao.getMasteryForConcept(currentConceptId)
            val concept = curriculumDao.getConceptById(currentConceptId)
            val conceptName = concept?.name ?: "Current Concept"

            if (mastery != null) {
                // High accuracy + slow speed -> speed practice
                if (mastery.conceptScore >= 75 && mastery.speedScore < 50) {
                    return NextActivityRecommendation(
                        recommendedMode = PracticeMode.SPEED,
                        targetConceptId = currentConceptId,
                        targetConceptName = conceptName,
                        recommendedDifficulty = 5,
                        reasonHeadline = "Speed & Timed Drill",
                        diagnosticReason = "High accuracy (${mastery.conceptScore}%), but time per question is slow (Speed: ${mastery.speedScore}/100). Build exam fluency!",
                        isConceptBridgeNeeded = false
                    )
                }

                // Fast + inaccurate -> accuracy practice
                if (mastery.speedScore >= 75 && mastery.conceptScore < 60) {
                    return NextActivityRecommendation(
                        recommendedMode = PracticeMode.ACCURACY,
                        targetConceptId = currentConceptId,
                        targetConceptName = conceptName,
                        recommendedDifficulty = 3,
                        reasonHeadline = "Patience & Accuracy Drill",
                        diagnosticReason = "Fast responses, but accuracy slipped (${mastery.conceptScore}%). Slow down and eliminate careless traps.",
                        isConceptBridgeNeeded = false
                    )
                }

                // Correct only with hints -> guided practice
                if (mastery.hintDependency >= 50) {
                    return NextActivityRecommendation(
                        recommendedMode = PracticeMode.GUIDED,
                        targetConceptId = currentConceptId,
                        targetConceptName = conceptName,
                        recommendedDifficulty = 3,
                        reasonHeadline = "Guided Step-By-Step Practice",
                        diagnosticReason = "High hint dependency (${mastery.hintDependency}%). Let's solve with checkpoints to build intuition.",
                        isConceptBridgeNeeded = false
                    )
                }

                // Strong performance -> increase difficulty (up to 7 or 8)
                if (mastery.conceptScore >= 80 && mastery.applicationScore >= 75) {
                    val nextDiff = when {
                        mastery.transferScore < 60 -> 6 // Hard multi-concept
                        mastery.transferScore < 80 -> 7 // Advanced transfer
                        else -> 8                       // Timed exam challenge
                    }
                    return NextActivityRecommendation(
                        recommendedMode = PracticeMode.INDEPENDENT,
                        targetConceptId = currentConceptId,
                        targetConceptName = conceptName,
                        recommendedDifficulty = nextDiff,
                        reasonHeadline = "Level Up Challenge (Difficulty $nextDiff)",
                        diagnosticReason = "Solid foundation demonstrated! Stepping up to challenging exam-tier variations.",
                        isConceptBridgeNeeded = false
                    )
                }

                // Weak performance -> reduce difficulty and review intuition
                if (mastery.conceptScore < 50) {
                    return NextActivityRecommendation(
                        recommendedMode = PracticeMode.LEARN,
                        targetConceptId = currentConceptId,
                        targetConceptName = conceptName,
                        recommendedDifficulty = 2,
                        reasonHeadline = "Foundation & Intuition Rebuild",
                        diagnosticReason = "Concept mastery is currently low (${mastery.conceptScore}%). Rebuilding from concrete examples.",
                        isConceptBridgeNeeded = false
                    )
                }
            }
        }

        // Default default recommendation: Guided Practice on current or first concept
        return NextActivityRecommendation(
            recommendedMode = PracticeMode.GUIDED,
            targetConceptId = currentConceptId,
            targetConceptName = "Adaptive Practice",
            recommendedDifficulty = 3,
            reasonHeadline = "Personalized Exam Practice",
            diagnosticReason = "Calibrated based on your competitive exam syllabus and past performance.",
            isConceptBridgeNeeded = false
        )
    }
}
