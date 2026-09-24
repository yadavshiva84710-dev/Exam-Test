package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ExamRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class ExamViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = ExamRepository(db)

    // Current logged-in user
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Categories & Exams
    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExams: StateFlow<List<ExamEntity>> = repository.allExams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    val filteredExams: StateFlow<List<ExamEntity>> = combine(allExams, selectedCategoryId) { exams, catId ->
        if (catId == null) exams else exams.filter { it.categoryId == catId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val announcements: StateFlow<List<AnnouncementEntity>> = repository.allAnnouncements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStudents: StateFlow<List<UserEntity>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Examination Session State
    private val _activeExam = MutableStateFlow<ExamEntity?>(null)
    val activeExam: StateFlow<ExamEntity?> = _activeExam.asStateFlow()

    private val _activeAttempt = MutableStateFlow<AttemptEntity?>(null)
    val activeAttempt: StateFlow<AttemptEntity?> = _activeAttempt.asStateFlow()

    private val _questions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val questions: StateFlow<List<QuestionEntity>> = _questions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _answersMap = MutableStateFlow<Map<String, AttemptAnswerEntity>>(emptyMap())
    val answersMap: StateFlow<Map<String, AttemptAnswerEntity>> = _answersMap.asStateFlow()

    private val _timeRemainingSeconds = MutableStateFlow<Long>(600)
    val timeRemainingSeconds: StateFlow<Long> = _timeRemainingSeconds.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    // Results & Leaderboard
    private val _currentResult = MutableStateFlow<ResultEntity?>(null)
    val currentResult: StateFlow<ResultEntity?> = _currentResult.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            // Default user is Rahul Kumar (Student)
            val defaultStudent = repository.getUser("student_rahul")
            _currentUser.value = defaultStudent
        }
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun switchUserRole(role: UserRole) {
        viewModelScope.launch {
            when (role) {
                UserRole.STUDENT -> {
                    _currentUser.value = repository.getUser("student_rahul") ?: UserEntity(
                        uid = "student_rahul",
                        studentId = "JT-2026-8841",
                        fullName = "Rahul Kumar",
                        email = "rahul.yadav@gmail.com",
                        phone = "+91 98765 43210",
                        role = UserRole.STUDENT,
                        preferredCategory = "UP_POLICE"
                    )
                }
                UserRole.EXAM_ADMIN -> {
                    _currentUser.value = repository.getUser("admin_teacher") ?: UserEntity(
                        uid = "admin_teacher",
                        studentId = "TEA-401",
                        fullName = "Dr. S. K. Sharma",
                        email = "teacher.sharma@testbook.org",
                        phone = "+91 91234 56789",
                        role = UserRole.EXAM_ADMIN
                    )
                }
                UserRole.SUPER_ADMIN -> {
                    _currentUser.value = repository.getUser("super_admin") ?: UserEntity(
                        uid = "super_admin",
                        studentId = "ADM-001",
                        fullName = "Chief Controller",
                        email = "admin@juniortestbook.in",
                        phone = "+91 99999 88888",
                        role = UserRole.SUPER_ADMIN
                    )
                }
            }
        }
    }

    fun registerStudent(fullName: String, phone: String, email: String, state: String, district: String, category: String) {
        viewModelScope.launch {
            val uid = "stu_${UUID.randomUUID().toString().take(8)}"
            val studentId = "JT-${(1000..9999).random()}"
            val newUser = UserEntity(
                uid = uid,
                studentId = studentId,
                fullName = fullName,
                email = email,
                phone = phone,
                role = UserRole.STUDENT,
                state = state,
                district = district,
                preferredCategory = category
            )
            repository.saveUser(newUser)
            _currentUser.value = newUser
            repository.logAction("STUDENT_REGISTERED", uid, "AUTH", "Student registered: $fullName ($studentId)")
        }
    }

    fun startExam(examId: String, onReady: () -> Unit) {
        viewModelScope.launch {
            val exam = repository.getExam(examId) ?: return@launch
            _activeExam.value = exam
            val student = _currentUser.value ?: return@launch

            val attempt = repository.startOrResumeAttempt(examId, student.uid)
            _activeAttempt.value = attempt

            val qList = repository.getQuestions(examId)
            _questions.value = qList
            _currentQuestionIndex.value = 0

            // Listen for answers updates reactively
            launch {
                repository.getAnswersFlow(attempt.attemptId).collect { list ->
                    _answersMap.value = list.associateBy { it.questionId }
                }
            }

            // Start countdown timer
            startAuthoritativeTimer(attempt.deadlineTimestamp) {
                // Auto submit when timer reaches 0
                submitExam { }
            }

            onReady()
        }
    }

    private fun startAuthoritativeTimer(deadlineTimestamp: Long, onExpire: () -> Unit) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val remainingMs = deadlineTimestamp - System.currentTimeMillis()
                if (remainingMs <= 0) {
                    _timeRemainingSeconds.value = 0
                    onExpire()
                    break
                }
                _timeRemainingSeconds.value = remainingMs / 1000
                delay(1000)
            }
        }
    }

    fun selectOption(questionId: String, option: String) {
        val attempt = _activeAttempt.value ?: return
        viewModelScope.launch {
            _isSaving.value = true
            val currentAnswer = _answersMap.value[questionId]
            repository.saveAnswer(
                attemptId = attempt.attemptId,
                questionId = questionId,
                selectedOption = option,
                isMarkedForReview = currentAnswer?.isMarkedForReview ?: false,
                isVisited = true
            )
            delay(100) // Brief feedback indicator
            _isSaving.value = false
        }
    }

    fun clearOption(questionId: String) {
        val attempt = _activeAttempt.value ?: return
        viewModelScope.launch {
            _isSaving.value = true
            val currentAnswer = _answersMap.value[questionId]
            repository.saveAnswer(
                attemptId = attempt.attemptId,
                questionId = questionId,
                selectedOption = null,
                isMarkedForReview = currentAnswer?.isMarkedForReview ?: false,
                isVisited = true
            )
            delay(100)
            _isSaving.value = false
        }
    }

    fun toggleMarkForReview(questionId: String) {
        val attempt = _activeAttempt.value ?: return
        viewModelScope.launch {
            _isSaving.value = true
            val currentAnswer = _answersMap.value[questionId]
            val newReviewState = !(currentAnswer?.isMarkedForReview ?: false)
            repository.saveAnswer(
                attemptId = attempt.attemptId,
                questionId = questionId,
                selectedOption = currentAnswer?.selectedOption,
                isMarkedForReview = newReviewState,
                isVisited = true
            )
            delay(100)
            _isSaving.value = false
        }
    }

    fun goToQuestion(index: Int) {
        if (index in _questions.value.indices) {
            _currentQuestionIndex.value = index
            val q = _questions.value[index]
            val attempt = _activeAttempt.value ?: return
            // Mark question visited
            viewModelScope.launch {
                val current = _answersMap.value[q.questionId]
                if (current == null || !current.isVisited) {
                    repository.saveAnswer(
                        attemptId = attempt.attemptId,
                        questionId = q.questionId,
                        selectedOption = current?.selectedOption,
                        isMarkedForReview = current?.isMarkedForReview ?: false,
                        isVisited = true
                    )
                }
            }
        }
    }

    fun nextQuestion() {
        if (_currentQuestionIndex.value < _questions.value.size - 1) {
            goToQuestion(_currentQuestionIndex.value + 1)
        }
    }

    fun previousQuestion() {
        if (_currentQuestionIndex.value > 0) {
            goToQuestion(_currentQuestionIndex.value - 1)
        }
    }

    fun submitExam(onSubmitted: (attemptId: String) -> Unit) {
        val attempt = _activeAttempt.value ?: return
        if (_isSubmitting.value) return
        _isSubmitting.value = true
        timerJob?.cancel()

        viewModelScope.launch {
            val studentName = _currentUser.value?.fullName ?: "Student"
            val result = repository.submitAttempt(attempt.attemptId, studentName)
            _currentResult.value = result

            val exam = _activeExam.value
            if (exam != null) {
                _leaderboard.value = repository.generateLeaderboard(exam, result)
            }

            _isSubmitting.value = false
            onSubmitted(attempt.attemptId)
        }
    }

    fun loadResultForAttempt(attemptId: String) {
        viewModelScope.launch {
            val result = repository.getResult(attemptId)
            _currentResult.value = result
            if (result != null) {
                val exam = repository.getExam(result.examId)
                if (exam != null) {
                    _activeExam.value = exam
                    _leaderboard.value = repository.generateLeaderboard(exam, result)
                    _questions.value = repository.getQuestions(result.examId)
                    val answers = db.attemptDao().getAnswersForAttempt(attemptId)
                    _answersMap.value = answers.associateBy { it.questionId }
                }
            }
        }
    }

    fun createNewExam(
        titleEn: String,
        titleHi: String,
        categoryId: String,
        subject: String,
        durationMinutes: Int,
        positiveMarks: Double,
        negativeMarks: Double,
        questionList: List<QuestionEntity>
    ) {
        viewModelScope.launch {
            val examId = "exam_${UUID.randomUUID().toString().take(8)}"
            val now = System.currentTimeMillis()
            val totalMarks = questionList.size * positiveMarks
            val exam = ExamEntity(
                examId = examId,
                titleEn = titleEn,
                titleHi = titleHi,
                categoryId = categoryId,
                subject = subject,
                descriptionEn = "Live examination conducted by Junior Testbook",
                descriptionHi = "जूनियर टेस्टबुक द्वारा आयोजित लाइव परीक्षा",
                instructionsEn = "Answer all questions. Negative marking applied.",
                instructionsHi = "सभी प्रश्नों के उत्तर दें। नकारात्मक अंकन लागू है।",
                durationMinutes = durationMinutes,
                totalQuestions = questionList.size,
                maxMarks = totalMarks,
                marksPerCorrect = positiveMarks,
                negativeMarks = negativeMarks,
                startTimestamp = now,
                endTimestamp = now + 86400000 * 3,
                status = ExamStatus.LIVE,
                registeredCount = 450,
                activeCount = 120,
                submittedCount = 330
            )
            val updatedQuestions = questionList.mapIndexed { idx, q ->
                q.copy(examId = examId, orderIndex = idx + 1, marks = positiveMarks, negativeMarks = negativeMarks)
            }
            repository.createExam(exam, updatedQuestions)
        }
    }

    fun postAnnouncement(titleEn: String, titleHi: String, messageEn: String, messageHi: String, priority: String) {
        viewModelScope.launch {
            repository.addAnnouncement(
                AnnouncementEntity(
                    id = "ann_${UUID.randomUUID().toString().take(6)}",
                    titleEn = titleEn,
                    titleHi = titleHi,
                    messageEn = messageEn,
                    messageHi = messageHi,
                    priority = priority
                )
            )
        }
    }
}
