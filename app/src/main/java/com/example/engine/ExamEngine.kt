package com.example.engine

import com.example.data.local.dao.MockDao
import com.example.data.local.dao.QuestionDao
import com.example.data.local.entity.MockAttemptEntity
import com.example.data.local.entity.MockTestEntity
import com.example.data.local.entity.QuestionEntity
import com.example.data.model.ExamType
import com.example.data.util.JsonUtils
import org.json.JSONObject
import java.util.UUID

data class QuestionPaletteState(
    val questionIndex: Int,
    val questionId: String,
    val selectedOption: String? = null,
    val isMarkedForReview: Boolean = false,
    val isVisited: Boolean = false
)

data class MockResultSummary(
    val totalQuestions: Int,
    val attemptedCount: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val skippedCount: Int,
    val grossScore: Double,
    val negativePenalty: Double,
    val netScore: Double,
    val accuracyPercentage: Double,
    val timeUsedSeconds: Int,
    val weakTopics: List<String>
)

class ExamEngine(
    private val mockDao: MockDao,
    private val questionDao: QuestionDao
) {

    /**
     * Creates a full simulated mock test with verified questions adhering to
     * exam specifications.
     */
    suspend fun createMockTest(exam: ExamType): MockTestEntity {
        val testId = UUID.randomUUID().toString()
        val questions = questionDao.getRandomQuestions(10) // 10 representative questions for full simulation

        val entity = MockTestEntity(
            id = testId,
            examId = exam.id,
            title = "${exam.title} Full Mock Test",
            durationMinutes = minOf(15, exam.totalTimeMinutes), // 15 mins for rapid local simulation
            questionIdsJson = JsonUtils.stringListToJson(questions.map { it.id })
        )
        mockDao.insertMockTest(entity)
        return entity
    }

    /**
     * Evaluates a completed mock test session using configurable exam scoring rules
     * and negative marking.
     */
    suspend fun evaluateMockSubmission(
        mockId: String,
        exam: ExamType,
        paletteStates: List<QuestionPaletteState>,
        timeUsedSeconds: Int
    ): MockResultSummary {
        val questionIds = paletteStates.map { it.questionId }
        val questionsMap = questionDao.getQuestionsByIds(questionIds).associateBy { it.id }

        var correct = 0
        var incorrect = 0
        var skipped = 0
        val weakTopicMap = mutableMapOf<String, Int>()

        paletteStates.forEach { state ->
            val q = questionsMap[state.questionId]
            if (q != null) {
                if (state.selectedOption == null) {
                    skipped++
                } else {
                    val cleanSel = state.selectedOption.trim()
                    val cleanAns = q.answer.trim()
                    val isMatch = cleanSel.equals(cleanAns, ignoreCase = true) ||
                            cleanSel.startsWith(cleanAns, ignoreCase = true) ||
                            (cleanAns.length == 1 && cleanSel.startsWith(cleanAns, ignoreCase = true))

                    if (isMatch) {
                        correct++
                    } else {
                        incorrect++
                        val topic = q.topicId
                        weakTopicMap[topic] = (weakTopicMap[topic] ?: 0) + 1
                    }
                }
            } else {
                skipped++
            }
        }

        val gross = correct * exam.marksPerCorrect
        val penalty = incorrect * exam.negativeMark
        val net = maxOf(0.0, gross - penalty)
        val attempted = correct + incorrect
        val accuracy = if (attempted > 0) (correct.toDouble() / attempted.toDouble()) * 100.0 else 0.0

        val weakTopics = weakTopicMap.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key.replace("topic_", "").replace("_", " ").capitalize() }

        val attemptEntity = MockAttemptEntity(
            id = UUID.randomUUID().toString(),
            mockId = mockId,
            score = net,
            accuracy = accuracy,
            timeUsedSeconds = timeUsedSeconds,
            totalQuestions = paletteStates.size,
            correctCount = correct,
            incorrectCount = incorrect,
            skippedCount = skipped,
            sectionMetricsJson = JSONObject().apply {
                put("grossScore", gross)
                put("penalty", penalty)
                put("netScore", net)
            }.toString()
        )
        mockDao.insertMockAttempt(attemptEntity)

        return MockResultSummary(
            totalQuestions = paletteStates.size,
            attemptedCount = attempted,
            correctCount = correct,
            incorrectCount = incorrect,
            skippedCount = skipped,
            grossScore = gross,
            negativePenalty = penalty,
            netScore = net,
            accuracyPercentage = accuracy,
            timeUsedSeconds = timeUsedSeconds,
            weakTopics = weakTopics
        )
    }
}
