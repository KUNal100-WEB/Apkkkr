package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.model.ExamType
import com.example.data.model.QuestionFamily
import com.example.data.util.JsonUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CurriculumSeed {

    suspend fun seedInitialDataIfEmpty(database: AppDatabase) = withContext(Dispatchers.IO) {
        val userDao = database.userDao()
        if (userDao.getUserDirect() != null) {
            return@withContext // already seeded
        }

        // 1. Seed User
        userDao.insertOrUpdateUser(
            UserProfileEntity(
                id = "default_user",
                name = "Aspirant Rahul",
                email = "rahul.aspirant@examtutor.ai",
                preferredLanguage = "hinglish",
                targetExamId = ExamType.SSC_CGL.id,
                examDate = "2026-11-20",
                dailyMinutesGoal = 60,
                targetScore = 165,
                explanationPreference = "intermediate"
            )
        )

        // 2. Seed Exams
        val examEntities = ExamType.values().map { exam ->
            ExamEntity(
                id = exam.id,
                name = exam.title,
                durationMinutes = exam.totalTimeMinutes,
                marksPerCorrect = exam.marksPerCorrect,
                negativeMark = exam.negativeMark,
                sectionsJson = JsonUtils.stringListToJson(
                    listOf("Quantitative Aptitude", "General Intelligence & Reasoning", "English", "General Awareness")
                )
            )
        }
        database.examDao().insertExams(examEntities)

        // 3. Seed Subjects
        val subjects = listOf(
            SubjectEntity("subject_quant", ExamType.SSC_CGL.id, "Quantitative Aptitude", 1.2),
            SubjectEntity("subject_reasoning", ExamType.SSC_CGL.id, "General Intelligence & Reasoning", 1.0),
            SubjectEntity("subject_english", ExamType.SSC_CGL.id, "English Comprehension", 1.0),
            SubjectEntity("subject_ga", ExamType.SSC_CGL.id, "General Awareness", 0.8)
        )
        database.curriculumDao().insertSubjects(subjects)

        // 4. Seed Topics
        val topics = listOf(
            TopicEntity("topic_ratio", "subject_quant", null, "Ratio & Proportion"),
            TopicEntity("topic_percentage", "subject_quant", null, "Percentage"),
            TopicEntity("topic_profit_loss", "subject_quant", null, "Profit, Loss & Discount"),
            TopicEntity("topic_time_work", "subject_quant", null, "Time & Work"),
            TopicEntity("topic_syllogism", "subject_reasoning", null, "Syllogism"),
            TopicEntity("topic_coding", "subject_reasoning", null, "Coding & Decoding")
        )
        database.curriculumDao().insertTopics(topics)

        // 5. Seed Concepts with Prerequisites (Knowledge Graph)
        val concepts = listOf(
            ConceptEntity(
                id = "concept_ratio_basics",
                topicId = "topic_ratio",
                name = "Ratio Basics & Simplification",
                description = "Understanding comparative parts, equivalent fractions, and simplifying A : B.",
                prerequisitesJson = "[]",
                difficultyBand = 2,
                examRelevance = "High",
                formulaSummary = "A : B = A / B",
                intuitionSnippet = "Ratio is just division in disguise: comparing parts of a whole."
            ),
            ConceptEntity(
                id = "concept_percentage_basics",
                topicId = "topic_percentage",
                name = "Percentage & Base Value",
                description = "Converting fractions to percentage, finding % of number, and identifying the true base value.",
                prerequisitesJson = JsonUtils.stringListToJson(listOf("concept_ratio_basics")),
                difficultyBand = 3,
                examRelevance = "Very High",
                formulaSummary = "% Value = (Part / Base) × 100",
                intuitionSnippet = "Percentage means 'per 100': scaling any ratio up to a common base of 100."
            ),
            ConceptEntity(
                id = "concept_product_constancy",
                topicId = "topic_percentage",
                name = "Product Constancy & Inverse Variation",
                description = "When A × B = Constant, percentage increase in A requires specific percentage decrease in B.",
                prerequisitesJson = JsonUtils.stringListToJson(listOf("concept_percentage_basics")),
                difficultyBand = 4,
                examRelevance = "Crucial for SSC/Railway",
                formulaSummary = "x/y increase => x/(x + y) decrease",
                intuitionSnippet = "Seesaw principle: Price up means Consumption down for fixed budget."
            ),
            ConceptEntity(
                id = "concept_profit_loss_basics",
                topicId = "topic_profit_loss",
                name = "Profit, Loss & Margin",
                description = "Cost Price (CP), Selling Price (SP), Profit % and Loss % on Cost Price.",
                prerequisitesJson = JsonUtils.stringListToJson(listOf("concept_percentage_basics")),
                difficultyBand = 3,
                examRelevance = "High",
                formulaSummary = "Profit% = (SP - CP)/CP × 100",
                intuitionSnippet = "CP is always your 100% baseline; SP is (100 + P)% or (100 - L)%."
            ),
            ConceptEntity(
                id = "concept_syllogism_venn",
                topicId = "topic_syllogism",
                name = "Syllogism Venn Diagrams",
                description = "All A are B, Some A are B, No A is B deduction rules.",
                prerequisitesJson = "[]",
                difficultyBand = 3,
                examRelevance = "Guaranteed 2-3 Qs",
                formulaSummary = "Venn intersection & subset rules",
                intuitionSnippet = "Look for definite truths vs mere possibilities."
            )
        )
        database.curriculumDao().insertConcepts(concepts)

        // 6. Seed Initial Questions (Verified PYQs & Exam Standard)
        val initialQuestions = listOf(
            QuestionEntity(
                id = "q_pyq_ssc_01",
                sourceType = "verified_pyq",
                examId = ExamType.SSC_CGL.id,
                subjectId = "subject_quant",
                topicId = "topic_ratio",
                conceptIdsJson = JsonUtils.stringListToJson(listOf("concept_ratio_basics")),
                difficulty = 3,
                questionFamily = QuestionFamily.DIRECT.title,
                skill = "Application",
                estimatedSeconds = 45,
                trap = "Watch out for adding without normalizing common term B",
                questionText = "If A : B = 2 : 3 and B : C = 4 : 5, then what is the ratio of A : B : C?",
                optionsJson = JsonUtils.stringListToJson(listOf("8 : 12 : 15", "6 : 8 : 10", "4 : 6 : 9", "8 : 10 : 15")),
                answer = "8 : 12 : 15",
                solution = "To combine A : B and B : C, make common term B equal:\nLCM of 3 and 4 is 12.\nA : B = (2 × 4) : (3 × 4) = 8 : 12\nB : C = (4 × 3) : (5 × 3) = 12 : 15\nHence, A : B : C = 8 : 12 : 15.",
                validationStatus = "passed",
                pyqExam = "SSC CGL",
                pyqYear = 2023,
                pyqShift = "Tier-1 Shift 2"
            ),
            QuestionEntity(
                id = "q_pyq_ssc_02",
                sourceType = "verified_pyq",
                examId = ExamType.SSC_CGL.id,
                subjectId = "subject_quant",
                topicId = "topic_percentage",
                conceptIdsJson = JsonUtils.stringListToJson(listOf("concept_product_constancy")),
                difficulty = 4,
                questionFamily = QuestionFamily.TRAP.title,
                skill = "Application",
                estimatedSeconds = 40,
                trap = "Confusing reduction on new price vs initial price",
                questionText = "If the price of sugar increases by 20%, by how much percentage should a household decrease its consumption so that expenditure remains the same?",
                optionsJson = JsonUtils.stringListToJson(listOf("16.66%", "20%", "25%", "15%")),
                answer = "16.66%",
                solution = "Price increases by 20% = +1/5.\nBy Product Constancy (Price × Consumption = Constant):\nRequired decrease in consumption = (1 / (5 + 1)) = 1/6.\n1/6 × 100% = 16.66% or 16 2/3%.",
                validationStatus = "passed",
                pyqExam = "SSC CGL",
                pyqYear = 2022,
                pyqShift = "Tier-1 Shift 1"
            ),
            QuestionEntity(
                id = "q_pyq_railway_01",
                sourceType = "verified_pyq",
                examId = ExamType.RAILWAY_NTPC.id,
                subjectId = "subject_quant",
                topicId = "topic_profit_loss",
                conceptIdsJson = JsonUtils.stringListToJson(listOf("concept_profit_loss_basics")),
                difficulty = 3,
                questionFamily = QuestionFamily.DIRECT.title,
                skill = "Application",
                estimatedSeconds = 50,
                trap = "Calculating profit on selling price instead of cost price",
                questionText = "A shopkeeper buys an article for ₹450 and sells it for ₹540. What is his profit percentage?",
                optionsJson = JsonUtils.stringListToJson(listOf("20%", "25%", "18%", "15%")),
                answer = "20%",
                solution = "Cost Price (CP) = ₹450, Selling Price (SP) = ₹540.\nProfit = SP - CP = 540 - 450 = ₹90.\nProfit % = (Profit / CP) × 100 = (90 / 450) × 100 = 20%.",
                validationStatus = "passed",
                pyqExam = "Railway RRB NTPC",
                pyqYear = 2021,
                pyqShift = "CBT-1 Phase 2"
            ),
            QuestionEntity(
                id = "q_pyq_banking_01",
                sourceType = "verified_pyq",
                examId = ExamType.BANKING_PO.id,
                subjectId = "subject_reasoning",
                topicId = "topic_syllogism",
                conceptIdsJson = JsonUtils.stringListToJson(listOf("concept_syllogism_venn")),
                difficulty = 4,
                questionFamily = QuestionFamily.STATEMENT.title,
                skill = "Reasoning",
                estimatedSeconds = 35,
                trap = "Assuming 'Some' means only some and not all",
                questionText = "Statements: All pens are pencils. Some pencils are erasers.\nConclusions:\nI. Some pens are erasers.\nII. Some pencils are pens.",
                optionsJson = JsonUtils.stringListToJson(listOf("Only II follows", "Only I follows", "Both I and II follow", "Neither follows")),
                answer = "Only II follows",
                solution = "1. 'All pens are pencils' directly implies 'Some pencils are pens' (Conclusion II is unconditionally TRUE).\n2. No direct overlap between pens and erasers is given, so Conclusion I is a mere possibility, not a definite conclusion.\nTherefore, Only II follows.",
                validationStatus = "passed",
                pyqExam = "IBPS PO",
                pyqYear = 2023,
                pyqShift = "Prelims Shift 3"
            )
        )
        database.questionDao().insertQuestions(initialQuestions)

        // 7. Seed Initial Mastery States
        val initialMastery = listOf(
            MasteryEntity(
                conceptId = "concept_ratio_basics",
                conceptScore = 75,
                applicationScore = 70,
                transferScore = 60,
                speedScore = 65,
                retentionScore = 70,
                confidenceScore = 80,
                hintDependency = 10,
                streakCount = 2,
                lastPracticed = System.currentTimeMillis() - 86400000L,
                nextRevisionDate = System.currentTimeMillis() + 86400000L,
                revisionIntervalDays = 3
            ),
            MasteryEntity(
                conceptId = "concept_percentage_basics",
                conceptScore = 65,
                applicationScore = 60,
                transferScore = 50,
                speedScore = 55,
                retentionScore = 60,
                confidenceScore = 70,
                hintDependency = 25,
                streakCount = 1,
                lastPracticed = System.currentTimeMillis() - 43200000L,
                nextRevisionDate = System.currentTimeMillis() + 86400000L,
                revisionIntervalDays = 1
            ),
            MasteryEntity(
                conceptId = "concept_product_constancy",
                conceptScore = 40,
                applicationScore = 35,
                transferScore = 20,
                speedScore = 40,
                retentionScore = 45,
                confidenceScore = 40,
                hintDependency = 55,
                streakCount = 0,
                lastPracticed = System.currentTimeMillis() - 21600000L,
                nextRevisionDate = System.currentTimeMillis() + 43200000L,
                revisionIntervalDays = 1
            )
        )
        initialMastery.forEach { database.masteryDao().upsertMastery(it) }

        // 8. Seed Initial Flashcards
        val initialCards = listOf(
            FlashcardEntity(
                id = "fc_01",
                conceptId = "concept_product_constancy",
                cardType = "formula",
                front = "Formula: Constant Expenditure Rule",
                back = "If Price increases by x/y, Consumption must decrease by x/(x + y).\nExample: 25% (+1/4) increase => 1/(1+4) = 1/5 (20%) decrease."
            ),
            FlashcardEntity(
                id = "fc_02",
                conceptId = "concept_ratio_basics",
                cardType = "shortcut",
                front = "Shortcut: Merging A:B & B:C",
                back = "A:B:C = (A × B₂) : (B₁ × B₂) : (B₁ × C).\nFor 2:3 and 4:5 => (2×4):(3×4):(3×5) = 8:12:15."
            )
        )
        database.flashcardDao().insertFlashcards(initialCards)
    }
}
