package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.model.*
import com.example.data.repository.AppRepository
import com.example.data.repository.CurriculumSeed
import com.example.engine.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ActiveQuestionState(
    val question: QuestionEntity,
    val selectedOption: String? = null,
    val isAnswerChecked: Boolean = false,
    val isCorrect: Boolean = false,
    val currentHintLevel: Int = 0, // 0 = no hints, 1..6
    val confidence: Int = 3,
    val startTimestamp: Long = System.currentTimeMillis(),
    val responseSeconds: Int = 0,
    val isSolveWithMeActive: Boolean = false,
    val solveWithMeSteps: List<SolverStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val diagnosis: MistakeDiagnosis? = null,
    val isRepairQuestion: Boolean = false
)

data class ActiveMockState(
    val mockTest: MockTestEntity,
    val questions: List<QuestionEntity>,
    val paletteStates: List<QuestionPaletteState>,
    val currentIndex: Int = 0,
    val remainingSeconds: Int = 900,
    val isSubmitted: Boolean = false,
    val resultSummary: MockResultSummary? = null
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository.getInstance(application)

    // User Profile
    val userProfile: StateFlow<UserProfileEntity?> = repository.getUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Curriculum
    val subjects: StateFlow<List<SubjectEntity>> = userProfile
        .flatMapLatest { user ->
            val examId = user?.targetExamId ?: ExamType.SSC_CGL.id
            repository.getSubjectsForExam(examId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allConcepts: StateFlow<List<ConceptEntity>> = repository.getAllConcepts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMastery: StateFlow<List<MasteryEntity>> = repository.getAllMastery()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMistakes: StateFlow<List<MistakeEntity>> = repository.getAllMistakes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val verifiedPyqs: StateFlow<List<QuestionEntity>> = repository.getVerifiedPyqs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQuestions: StateFlow<List<QuestionEntity>> = repository.getAllQuestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashcards: StateFlow<List<FlashcardEntity>> = repository.getAllFlashcards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<DocumentEntity>> = repository.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mockAttempts: StateFlow<List<MockAttemptEntity>> = repository.getAllMockAttempts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Overall metrics
    val totalAttemptsCount = repository.getTotalAttempts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val correctAttemptsCount = repository.getCorrectAttempts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val averageResponseSeconds = repository.getAverageResponseSeconds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 45.0)

    // Active learning session state
    private val _activeQuestion = MutableStateFlow<ActiveQuestionState?>(null)
    val activeQuestion: StateFlow<ActiveQuestionState?> = _activeQuestion.asStateFlow()

    // Concept Bridge state
    private val _activeConceptBridge = MutableStateFlow<ConceptBridgeData?>(null)
    val activeConceptBridge: StateFlow<ConceptBridgeData?> = _activeConceptBridge.asStateFlow()

    // Next activity recommendation
    private val _nextActivity = MutableStateFlow<NextActivityRecommendation?>(null)
    val nextActivity: StateFlow<NextActivityRecommendation?> = _nextActivity.asStateFlow()

    // Active Mock Test state
    private val _activeMock = MutableStateFlow<ActiveMockState?>(null)
    val activeMock: StateFlow<ActiveMockState?> = _activeMock.asStateFlow()
    private var mockTimerJob: Job? = null

    // Document Scanner / OCR State
    private val _ocrProcessing = MutableStateFlow(false)
    val ocrProcessing: StateFlow<Boolean> = _ocrProcessing.asStateFlow()

    private val _ocrUncertainWords = MutableStateFlow<List<String>>(emptyList())
    val ocrUncertainWords: StateFlow<List<String>> = _ocrUncertainWords.asStateFlow()

    private val _tutorMessage = MutableStateFlow<String?>(null)
    val tutorMessage: StateFlow<String?> = _tutorMessage.asStateFlow()

    private var previousConceptId: String? = null

    init {
        viewModelScope.launch {
            CurriculumSeed.seedInitialDataIfEmpty(com.example.data.local.AppDatabase.getInstance(application))
            refreshNextActivityRecommendation()
        }
    }

    fun refreshNextActivityRecommendation() {
        viewModelScope.launch {
            val recommendation = repository.adaptiveEngine.determineNextActivity(previousConceptId)
            _nextActivity.value = recommendation
        }
    }

    /**
     * Starts practicing a question or concept, triggering Concept Bridge if concept transitioned.
     */
    fun startConceptPractice(concept: ConceptEntity, forceMode: PracticeMode = PracticeMode.GUIDED) {
        viewModelScope.launch {
            val language = userProfile.value?.preferredLanguage ?: "hinglish"

            // Check if concept transitioned
            if (previousConceptId != null && previousConceptId != concept.id) {
                val oldConcept = repository.getConceptById(previousConceptId!!)
                if (oldConcept != null) {
                    val bridge = repository.tutorEngine.generateConceptBridge(oldConcept, concept, language)
                    _activeConceptBridge.value = bridge
                }
            }
            previousConceptId = concept.id

            // Fetch or generate question
            val examId = userProfile.value?.targetExamId ?: ExamType.SSC_CGL.id
            val questions = repository.questionEngine.generateAndValidateQuestion(
                examId = examId,
                subjectId = "subject_quant",
                topicId = concept.topicId,
                conceptId = concept.id,
                conceptName = concept.name,
                difficulty = DifficultyLevel.BASIC,
                family = QuestionFamily.DIRECT,
                language = language
            )

            val q = questions.getOrNull() ?: repository.getAllQuestions().first().firstOrNull()
            if (q != null) {
                loadQuestion(q)
            }
        }
    }

    fun dismissConceptBridge() {
        _activeConceptBridge.value = null
    }

    fun loadQuestion(question: QuestionEntity, isRepair: Boolean = false) {
        _activeQuestion.value = ActiveQuestionState(
            question = question,
            isRepairQuestion = isRepair
        )
    }

    fun selectOption(option: String) {
        _activeQuestion.value = _activeQuestion.value?.copy(selectedOption = option)
    }

    fun setConfidence(confidence: Int) {
        _activeQuestion.value = _activeQuestion.value?.copy(confidence = confidence)
    }

    fun unlockNextHint() {
        val current = _activeQuestion.value ?: return
        if (current.currentHintLevel < 6) {
            _activeQuestion.value = current.copy(currentHintLevel = current.currentHintLevel + 1)
        }
    }

    fun startSolveWithMe() {
        val current = _activeQuestion.value ?: return
        viewModelScope.launch {
            val steps = repository.tutorEngine.generateSolveWithMeSteps(
                current.question,
                userProfile.value?.preferredLanguage ?: "hinglish"
            )
            _activeQuestion.value = current.copy(
                isSolveWithMeActive = true,
                solveWithMeSteps = steps,
                currentStepIndex = 0
            )
        }
    }

    fun advanceSolveWithMeStep() {
        val current = _activeQuestion.value ?: return
        val nextIdx = current.currentStepIndex + 1
        if (nextIdx < current.solveWithMeSteps.size) {
            _activeQuestion.value = current.copy(currentStepIndex = nextIdx)
        } else {
            // Completed all steps
            _activeQuestion.value = current.copy(
                selectedOption = current.question.answer,
                isAnswerChecked = true,
                isCorrect = true
            )
            submitCurrentAnswer()
        }
    }

    fun submitCurrentAnswer() {
        val current = _activeQuestion.value ?: return
        if (current.selectedOption == null) return

        val responseSeconds = ((System.currentTimeMillis() - current.startTimestamp) / 1000).toInt().coerceAtLeast(5)
        val cleanSelected = current.selectedOption.trim()
        val cleanAnswer = current.question.answer.trim()
        val isCorrect = cleanSelected.equals(cleanAnswer, ignoreCase = true) ||
                cleanSelected.startsWith(cleanAnswer, ignoreCase = true) ||
                (cleanAnswer.length == 1 && cleanSelected.startsWith(cleanAnswer, ignoreCase = true))

        viewModelScope.launch {
            val (updatedMastery, diagnosis) = repository.recordAttempt(
                question = current.question,
                studentAnswer = current.selectedOption,
                isCorrect = isCorrect,
                responseSeconds = responseSeconds,
                hintsUsed = current.currentHintLevel,
                confidence = current.confidence,
                errorType = if (isCorrect) null else MistakeCategory.CONCEPTUAL,
                language = userProfile.value?.preferredLanguage ?: "hinglish"
            )

            _activeQuestion.value = current.copy(
                isAnswerChecked = true,
                isCorrect = isCorrect,
                responseSeconds = responseSeconds,
                diagnosis = diagnosis
            )

            refreshNextActivityRecommendation()
        }
    }

    fun askTutorWhy(studentQuestion: String) {
        val current = _activeQuestion.value ?: return
        viewModelScope.launch {
            _tutorMessage.value = "Tutor is analyzing your question..."
            val explanation = repository.tutorEngine.explainWhy(
                questionText = current.question.questionText,
                solutionContext = current.question.solution,
                studentQuery = studentQuestion,
                language = userProfile.value?.preferredLanguage ?: "hinglish"
            )
            _tutorMessage.value = explanation
        }
    }

    fun clearTutorMessage() {
        _tutorMessage.value = null
    }

    // Document & Image Scan
    fun processScannedImage(uri: Uri) {
        viewModelScope.launch {
            _ocrProcessing.value = true
            val examId = userProfile.value?.targetExamId ?: ExamType.SSC_CGL.id
            val result = repository.documentEngine.processImageDocument(uri, examId = examId)
            _ocrProcessing.value = false

            if (result.isSuccess) {
                val data = result.getOrThrow()
                _ocrUncertainWords.value = data.uncertainWords
                val q = data.extractedQuestions.firstOrNull()
                if (q != null) {
                    loadQuestion(q)
                }
            }
        }
    }

    fun dismissOcrUncertaintyBanner() {
        _ocrUncertainWords.value = emptyList()
    }

    // Full Mock Exam Simulation
    fun startMockExam(examType: ExamType) {
        viewModelScope.launch {
            val mockEntity = repository.examEngine.createMockTest(examType)
            val questionIds = com.example.data.util.JsonUtils.jsonToStringList(mockEntity.questionIdsJson)
            val questions = repository.database.questionDao().getQuestionsByIds(questionIds)

            val initialPalette = questions.mapIndexed { index, q ->
                QuestionPaletteState(
                    questionIndex = index,
                    questionId = q.id,
                    isVisited = index == 0
                )
            }

            _activeMock.value = ActiveMockState(
                mockTest = mockEntity,
                questions = questions,
                paletteStates = initialPalette,
                remainingSeconds = mockEntity.durationMinutes * 60
            )

            startMockTimer()
        }
    }

    private fun startMockTimer() {
        mockTimerJob?.cancel()
        mockTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _activeMock.value ?: break
                if (current.remainingSeconds <= 1) {
                    submitMockTest()
                    break
                }
                _activeMock.value = current.copy(remainingSeconds = current.remainingSeconds - 1)
            }
        }
    }

    fun selectMockOption(option: String) {
        val current = _activeMock.value ?: return
        val currentIdx = current.currentIndex
        val updatedPalette = current.paletteStates.toMutableList()
        val existing = updatedPalette[currentIdx]
        updatedPalette[currentIdx] = existing.copy(selectedOption = option, isVisited = true)

        _activeMock.value = current.copy(paletteStates = updatedPalette)
    }

    fun toggleMockMarkForReview() {
        val current = _activeMock.value ?: return
        val currentIdx = current.currentIndex
        val updatedPalette = current.paletteStates.toMutableList()
        val existing = updatedPalette[currentIdx]
        updatedPalette[currentIdx] = existing.copy(isMarkedForReview = !existing.isMarkedForReview)

        _activeMock.value = current.copy(paletteStates = updatedPalette)
    }

    fun navigateMockQuestion(targetIndex: Int) {
        val current = _activeMock.value ?: return
        if (targetIndex in current.questions.indices) {
            val updatedPalette = current.paletteStates.toMutableList()
            updatedPalette[targetIndex] = updatedPalette[targetIndex].copy(isVisited = true)
            _activeMock.value = current.copy(currentIndex = targetIndex, paletteStates = updatedPalette)
        }
    }

    fun submitMockTest() {
        mockTimerJob?.cancel()
        val current = _activeMock.value ?: return
        if (current.isSubmitted) return

        viewModelScope.launch {
            val exam = ExamType.values().firstOrNull { it.id == current.mockTest.examId } ?: ExamType.SSC_CGL
            val timeUsed = (current.mockTest.durationMinutes * 60) - current.remainingSeconds
            val summary = repository.examEngine.evaluateMockSubmission(
                mockId = current.mockTest.id,
                exam = exam,
                paletteStates = current.paletteStates,
                timeUsedSeconds = timeUsed
            )
            _activeMock.value = current.copy(isSubmitted = true, resultSummary = summary)
        }
    }

    fun exitMockTest() {
        mockTimerJob?.cancel()
        _activeMock.value = null
    }

    // Profile & Settings
    fun updateLanguage(lang: AppLanguage) {
        viewModelScope.launch {
            val current = userProfile.value ?: return@launch
            repository.updateUserProfile(current.copy(preferredLanguage = lang.code))
        }
    }

    fun updateTargetExam(exam: ExamType) {
        viewModelScope.launch {
            val current = userProfile.value ?: return@launch
            repository.updateUserProfile(current.copy(targetExamId = exam.id))
            refreshNextActivityRecommendation()
        }
    }

    fun updateDailyGoal(minutes: Int) {
        viewModelScope.launch {
            val current = userProfile.value ?: return@launch
            repository.updateUserProfile(current.copy(dailyMinutesGoal = minutes))
        }
    }
}
