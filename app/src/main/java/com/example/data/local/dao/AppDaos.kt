package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUser(userId: String = "default_user"): Flow<UserProfileEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserDirect(userId: String = "default_user"): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserProfileEntity)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams WHERE active = 1")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    suspend fun getExamById(id: String): ExamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<ExamEntity>)
}

@Dao
interface CurriculumDao {
    @Query("SELECT * FROM subjects WHERE examId = :examId ORDER BY weight DESC")
    fun getSubjectsForExam(examId: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM topics WHERE subjectId = :subjectId")
    fun getTopicsForSubject(subjectId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM concepts WHERE topicId = :topicId")
    fun getConceptsForTopic(topicId: String): Flow<List<ConceptEntity>>

    @Query("SELECT * FROM concepts")
    fun getAllConcepts(): Flow<List<ConceptEntity>>

    @Query("SELECT * FROM concepts WHERE id = :conceptId LIMIT 1")
    suspend fun getConceptById(conceptId: String): ConceptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcepts(concepts: List<ConceptEntity>)
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    suspend fun getQuestionById(id: String): QuestionEntity?

    @Query("SELECT * FROM questions WHERE topicId = :topicId")
    fun getQuestionsByTopic(topicId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE examId = :examId")
    fun getQuestionsByExam(examId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun getQuestionsByIds(ids: List<String>): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE sourceType = 'verified_pyq'")
    fun getVerifiedPyqs(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomQuestions(limit: Int): List<QuestionEntity>

    @Query("SELECT * FROM questions WHERE examId = :examId AND difficulty = :difficulty ORDER BY RANDOM() LIMIT :limit")
    suspend fun getQuestionsByExamAndDifficulty(examId: String, difficulty: Int, limit: Int): List<QuestionEntity>

    @Query("SELECT * FROM questions")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)
}

@Dao
interface AttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: AttemptEntity)

    @Query("SELECT * FROM attempts ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAttempts(limit: Int = 50): Flow<List<AttemptEntity>>

    @Query("SELECT * FROM attempts WHERE questionId = :questionId ORDER BY timestamp DESC")
    suspend fun getAttemptsForQuestion(questionId: String): List<AttemptEntity>

    @Query("SELECT COUNT(*) FROM attempts")
    fun getTotalAttemptsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM attempts WHERE isCorrect = 1")
    fun getCorrectAttemptsCount(): Flow<Int>

    @Query("SELECT AVG(responseSeconds) FROM attempts")
    fun getAverageResponseSeconds(): Flow<Double?>
}

@Dao
interface MasteryDao {
    @Query("SELECT * FROM mastery")
    fun getAllMastery(): Flow<List<MasteryEntity>>

    @Query("SELECT * FROM mastery WHERE conceptId = :conceptId LIMIT 1")
    suspend fun getMasteryForConcept(conceptId: String): MasteryEntity?

    @Query("SELECT * FROM mastery WHERE conceptId = :conceptId LIMIT 1")
    fun observeMasteryForConcept(conceptId: String): Flow<MasteryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMastery(mastery: MasteryEntity)

    @Query("SELECT * FROM mastery WHERE conceptScore < 60 OR applicationScore < 60")
    fun getWeakMastery(): Flow<List<MasteryEntity>>

    @Query("SELECT * FROM mastery WHERE nextRevisionDate <= :currentTime AND nextRevisionDate > 0 ORDER BY nextRevisionDate ASC")
    fun getOverdueRevisions(currentTime: Long = System.currentTimeMillis()): Flow<List<MasteryEntity>>
}

@Dao
interface MistakeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMistake(mistake: MistakeEntity)

    @Query("SELECT * FROM mistakes ORDER BY repeatCount DESC, timestamp DESC")
    fun getAllMistakes(): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes ORDER BY repeatCount DESC, timestamp DESC")
    suspend fun getAllMistakesDirect(): List<MistakeEntity>

    @Query("SELECT * FROM mistakes WHERE repeatCount > 1 ORDER BY repeatCount DESC")
    fun getRepeatedMistakes(): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes WHERE repeatCount > 1 ORDER BY repeatCount DESC")
    suspend fun getRepeatedMistakesDirect(): List<MistakeEntity>

    @Query("SELECT * FROM mistakes WHERE mistakeType = :type ORDER BY timestamp DESC")
    fun getMistakesByType(type: String): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes WHERE id = :id LIMIT 1")
    suspend fun getMistakeById(id: String): MistakeEntity?

    @Query("SELECT * FROM mistakes WHERE questionId = :questionId LIMIT 1")
    suspend fun getMistakeByQuestionId(questionId: String): MistakeEntity?

    @Update
    suspend fun updateMistake(mistake: MistakeEntity)

    @Query("DELETE FROM mistakes WHERE id = :id")
    suspend fun deleteMistake(id: String)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE nextReview <= :currentTime ORDER BY nextReview ASC")
    fun getDueFlashcards(currentTime: Long = System.currentTimeMillis()): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards ORDER BY nextReview ASC")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE conceptId = :conceptId")
    fun getFlashcardsForConcept(conceptId: String): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>)

    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentById(id: String): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunks(chunks: List<DocumentChunkEntity>)

    @Query("SELECT * FROM document_chunks WHERE documentId = :documentId ORDER BY pageNumber ASC")
    suspend fun getChunksForDocument(documentId: String): List<DocumentChunkEntity>
}

@Dao
interface MockDao {
    @Query("SELECT * FROM mock_tests WHERE examId = :examId")
    fun getMockTestsForExam(examId: String): Flow<List<MockTestEntity>>

    @Query("SELECT * FROM mock_tests")
    fun getAllMockTests(): Flow<List<MockTestEntity>>

    @Query("SELECT * FROM mock_tests WHERE id = :id LIMIT 1")
    suspend fun getMockTestById(id: String): MockTestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMockTest(test: MockTestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMockAttempt(attempt: MockAttemptEntity)

    @Query("SELECT * FROM mock_attempts ORDER BY createdAt DESC")
    fun getAllMockAttempts(): Flow<List<MockAttemptEntity>>
}
