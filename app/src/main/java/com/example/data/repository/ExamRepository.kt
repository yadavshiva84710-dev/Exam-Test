package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ExamRepository(private val db: AppDatabase) {
    val allExams: Flow<List<ExamEntity>> = db.examDao().getAllExams()
    val allCategories: Flow<List<CategoryEntity>> = db.categoryDao().getAllCategoriesFlow()
    val allAnnouncements: Flow<List<AnnouncementEntity>> = db.announcementDao().getAllAnnouncementsFlow()
    val allStudents: Flow<List<UserEntity>> = db.userDao().getAllStudents()
    val auditLogs: Flow<List<AuditLogEntity>> = db.auditDao().getAllAuditLogsFlow()

    suspend fun getExam(examId: String): ExamEntity? = db.examDao().getExamById(examId)

    fun getQuestionsFlow(examId: String): Flow<List<QuestionEntity>> =
        db.questionDao().getQuestionsForExamFlow(examId)

    suspend fun getQuestions(examId: String): List<QuestionEntity> =
        db.questionDao().getQuestionsForExam(examId)

    suspend fun getAttempt(attemptId: String): AttemptEntity? =
        db.attemptDao().getAttempt(attemptId)

    fun getAnswersFlow(attemptId: String): Flow<List<AttemptAnswerEntity>> =
        db.attemptDao().getAnswersForAttemptFlow(attemptId)

    suspend fun getUser(uid: String): UserEntity? = db.userDao().getUserById(uid)

    suspend fun saveUser(user: UserEntity) = db.userDao().insertUser(user)

    suspend fun startOrResumeAttempt(examId: String, studentId: String): AttemptEntity {
        val existing = db.attemptDao().getLatestAttempt(examId, studentId)
        val now = System.currentTimeMillis()
        if (existing != null && !existing.isSubmitted && existing.deadlineTimestamp > now) {
            return existing
        }

        val exam = db.examDao().getExamById(examId)
        val durationMs = ((exam?.durationMinutes ?: 15) * 60 * 1000).toLong()
        val newAttempt = AttemptEntity(
            attemptId = "att_${UUID.randomUUID().toString().take(8)}",
            examId = examId,
            studentId = studentId,
            startTimestamp = now,
            deadlineTimestamp = now + durationMs,
            isSubmitted = false
        )
        db.attemptDao().insertAttempt(newAttempt)

        // Seed initial unanswered states for palette
        val questions = db.questionDao().getQuestionsForExam(examId)
        val initialAnswers = questions.mapIndexed { index, q ->
            AttemptAnswerEntity(
                attemptId = newAttempt.attemptId,
                questionId = q.questionId,
                selectedOption = null,
                isMarkedForReview = false,
                isVisited = (index == 0) // mark first question visited
            )
        }
        db.attemptDao().insertAnswers(initialAnswers)

        logAction("EXAM_JOINED", studentId, examId, "Student entered live test: ${exam?.titleEn}")
        return newAttempt
    }

    suspend fun saveAnswer(
        attemptId: String,
        questionId: String,
        selectedOption: String?,
        isMarkedForReview: Boolean,
        isVisited: Boolean = true
    ) {
        val existing = db.attemptDao().getAnswer(attemptId, questionId)
        val answer = existing?.copy(
            selectedOption = selectedOption,
            isMarkedForReview = isMarkedForReview,
            isVisited = isVisited,
            savedTimestamp = System.currentTimeMillis(),
            isSyncedToServer = true
        ) ?: AttemptAnswerEntity(
            attemptId = attemptId,
            questionId = questionId,
            selectedOption = selectedOption,
            isMarkedForReview = isMarkedForReview,
            isVisited = isVisited,
            savedTimestamp = System.currentTimeMillis(),
            isSyncedToServer = true
        )
        db.attemptDao().insertOrUpdateAnswer(answer)
    }

    suspend fun submitAttempt(attemptId: String, studentName: String): ResultEntity {
        val attempt = db.attemptDao().getAttempt(attemptId) ?: throw IllegalStateException("Attempt not found")
        val exam = db.examDao().getExamById(attempt.examId) ?: throw IllegalStateException("Exam not found")
        val questions = db.questionDao().getQuestionsForExam(attempt.examId)
        val answers = db.attemptDao().getAnswersForAttempt(attemptId)

        val answersMap = answers.associateBy { it.questionId }
        var correctCount = 0
        var incorrectCount = 0
        var unansweredCount = 0

        for (q in questions) {
            val ans = answersMap[q.questionId]
            if (ans == null || ans.selectedOption.isNullOrBlank()) {
                unansweredCount++
            } else if (ans.selectedOption.trim().equals(q.correctOption.trim(), ignoreCase = true)) {
                correctCount++
            } else {
                incorrectCount++
            }
        }

        val positiveMarks = exam.marksPerCorrect
        val negMarks = exam.negativeMarks
        val rawScore = (correctCount * positiveMarks) - (incorrectCount * negMarks)
        val totalScore = Math.max(0.0, Math.round(rawScore * 100.0) / 100.0)
        val maxScore = exam.maxMarks
        val percentage = Math.round((totalScore / maxScore * 100.0) * 10.0) / 10.0

        val attemptedCount = correctCount + incorrectCount
        val accuracy = if (attemptedCount > 0) {
            Math.round((correctCount.toDouble() / attemptedCount * 100.0) * 10.0) / 10.0
        } else 0.0

        val totalParticipants = exam.registeredCount
        // Deterministic realistic rank based on score ratio
        val ratio = totalScore / maxScore
        val simulatedRank = Math.max(1, ((1.0 - ratio) * totalParticipants * 0.85).toInt() + 1)
        val percentile = Math.round(((totalParticipants - simulatedRank).toDouble() / totalParticipants * 100.0) * 10.0) / 10.0

        val result = ResultEntity(
            resultId = "res_${UUID.randomUUID().toString().take(8)}",
            examId = exam.examId,
            examTitle = exam.titleEn,
            attemptId = attemptId,
            studentId = attempt.studentId,
            studentName = studentName,
            totalScore = totalScore,
            maxScore = maxScore,
            percentage = percentage,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            unansweredCount = unansweredCount,
            accuracy = accuracy,
            rank = simulatedRank,
            totalParticipants = totalParticipants,
            percentile = percentile,
            submittedAt = System.currentTimeMillis()
        )

        db.resultDao().insertResult(result)
        db.attemptDao().updateAttempt(
            attempt.copy(
                isSubmitted = true,
                submittedTimestamp = System.currentTimeMillis()
            )
        )

        logAction("EXAM_SUBMITTED", attempt.studentId, exam.examId, "Score: $totalScore / $maxScore (Rank: $simulatedRank)")
        return result
    }

    suspend fun getResult(attemptId: String): ResultEntity? =
        db.resultDao().getResultForAttempt(attemptId)

    fun getStudentResults(studentId: String): Flow<List<ResultEntity>> =
        db.resultDao().getResultsForStudentFlow(studentId)

    fun getExamResults(examId: String): Flow<List<ResultEntity>> =
        db.resultDao().getResultsForExamFlow(examId)

    suspend fun createExam(exam: ExamEntity, questions: List<QuestionEntity>) {
        db.examDao().insertExam(exam)
        db.questionDao().insertQuestions(questions)
        logAction("EXAM_CREATED", "ADMIN", exam.examId, "New exam created: ${exam.titleEn} with ${questions.size} questions")
    }

    suspend fun addAnnouncement(announcement: AnnouncementEntity) {
        db.announcementDao().insertAnnouncement(announcement)
        logAction("ANNOUNCEMENT_POSTED", "ADMIN", announcement.id, announcement.titleEn)
    }

    suspend fun logAction(action: String, performedBy: String, target: String, details: String) {
        db.auditDao().insertAuditLog(
            AuditLogEntity(
                action = action,
                performedBy = performedBy,
                target = target,
                details = details,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun generateLeaderboard(exam: ExamEntity, userResult: ResultEntity?): List<LeaderboardEntry> {
        val list = mutableListOf<LeaderboardEntry>()
        val maxMarks = exam.maxMarks

        // Benchmark top performers (simulated cohort)
        val benchmarkScores = listOf(
            Triple("Amitabh Sharma", 0.96, "Lucknow, UP"),
            Triple("Priya Singh", 0.94, "Prayagraj, UP"),
            Triple("Deepak Verma", 0.91, "Varanasi, UP"),
            Triple("Sneha Patel", 0.89, "Kanpur, UP"),
            Triple("Vikram Yadav", 0.88, "Gorakhpur, UP"),
            Triple("Pooja Mishra", 0.86, "Agra, UP"),
            Triple("Rohan Tiwari", 0.85, "Meerut, UP"),
            Triple("Ananya Dubey", 0.83, "Bareilly, UP"),
            Triple("Manish Chauhan", 0.81, "Noida, UP"),
            Triple("Sunil Kumar", 0.80, "Jhansi, UP")
        )

        var userInserted = false
        var currentRank = 1

        for ((name, pct, loc) in benchmarkScores) {
            val marks = Math.round(maxMarks * pct * 10.0) / 10.0
            if (userResult != null && !userInserted && userResult.totalScore >= marks) {
                list.add(
                    LeaderboardEntry(
                        rank = currentRank++,
                        studentName = userResult.studentName + " (You)",
                        studentId = userResult.studentId,
                        marks = userResult.totalScore,
                        maxMarks = maxMarks,
                        percentage = userResult.percentage,
                        submissionTimeFormatted = "Just now",
                        state = "UP",
                        isCurrentUser = true
                    )
                )
                userInserted = true
            }

            list.add(
                LeaderboardEntry(
                    rank = currentRank++,
                    studentName = name,
                    studentId = "STU${1000 + currentRank}",
                    marks = marks,
                    maxMarks = maxMarks,
                    percentage = Math.round(pct * 1000.0) / 10.0,
                    submissionTimeFormatted = "${12 + currentRank}m ago",
                    state = loc,
                    isCurrentUser = false
                )
            )
        }

        if (userResult != null && !userInserted) {
            list.add(
                LeaderboardEntry(
                    rank = userResult.rank,
                    studentName = userResult.studentName + " (You)",
                    studentId = userResult.studentId,
                    marks = userResult.totalScore,
                    maxMarks = maxMarks,
                    percentage = userResult.percentage,
                    submissionTimeFormatted = "Just now",
                    state = "UP",
                    isCurrentUser = true
                )
            )
        }

        return list
    }

    suspend fun exportResultsCsv(examId: String): String {
        val exam = db.examDao().getExamById(examId)
        val sb = StringBuilder()
        sb.append("Rank,Student ID,Student Name,State,Marks Obtained,Max Marks,Percentage,Accuracy,Status\n")
        val sampleResults = listOf(
            "1,STU1001,Amitabh Sharma,UP,28.5,30.0,95.0%,96.0%,Submitted",
            "2,STU1002,Priya Singh,UP,27.0,30.0,90.0%,92.5%,Submitted",
            "3,STU1003,Deepak Verma,UP,26.0,30.0,86.6%,89.0%,Submitted",
            "4,STU1004,Sneha Patel,UP,25.0,30.0,83.3%,87.5%,Submitted",
            "5,STU1005,Vikram Yadav,UP,24.5,30.0,81.6%,85.0%,Submitted"
        )
        sampleResults.forEach { sb.append(it).append("\n") }
        return sb.toString()
    }

    suspend fun seedInitialDataIfEmpty() {
        val questions = db.questionDao().getQuestionsForExam("exam_up_police_01")
        if (questions.isNotEmpty()) return

        // 1. Seed Categories
        val categories = listOf(
            CategoryEntity("UP_POLICE", "UP Police", "उत्तर प्रदेश पुलिस", "shield", 4),
            CategoryEntity("UP_PET", "UP Police PET", "यूपी पुलिस पीईटी", "run", 2),
            CategoryEntity("TGT", "TGT (Trained Teacher)", "टीजीटी शिक्षक भर्ती", "school", 3),
            CategoryEntity("PGT", "PGT (Post Graduate)", "पीजीटी प्रवक्ता परीक्षा", "history_edu", 3),
            CategoryEntity("UGC_NET", "UGC NET Paper 1", "यूजीसी नेट पेपर 1", "menu_book", 5),
            CategoryEntity("UGC_JRF", "UGC JRF Special", "यूजीसी जेआरएफ", "workspace_premium", 2),
            CategoryEntity("SSC", "SSC (CGL, CHSL, GD)", "एसएससी परीक्षाएं", "psychology", 6)
        )
        db.categoryDao().insertCategories(categories)

        // 2. Seed Default Users
        val defaultStudents = listOf(
            UserEntity(
                uid = "student_rahul",
                studentId = "JT-2026-8841",
                fullName = "Rahul Kumar",
                email = "rahul.yadav@gmail.com",
                phone = "+91 98765 43210",
                role = UserRole.STUDENT,
                state = "Uttar Pradesh",
                district = "Lucknow",
                preferredCategory = "UP_POLICE"
            ),
            UserEntity(
                uid = "admin_teacher",
                studentId = "TEA-401",
                fullName = "Dr. S. K. Sharma",
                email = "teacher.sharma@testbook.org",
                phone = "+91 91234 56789",
                role = UserRole.EXAM_ADMIN,
                state = "Uttar Pradesh",
                district = "Prayagraj"
            ),
            UserEntity(
                uid = "super_admin",
                studentId = "ADM-001",
                fullName = "Chief Controller",
                email = "admin@juniortestbook.in",
                phone = "+91 99999 88888",
                role = UserRole.SUPER_ADMIN,
                state = "Delhi",
                district = "Central Delhi"
            )
        )
        defaultStudents.forEach { db.userDao().insertUser(it) }

        // 3. Seed Exams
        val now = System.currentTimeMillis()
        val exams = listOf(
            ExamEntity(
                examId = "exam_up_police_01",
                titleEn = "UP Police Constable Live Mock Test 2026",
                titleHi = "उत्तर प्रदेश पुलिस कांस्टेबल लाइव मॉक टेस्ट 2026",
                categoryId = "UP_POLICE",
                subject = "General Knowledge & Hindi & Reasoning",
                descriptionEn = "All UP State Live Test with actual exam pattern, timer, negative marking, and real-time live ranking.",
                descriptionHi = "वास्तविक परीक्षा पैटर्न, समय सीमा, नकारात्मक अंकन और रीयल-टाइम रैंक के साथ ऑल यूपी लाइव टेस्ट।",
                instructionsEn = "1. Total 10 Questions, Duration 10 mins.\n2. +2.0 marks for correct, -0.5 marks for wrong answer.\n3. Question palette displays status.\n4. Answers save automatically.",
                instructionsHi = "1. कुल 10 प्रश्न, समय 10 मिनट।\n2. सही उत्तर पर +2.0 अंक, गलत उत्तर पर -0.5 अंक काटे जाएंगे।\n3. प्रश्न पैलेट वास्तविक स्थिति दर्शाएगा।\n4. उत्तर स्वतः सुरक्षित होते हैं।",
                durationMinutes = 10,
                totalQuestions = 10,
                maxMarks = 20.0,
                marksPerCorrect = 2.0,
                negativeMarks = 0.5,
                startTimestamp = now - 600000,
                endTimestamp = now + 86400000,
                status = ExamStatus.LIVE,
                registeredCount = 3420,
                activeCount = 890,
                submittedCount = 2530
            ),
            ExamEntity(
                examId = "exam_ugc_net_01",
                titleEn = "UGC NET Paper 1: Teaching & Research Aptitude",
                titleHi = "यूजीसी नेट पेपर 1: शिक्षण एवं शोध अभिक्षमता",
                categoryId = "UGC_NET",
                subject = "Paper 1 Common",
                descriptionEn = "Standard NTA pattern exam covering Teaching Aptitude, Research Methodology, and Higher Education.",
                descriptionHi = "शिक्षण अभिरुचि, शोध पद्धति और उच्च शिक्षा पर आधारित एनटीए पैटर्न परीक्षा।",
                instructionsEn = "No negative marking. 10 Questions, 20 Marks. Duration 10 Minutes.",
                instructionsHi = "नकारात्मक अंकन नहीं है। 10 प्रश्न, 20 अंक। समय 10 मिनट।",
                durationMinutes = 10,
                totalQuestions = 10,
                maxMarks = 20.0,
                marksPerCorrect = 2.0,
                negativeMarks = 0.0,
                startTimestamp = now - 3600000,
                endTimestamp = now + 172800000,
                status = ExamStatus.LIVE,
                registeredCount = 1840,
                activeCount = 420,
                submittedCount = 1420
            ),
            ExamEntity(
                examId = "exam_tgt_pgt_01",
                titleEn = "UP TGT/PGT General Studies & Hindi Practice Test",
                titleHi = "यूपी टीजीटी/पीजीटी सामान्य अध्ययन व हिन्दी अभ्यास परीक्षा",
                categoryId = "TGT",
                subject = "General Studies & Hindi Literature",
                descriptionEn = "Essential practice test for UP Secondary Education Service Selection Board candidates.",
                descriptionHi = "उत्तर प्रदेश माध्यमिक शिक्षा सेवा चयन बोर्ड अभ्यर्थियों के लिए आवश्यक अभ्यास टेस्ट।",
                instructionsEn = "4 Marks per question. Negative marking 1.0. Duration 15 minutes.",
                instructionsHi = "प्रत्येक प्रश्न के 4 अंक। नकारात्मक अंकन 1.0 अंक। समय 15 मिनट।",
                durationMinutes = 15,
                totalQuestions = 10,
                maxMarks = 40.0,
                marksPerCorrect = 4.0,
                negativeMarks = 1.0,
                startTimestamp = now + 1800000,
                endTimestamp = now + 86400000 * 2,
                status = ExamStatus.SCHEDULED,
                registeredCount = 920,
                activeCount = 0,
                submittedCount = 0
            )
        )
        db.examDao().insertExams(exams)

        // 4. Seed Questions for UP Police Exam
        val upPoliceQuestions = listOf(
            QuestionEntity(
                questionId = "up_q1",
                examId = "exam_up_police_01",
                orderIndex = 1,
                questionEn = "In which year was Uttar Pradesh Police department officially established?",
                questionHi = "उत्तर प्रदेश पुलिस विभाग की स्थापना आधिकारिक तौर पर किस वर्ष में हुई थी?",
                optionAEn = "1861",
                optionAHi = "1861 में",
                optionBEn = "1863",
                optionBHi = "1863 में",
                optionCEn = "1905",
                optionCHi = "1905 में",
                optionDEn = "1947",
                optionDHi = "1947 में",
                correctOption = "B",
                explanationEn = "Uttar Pradesh Police was officially constituted in 1863 following the Police Act of 1861.",
                explanationHi = "उत्तर प्रदेश पुलिस का गठन 1861 के पुलिस अधिनियम के अनुसरण में 1863 में किया गया था। इसका मुख्यालय प्रयागराज से बाद में लखनऊ स्थानांतरित किया गया।",
                subject = "General Knowledge",
                topic = "UP Police History",
                marks = 2.0,
                negativeMarks = 0.5
            ),
            QuestionEntity(
                questionId = "up_q2",
                examId = "exam_up_police_01",
                orderIndex = 2,
                questionEn = "Which of the following is the official state animal of Uttar Pradesh?",
                questionHi = "निम्नलिखित में से कौन सा उत्तर प्रदेश का राजकीय पशु है?",
                optionAEn = "Swamp Deer (Barasingha)",
                optionAHi = "बारहसिंगा (दलदली हिरण)",
                optionBEn = "Bengal Tiger",
                optionBHi = "रॉयल बंगाल टाइगर",
                optionCEn = "Indian Elephant",
                optionCHi = "एशियाई हाथी",
                optionDEn = "One-horned Rhinoceros",
                optionDHi = "एक सींग वाला गैंडा",
                correctOption = "A",
                explanationEn = "Barasingha (Swamp Deer) is the state animal of Uttar Pradesh. The state bird is Sarus Crane.",
                explanationHi = "उत्तर प्रदेश का राजकीय पशु बारहसिंगा (Swamp Deer) है। राजकीय पक्षी सारस (क्रौंच), राजकीय वृक्ष अशोक और राजकीय पुष्प पलाश है।",
                subject = "General Knowledge",
                topic = "UP Geography",
                marks = 2.0,
                negativeMarks = 0.5
            ),
            QuestionEntity(
                questionId = "up_q3",
                examId = "exam_up_police_01",
                orderIndex = 3,
                questionEn = "Identify the sandhi in the Hindi word 'सूर्योदय' (Suryodaya):",
                questionHi = "'सूर्योदय' शब्द में कौन सी संधि प्रयुक्त हुई है?",
                optionAEn = "Dirgha Sandhi",
                optionAHi = "दीर्घ संधि",
                optionBEn = "Guna Sandhi",
                optionBHi = "गुण संधि",
                optionCEn = "Vriddhi Sandhi",
                optionCHi = "वृद्धि संधि",
                optionDEn = "Yan Sandhi",
                optionDHi = "यण संधि",
                correctOption = "B",
                explanationEn = "सूर्य + उदय = सूर्योदय (अ + उ = ओ), hence it is Guna Sandhi.",
                explanationHi = "सूर्य + उदय = सूर्योदय। नियम: अ/आ के बाद इ/ई, उ/ऊ या ऋ आए तो क्रमशः ए, ओ, अर् हो जाते हैं। अतः यह 'गुण स्वर संधि' है।",
                subject = "General Hindi",
                topic = "Sandhi",
                marks = 2.0,
                negativeMarks = 0.5
            ),
            QuestionEntity(
                questionId = "up_q4",
                examId = "exam_up_police_01",
                orderIndex = 4,
                questionEn = "What is the antonym (विलोम) of 'उत्कर्ष' (Utkarsh)?",
                questionHi = "'उत्कर्ष' शब्द का सही विलोम शब्द क्या है?",
                optionAEn = "Apkarsh",
                optionAHi = "अपकर्ष",
                optionBEn = "Nikarsh",
                optionBHi = "निकर्ष",
                optionCEn = "Prakash",
                optionCHi = "प्रकाश",
                optionDEn = "Vimarsh",
                optionDHi = "विमर्श",
                correctOption = "A",
                explanationEn = "The opposite of Utkarsh (rise/elevation) is Apkarsh (decline/downfall).",
                explanationHi = "उत्कर्ष का अर्थ उन्नति या चढ़ाव होता है, जिसका विलोम 'अपकर्ष' (पतन/अवनति) होता है।",
                subject = "General Hindi",
                topic = "Vilom Shabd",
                marks = 2.0,
                negativeMarks = 0.5
            ),
            QuestionEntity(
                questionId = "up_q5",
                examId = "exam_up_police_01",
                orderIndex = 5,
                questionEn = "If POLICE is coded as QPMJDF, how will REPUTE be coded?",
                questionHi = "यदि किसी सांकेतिक भाषा में POLICE को QPMJDF लिखा जाता है, तो REPUTE को क्या लिखा जाएगा?",
                optionAEn = "SFQVU F",
                optionAHi = "SFQVUF",
                optionBEn = "SFQUVF",
                optionBHi = "SFQUVF",
                optionCEn = "RFQVTF",
                optionCHi = "RFQVTF",
                optionDEn = "SGQWVF",
                optionDHi = "SGQWVF",
                correctOption = "A",
                explanationEn = "Each letter is shifted by +1: R->S, E->F, P->Q, U->V, T->U, E->F. Result: SFQVUF.",
                explanationHi = "प्रत्येक वर्ण में +1 की वृद्धि हो रही है: P(+1)=Q, O(+1)=P... इसी प्रकार REPUTE में R(+1)=S, E(+1)=F, P(+1)=Q, U(+1)=V, T(+1)=U, E(+1)=F = SFQVUF।",
                subject = "Reasoning",
                topic = "Coding-Decoding",
                marks = 2.0,
                negativeMarks = 0.5
            ),
            QuestionEntity(
                questionId = "up_q6",
                examId = "exam_up_police_01",
                orderIndex = 6,
                questionEn = "Find the missing number in the series: 3, 7, 15, 31, 63, ?",
                questionHi = "दी गई श्रृंखला में लुप्त संख्या ज्ञात कीजिए: 3, 7, 15, 31, 63, ?",
                optionAEn = "125",
                optionAHi = "125",
                optionBEn = "127",
                optionBHi = "127",
                optionCEn = "129",
                optionCHi = "129",
                optionDEn = "131",
                optionDHi = "131",
                correctOption = "B",
                explanationEn = "Pattern is: (x * 2) + 1. 3*2+1=7; 7*2+1=15; 15*2+1=31; 31*2+1=63; 63*2+1=127.",
                explanationHi = "पैटर्न: (पिछली संख्या × 2) + 1। 63 × 2 + 1 = 126 + 1 = 127।",
                subject = "Reasoning",
                topic = "Number Series",
                marks = 2.0,
                negativeMarks = 0.5
            ),
            QuestionEntity(
                questionId = "up_q7",
                examId = "exam_up_police_01",
                orderIndex = 7,
                questionEn = "A train 180 meters long crosses a pole in 9 seconds. What is its speed in km/h?",
                questionHi = "180 मीटर लंबी एक रेलगाड़ी किसी खंभे को 9 सेकंड में पार करती है। रेलगाड़ी की चाल किमी/घंटा में क्या होगी?",
                optionAEn = "60 km/h",
                optionAHi = "60 किमी/घंटा",
                optionBEn = "72 km/h",
                optionBHi = "72 किमी/घंटा",
                optionCEn = "80 km/h",
                optionCHi = "80 किमी/घंटा",
                optionDEn = "90 km/h",
                optionDHi = "90 किमी/घंटा",
                correctOption = "B",
                explanationEn = "Speed = Distance / Time = 180 / 9 = 20 m/s. In km/h: 20 * (18 / 5) = 72 km/h.",
                explanationHi = "चाल = दूरी / समय = 180 / 9 = 20 मीटर/सेकंड। किमी/घंटे में बदलने के लिए: 20 × (18/5) = 72 किमी/घंटा।",
                subject = "Numerical Aptitude",
                topic = "Speed, Time & Distance",
                marks = 2.0,
                negativeMarks = 0.5
            ),
            QuestionEntity(
                questionId = "up_q8",
                examId = "exam_up_police_01",
                orderIndex = 8,
                questionEn = "The simple interest on ₹5,000 for 3 years at 8% per annum is:",
                questionHi = "₹5,000 पर 8% वार्षिक दर से 3 वर्ष का साधारण ब्याज कितना होगा?",
                optionAEn = "₹1,000",
                optionAHi = "₹1,000",
                optionBEn = "₹1,200",
                optionBHi = "₹1,200",
                optionCEn = "₹1,400",
                optionCHi = "₹1,400",
                optionDEn = "₹1,500",
                optionDHi = "₹1,500",
                correctOption = "B",
                explanationEn = "SI = (P * R * T) / 100 = (5000 * 8 * 3) / 100 = ₹1,200.",
                explanationHi = "साधारण ब्याज = (मूलधन × दर × समय) / 100 = (5000 × 8 × 3) / 100 = ₹1,200।",
                subject = "Numerical Aptitude",
                topic = "Simple Interest",
                marks = 2.0,
                negativeMarks = 0.5
            ),
            QuestionEntity(
                questionId = "up_q9",
                examId = "exam_up_police_01",
                orderIndex = 9,
                questionEn = "Under which Article of the Indian Constitution is the Right to Equality guaranteed?",
                questionHi = "भारतीय संविधान के किस अनुच्छेद के तहत 'समानता का अधिकार' प्रदान किया गया है?",
                optionAEn = "Articles 14 to 18",
                optionAHi = "अनुच्छेद 14 से 18",
                optionBEn = "Articles 19 to 22",
                optionBHi = "अनुच्छेद 19 से 22",
                optionCEn = "Articles 23 to 24",
                optionCHi = "अनुच्छेद 23 से 24",
                optionDEn = "Articles 25 to 28",
                optionDHi = "अनुच्छेद 25 से 28",
                correctOption = "A",
                explanationEn = "Articles 14 to 18 guarantee Right to Equality before law and equal protection of laws.",
                explanationHi = "भारतीय संविधान के भाग 3 में अनुच्छेद 14 से 18 तक विधि के समक्ष समता और समान संरक्षण (समानता का अधिकार) का प्रावधान है।",
                subject = "Polity",
                topic = "Fundamental Rights",
                marks = 2.0,
                negativeMarks = 0.5
            ),
            QuestionEntity(
                questionId = "up_q10",
                examId = "exam_up_police_01",
                orderIndex = 10,
                questionEn = "Which river is known as the 'Sorrow of Bihar'?",
                questionHi = "किस नदी को 'बिहार का शोक' कहा जाता है?",
                optionAEn = "Son River",
                optionAHi = "सोन नदी",
                optionBEn = "Gandak River",
                optionBHi = "गंडक नदी",
                optionCEn = "Kosi River",
                optionCHi = "कोसी नदी",
                optionDEn = "Ghaghara River",
                optionDHi = "घाघरा नदी",
                correctOption = "C",
                explanationEn = "Kosi River is notorious for frequently changing its course and causing devastating floods, earning the name 'Sorrow of Bihar'.",
                explanationHi = "कोसी नदी अपना मार्ग बदलने और भयंकर बाढ़ लाने के कारण 'बिहार का शोक' कहलाती है। यह गंगा की प्रमुख सहायक नदी है।",
                subject = "Geography",
                topic = "Indian Rivers",
                marks = 2.0,
                negativeMarks = 0.5
            )
        )
        db.questionDao().insertQuestions(upPoliceQuestions)

        // 5. Seed Questions for UGC NET Paper 1
        val ugcNetQuestions = (1..10).map { i ->
            QuestionEntity(
                questionId = "ugc_q$i",
                examId = "exam_ugc_net_01",
                orderIndex = i,
                questionEn = "Which evaluation method is conducted periodically during the instructional process to monitor learning progress?",
                questionHi = "अधिगम की प्रगति की निगरानी के लिए शिक्षण प्रक्रिया के दौरान समय-समय पर कौन सा मूल्यांकन किया जाता है?",
                optionAEn = "Summative Evaluation",
                optionAHi = "संकलनात्मक (सत्रांत) मूल्यांकन",
                optionBEn = "Formative Evaluation",
                optionBHi = "रचनात्मक (निर्माणात्मक) मूल्यांकन",
                optionCEn = "Diagnostic Evaluation",
                optionCHi = "निदानात्मक मूल्यांकन",
                optionDEn = "Placement Evaluation",
                optionDHi = "स्थान निर्धारण मूल्यांकन",
                correctOption = "B",
                explanationEn = "Formative evaluation provides ongoing feedback to improve students' learning during instruction.",
                explanationHi = "निर्माणात्मक (फॉर्मेटिव) मूल्यांकन शिक्षण प्रक्रिया के दौरान निरंतर प्रतिक्रिया देने और अधिगम सुधार हेतु किया जाता है।",
                subject = "Teaching Aptitude",
                topic = "Evaluation Systems",
                marks = 2.0,
                negativeMarks = 0.0
            )
        }
        db.questionDao().insertQuestions(ugcNetQuestions)

        // 6. Seed Announcements
        val announcements = listOf(
            AnnouncementEntity(
                id = "ann_01",
                titleEn = "UP Police Constable 2026 Live Test is Active!",
                titleHi = "यूपी पुलिस कांस्टेबल 2026 लाइव टेस्ट प्रारंभ हो चुका है!",
                messageEn = "Over 3,400 students are taking the live exam. Join now before entry closes at 8:00 PM.",
                messageHi = "3,400 से अधिक छात्र लाइव परीक्षा दे रहे हैं। शाम 8:00 बजे से पहले तुरंत शामिल हों।",
                priority = "EXAM_ALERT"
            ),
            AnnouncementEntity(
                id = "ann_02",
                titleEn = "Official Answer Keys & Rank List Released for Batch 4",
                titleHi = "बैच 4 के लिए आधिकारिक उत्तर कुंजी और रैंक सूची जारी",
                messageEn = "Students can now review their solutions with detailed Hindi explanations in their result dashboard.",
                messageHi = "छात्र अब अपने परिणाम डैशबोर्ड में विस्तृत हिन्दी व्याख्या के साथ उत्तर देख सकते हैं।",
                priority = "NORMAL"
            )
        )
        announcements.forEach { db.announcementDao().insertAnnouncement(it) }

        // 7. Seed initial Audit log
        db.auditDao().insertAuditLog(
            AuditLogEntity(
                action = "SYSTEM_INITIALIZED",
                performedBy = "SYSTEM",
                target = "JUNIOR_TESTBOOK",
                details = "Junior Testbook examination platform initialized with UP Police, UGC NET, TGT/PGT test banks."
            )
        )
    }
}
