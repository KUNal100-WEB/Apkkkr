package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.ai.AIProvider
import com.example.ai.GeminiAIProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.model.*
import com.example.engine.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AppRepository(
    val database: AppDatabase,
    val aiProvider: AIProvider,
    val context: Context
) {
    val adaptiveEngine = AdaptiveEngine(
        masteryDao = database.masteryDao(),
        attemptDao = database.attemptDao(),
        mistakeDao = database.mistakeDao(),
        curriculumDao = database.curriculumDao(),
        questionDao = database.questionDao()
    )

    val questionEngine = QuestionEngine(
        aiProvider = aiProvider,
        questionDao = database.questionDao()
    )

    val tutorEngine = TutorEngine(
        aiProvider = aiProvider
    )

    val mistakeEngine = MistakeEngine(
        aiProvider = aiProvider,
        mistakeDao = database.mistakeDao(),
        questionDao = database.questionDao()
    )

    val revisionEngine = RevisionEngine(
        masteryDao = database.masteryDao(),
        mistakeDao = database.mistakeDao(),
        flashcardDao = database.flashcardDao(),
        curriculumDao = database.curriculumDao(),
        aiProvider = aiProvider
    )

    val documentEngine = DocumentIntelligenceEngine(
        aiProvider = aiProvider,
        documentDao = database.documentDao(),
        questionDao = database.questionDao(),
        context = context
    )

    val examEngine = ExamEngine(
        mockDao = database.mockDao(),
        questionDao = database.questionDao()
    )

    // User Profile
    fun getUserProfile(): Flow<UserProfileEntity?> = database.userDao().getUser()

    suspend fun updateUserProfile(user: UserProfileEntity) {
        database.userDao().insertOrUpdateUser(user)
    }

    // Curriculum
    fun getSubjectsForExam(examId: String): Flow<List<SubjectEntity>> =
        database.curriculumDao().getSubjectsForExam(examId)

    fun getTopicsForSubject(subjectId: String): Flow<List<TopicEntity>> =
        database.curriculumDao().getTopicsForSubject(subjectId)

    fun getConceptsForTopic(topicId: String): Flow<List<ConceptEntity>> =
        database.curriculumDao().getConceptsForTopic(topicId)

    fun getAllConcepts(): Flow<List<ConceptEntity>> =
        database.curriculumDao().getAllConcepts()

    suspend fun getConceptById(conceptId: String): ConceptEntity? =
        database.curriculumDao().getConceptById(conceptId)

    // Questions
    fun getAllQuestions(): Flow<List<QuestionEntity>> = database.questionDao().getAllQuestions()

    fun getVerifiedPyqs(): Flow<List<QuestionEntity>> = database.questionDao().getVerifiedPyqs()

    suspend fun getQuestionById(id: String): QuestionEntity? = database.questionDao().getQuestionById(id)

    suspend fun getRandomQuestions(limit: Int): List<QuestionEntity> = database.questionDao().getRandomQuestions(limit)

    // Attempts & Mastery
    fun getTotalAttempts(): Flow<Int> = database.attemptDao().getTotalAttemptsCount()
    fun getCorrectAttempts(): Flow<Int> = database.attemptDao().getCorrectAttemptsCount()
    fun getAverageResponseSeconds(): Flow<Double?> = database.attemptDao().getAverageResponseSeconds()
    fun getAllMastery(): Flow<List<MasteryEntity>> = database.masteryDao().getAllMastery()
    fun getWeakMastery(): Flow<List<MasteryEntity>> = database.masteryDao().getWeakMastery()

    suspend fun recordAttempt(
        question: QuestionEntity,
        studentAnswer: String,
        isCorrect: Boolean,
        responseSeconds: Int,
        hintsUsed: Int,
        confidence: Int,
        errorType: MistakeCategory?,
        language: String
    ): Pair<MasteryEntity, MistakeDiagnosis?> {
        val attemptId = UUID.randomUUID().toString()
        val attemptEntity = AttemptEntity(
            id = attemptId,
            questionId = question.id,
            studentAnswer = studentAnswer,
            isCorrect = isCorrect,
            responseSeconds = responseSeconds,
            hintsUsed = hintsUsed,
            confidence = confidence,
            errorType = errorType?.code
        )
        database.attemptDao().insertAttempt(attemptEntity)

        val conceptId = com.example.data.util.JsonUtils.jsonToStringList(question.conceptIdsJson).firstOrNull() ?: "general"

        val updatedMastery = adaptiveEngine.recordAttemptAndCalibrateMastery(
            conceptId = conceptId,
            isCorrect = isCorrect,
            responseSeconds = responseSeconds,
            targetSeconds = question.estimatedSeconds,
            hintsUsed = hintsUsed,
            confidence = confidence,
            errorType = errorType,
            difficulty = question.difficulty
        )

        var diagnosis: MistakeDiagnosis? = null
        if (!isCorrect) {
            diagnosis = mistakeEngine.diagnoseAndRecordMistake(
                attemptId = attemptId,
                question = question,
                studentAnswer = studentAnswer,
                responseSeconds = responseSeconds,
                language = language
            )
        }

        return Pair(updatedMastery, diagnosis)
    }

    // Mistakes
    fun getAllMistakes(): Flow<List<MistakeEntity>> = database.mistakeDao().getAllMistakes()
    fun getRepeatedMistakes(): Flow<List<MistakeEntity>> = database.mistakeDao().getRepeatedMistakes()

    // Flashcards
    fun getAllFlashcards(): Flow<List<FlashcardEntity>> = database.flashcardDao().getAllFlashcards()

    // Documents
    fun getAllDocuments(): Flow<List<DocumentEntity>> = database.documentDao().getAllDocuments()

    // Mock
    fun getAllMockAttempts(): Flow<List<MockAttemptEntity>> = database.mockDao().getAllMockAttempts()

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val ai = GeminiAIProvider()
                val instance = AppRepository(db, ai, context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
