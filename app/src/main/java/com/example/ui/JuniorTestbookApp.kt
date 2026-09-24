package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserRole
import com.example.ui.components.AppHeader
import com.example.ui.screens.admin.AdminDashboardScreen
import com.example.ui.screens.auth.RoleSelectionDialog
import com.example.ui.screens.exam.LiveExamScreen
import com.example.ui.screens.exam.ResultScreen
import com.example.ui.screens.leaderboard.LeaderboardScreen
import com.example.ui.screens.student.StudentDashboardScreen
import com.example.ui.theme.CleanBgLight
import com.example.ui.viewmodel.ExamViewModel
import kotlinx.coroutines.launch

enum class AppScreen {
    DASHBOARD,
    EXAM,
    RESULT,
    LEADERBOARD
}

@Composable
fun JuniorTestbookApp(
    viewModel: ExamViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var showRoleDialog by remember { mutableStateOf(false) }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val filteredExams by viewModel.filteredExams.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

    val activeExam by viewModel.activeExam.collectAsStateWithLifecycle()
    val questions by viewModel.questions.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val answersMap by viewModel.answersMap.collectAsStateWithLifecycle()
    val timeRemainingSeconds by viewModel.timeRemainingSeconds.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isSubmitting by viewModel.isSubmitting.collectAsStateWithLifecycle()
    val currentResult by viewModel.currentResult.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("junior_testbook_root"),
        topBar = {
            if (currentScreen != AppScreen.EXAM) {
                AppHeader(
                    currentUser = currentUser,
                    onOpenRoleDialog = { showRoleDialog = true }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CleanBgLight)
        ) {
            Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                when (screen) {
                    AppScreen.DASHBOARD -> {
                        if (currentUser?.role == UserRole.STUDENT) {
                            StudentDashboardScreen(
                                currentUser = currentUser,
                                categories = categories,
                                selectedCategoryId = selectedCategoryId,
                                exams = filteredExams,
                                announcements = announcements,
                                onSelectCategory = { viewModel.selectCategory(it) },
                                onStartExam = { examId ->
                                    viewModel.startExam(examId) {
                                        currentScreen = AppScreen.EXAM
                                    }
                                },
                                onViewLeaderboard = { examId ->
                                    viewModel.startExam(examId) {
                                        currentScreen = AppScreen.LEADERBOARD
                                    }
                                },
                                onViewHistory = { /* History */ }
                            )
                        } else {
                            // Admin / Super Admin Dashboard
                            AdminDashboardScreen(
                                currentUser = currentUser,
                                exams = filteredExams,
                                students = allStudents,
                                auditLogs = auditLogs,
                                onCreateExam = { titleEn, titleHi, cat, subj, dur, pos, neg, qList ->
                                    viewModel.createNewExam(titleEn, titleHi, cat, subj, dur, pos, neg, qList)
                                },
                                onPostAnnouncement = { tEn, tHi, mEn, mHi, prio ->
                                    viewModel.postAnnouncement(tEn, tHi, mEn, mHi, prio)
                                },
                                onExportCsv = { examId ->
                                    "Rank,Student ID,Student Name,State,Marks Obtained,Max Marks,Percentage,Accuracy\n1,STU1001,Amitabh Sharma,UP,28.5,30.0,95.0%,96.0%\n2,STU1002,Priya Singh,UP,27.0,30.0,90.0%,92.5%\n3,STU1003,Deepak Verma,UP,26.0,30.0,86.6%,89.0%\n4,STU1004,Sneha Patel,UP,25.0,30.0,83.3%,87.5%\n5,STU1005,Vikram Yadav,UP,24.5,30.0,81.6%,85.0%"
                                }
                            )
                        }
                    }

                    AppScreen.EXAM -> {
                        LiveExamScreen(
                            exam = activeExam,
                            questions = questions,
                            currentIndex = currentIndex,
                            answersMap = answersMap,
                            timeRemainingSeconds = timeRemainingSeconds,
                            isSaving = isSaving,
                            isSubmitting = isSubmitting,
                            onSelectOption = { qId, opt -> viewModel.selectOption(qId, opt) },
                            onClearOption = { qId -> viewModel.clearOption(qId) },
                            onToggleMarkForReview = { qId -> viewModel.toggleMarkForReview(qId) },
                            onGoToQuestion = { idx -> viewModel.goToQuestion(idx) },
                            onNextQuestion = { viewModel.nextQuestion() },
                            onPreviousQuestion = { viewModel.previousQuestion() },
                            onSubmitExam = {
                                viewModel.submitExam { attemptId ->
                                    currentScreen = AppScreen.RESULT
                                }
                            }
                        )
                    }

                    AppScreen.RESULT -> {
                        ResultScreen(
                            result = currentResult,
                            exam = activeExam,
                            questions = questions,
                            answersMap = answersMap,
                            onViewLeaderboard = {
                                currentScreen = AppScreen.LEADERBOARD
                            },
                            onBackToDashboard = {
                                currentScreen = AppScreen.DASHBOARD
                            }
                        )
                    }

                    AppScreen.LEADERBOARD -> {
                        LeaderboardScreen(
                            exam = activeExam,
                            leaderboardEntries = leaderboard,
                            onBack = { currentScreen = AppScreen.DASHBOARD }
                        )
                    }
                }
            }
        }
    }

    if (showRoleDialog) {
        RoleSelectionDialog(
            currentUser = currentUser,
            onDismiss = { showRoleDialog = false },
            onRoleSelected = { role ->
                viewModel.switchUserRole(role)
            },
            onRegisterNewStudent = { name, phone, email, state, dist, cat ->
                viewModel.registerStudent(name, phone, email, state, dist, cat)
            }
        )
    }
}
