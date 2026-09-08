package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserProfileEntity(
    @PrimaryKey val id: String = "default_user",
    val name: String = "Exam Aspirant",
    val email: String = "student@examtutor.ai",
    val preferredLanguage: String = "hinglish", // english, hindi, hinglish
    val targetExamId: String = "ssc_cgl",
    val examDate: String = "2026-11-15",
    val dailyMinutesGoal: Int = 60,
    val targetScore: Int = 165,
    val explanationPreference: String = "intermediate", // beginner, intermediate, advanced
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String,
    val name: String,
    val version: String = "2026.1",
    val durationMinutes: Int,
    val marksPerCorrect: Double,
    val negativeMark: Double,
    val sectionsJson: String, // e.g. ["Quantitative", "Reasoning", "English", "General Awareness"]
    val active: Boolean = true
)

@Entity(
    tableName = "subjects",
    indices = [Index("examId")]
)
data class SubjectEntity(
    @PrimaryKey val id: String,
    val examId: String,
    val name: String,
    val weight: Double = 1.0
)

@Entity(
    tableName = "topics",
    indices = [Index("subjectId")]
)
data class TopicEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val parentTopicId: String? = null,
    val name: String
)

@Entity(
    tableName = "concepts",
    indices = [Index("topicId")]
)
data class ConceptEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val name: String,
    val description: String,
    val prerequisitesJson: String = "[]", // JSON List<String> of concept IDs
    val difficultyBand: Int = 2,
    val examRelevance: String = "High",
    val masteryRequirements: String = "Direct + Variation + Reverse + Mixed + Timed",
    val formulaSummary: String = "",
    val intuitionSnippet: String = ""
)

@Entity(
    tableName = "questions",
    indices = [Index("examId"), Index("topicId"), Index("difficulty")]
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val sourceType: String, // verified_pyq, user_upload, ai_generated, uncertain
    val documentId: String? = null,
    val examId: String,
    val subjectId: String,
    val topicId: String,
    val conceptIdsJson: String = "[]",
    val prerequisiteIdsJson: String = "[]",
    val difficulty: Int = 3, // 1..8
    val questionFamily: String = "Direct",
    val skill: String = "Application",
    val estimatedSeconds: Int = 60,
    val trap: String = "",
    val questionText: String,
    val optionsJson: String, // JSON List<String>
    val answer: String,
    val solution: String,
    val alternativeMethodsJson: String = "[]",
    val validationStatus: String = "passed", // passed, failed, pending
    val pyqExam: String? = null,
    val pyqYear: Int? = null,
    val pyqShift: String? = null,
    val uncertainFlag: Boolean = false,
    val uncertainWordsJson: String = "[]"
)

@Entity(
    tableName = "attempts",
    indices = [Index("questionId"), Index("userId")]
)
data class AttemptEntity(
    @PrimaryKey val id: String,
    val userId: String = "default_user",
    val questionId: String,
    val studentAnswer: String,
    val isCorrect: Boolean,
    val responseSeconds: Int,
    val hintsUsed: Int = 0,
    val confidence: Int = 3, // 1..5
    val errorType: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "mastery",
    indices = [Index("conceptId", unique = true)]
)
data class MasteryEntity(
    @PrimaryKey val conceptId: String,
    val userId: String = "default_user",
    val conceptScore: Int = 0,     // 0..100
    val applicationScore: Int = 0, // 0..100
    val transferScore: Int = 0,    // 0..100
    val speedScore: Int = 0,       // 0..100
    val retentionScore: Int = 0,   // 0..100
    val confidenceScore: Int = 0,  // 0..100
    val hintDependency: Int = 0,   // 0..100
    val streakCount: Int = 0,
    val lastPracticed: Long = 0L,
    val nextRevisionDate: Long = 0L,
    val revisionIntervalDays: Int = 1
)

@Entity(
    tableName = "mistakes",
    indices = [Index("attemptId"), Index("questionId")]
)
data class MistakeEntity(
    @PrimaryKey val id: String,
    val userId: String = "default_user",
    val attemptId: String,
    val questionId: String,
    val mistakeType: String, // one of 13 types
    val rootCause: String,
    val mentalModel: String,
    val howToAvoid: String,
    val repairStatus: String = "pending", // pending, repaired, retested
    val repeatCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "flashcards",
    indices = [Index("conceptId")]
)
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val conceptId: String,
    val cardType: String, // formula, definition, concept, exception, shortcut, common_mistake, question_pattern
    val front: String,
    val back: String,
    val nextReview: Long = System.currentTimeMillis(),
    val intervalDays: Int = 1,
    val easeFactor: Double = 2.5
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val userId: String = "default_user",
    val filename: String,
    val fileType: String, // pdf, image, camera
    val storagePath: String,
    val processingStatus: String = "processed", // uploaded, processing, processed, error
    val pageCount: Int = 1,
    val extractedQuestionsCount: Int = 0,
    val hasUncertainContent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "document_chunks",
    indices = [Index("documentId")]
)
data class DocumentChunkEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val pageNumber: Int,
    val text: String,
    val layoutMetadata: String = "text",
    val uncertainWordsJson: String = "[]"
)

@Entity(tableName = "mock_tests")
data class MockTestEntity(
    @PrimaryKey val id: String,
    val examId: String,
    val title: String,
    val durationMinutes: Int,
    val questionIdsJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "mock_attempts",
    indices = [Index("mockId")]
)
data class MockAttemptEntity(
    @PrimaryKey val id: String,
    val userId: String = "default_user",
    val mockId: String,
    val score: Double,
    val accuracy: Double,
    val timeUsedSeconds: Int,
    val totalQuestions: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val skippedCount: Int,
    val sectionMetricsJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
