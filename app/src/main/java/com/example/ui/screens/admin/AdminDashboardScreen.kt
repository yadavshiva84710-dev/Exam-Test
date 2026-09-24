package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.components.LiveDotIndicator
import com.example.ui.components.StatusBadge
import com.example.ui.language.LanguageManager
import com.example.ui.theme.*

@Composable
fun AdminDashboardScreen(
    currentUser: UserEntity?,
    exams: List<ExamEntity>,
    students: List<UserEntity>,
    auditLogs: List<AuditLogEntity>,
    onCreateExam: (titleEn: String, titleHi: String, category: String, subject: String, duration: Int, posMarks: Double, negMarks: Double, questions: List<QuestionEntity>) -> Unit,
    onPostAnnouncement: (titleEn: String, titleHi: String, msgEn: String, msgHi: String, priority: String) -> Unit,
    onExportCsv: (String) -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Live Monitoring, 1: Create Exam, 2: Announcements & Logs
    var showCreateExamDialog by remember { mutableStateOf(false) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var exportedCsvText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CleanBgLight)
            .testTag("admin_dashboard_screen")
    ) {
        // Admin Banner
        Surface(
            color = NavyPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = LanguageManager.text(
                                en = if (currentUser?.role == UserRole.SUPER_ADMIN) "Super Admin Command Center" else "Exam Admin Dashboard",
                                hi = if (currentUser?.role == UserRole.SUPER_ADMIN) "सुपर एडमिन कमांड सेंटर" else "परीक्षा नियंत्रक डैशबोर्ड"
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Admin: ${currentUser?.fullName ?: "Director"} • ${currentUser?.studentId}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SERVER: ONLINE",
                            color = ExamGreenLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(LanguageManager.text("Live Monitor", "लाइव निगरानी"), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(LanguageManager.text("Manage Exams", "परीक्षा प्रबंधन"), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(LanguageManager.text("Audit & Notices", "ऑडिट व सूचनाएं"), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                // Tab 0: Live Monitoring
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Operational Live Metrics Grid
                    item {
                        Text(
                            text = LanguageManager.text("Live Concurrency & Test Takers", "समवर्ती परीक्षा सत्र व छात्र"),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AdminCounterCard("Registered", "3,420", Icons.Default.Groups, NavyPrimary, Modifier.weight(1f))
                            AdminCounterCard("Active Live", "890", Icons.Default.Sensors, ExamOrange, Modifier.weight(1f))
                            AdminCounterCard("Submitted", "2,530", Icons.Default.DoneAll, ExamGreen, Modifier.weight(1f))
                        }
                    }

                    // Progress Bar
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = LanguageManager.text("Overall Exam Submission Progress", "कुल परीक्षा सबमिशन प्रगति"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(text = "74.0%", fontWeight = FontWeight.Black, color = NavyPrimary, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { 0.74f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = NavyPrimary,
                                    trackColor = ExamGrayLight
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "2,530 submitted / 3,420 enrolled • Zero sync drops reported",
                                    fontSize = 11.sp,
                                    color = ExamGray
                                )
                            }
                        }
                    }

                    // Actions Bar: CSV Export
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = LanguageManager.text("Candidate Monitoring List", "अभ्यर्थी निगरानी सूची"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )

                            Button(
                                onClick = {
                                    val examId = exams.firstOrNull()?.examId ?: "exam_up_police_01"
                                    exportedCsvText = onExportCsv(examId)
                                    Toast.makeText(context, "CSV Results exported successfully!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("admin_export_csv_btn")
                            ) {
                                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(LanguageManager.text("Export CSV", "सीएसवी निर्यात"), fontSize = 11.sp)
                            }
                        }
                    }

                    // Student List Preview
                    items(students) { stu ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(SoftIndigoBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = NavyPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = stu.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(text = "${stu.studentId} • ${stu.district}", fontSize = 11.sp, color = ExamGray)
                                    }
                                }

                                StatusBadge(
                                    text = "ACTIVE",
                                    containerColor = ExamGreenLight,
                                    contentColor = ExamGreen
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // Tab 1: Manage Exams
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Button(
                            onClick = { showCreateExamDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_create_new_exam"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(LanguageManager.text("Create & Publish New Exam", "नई परीक्षा बनाएं व प्रकाशित करें"), fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        Text(
                            text = LanguageManager.text("Active Exam Catalog", "सक्रिय परीक्षा सूची"),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    items(exams) { exam ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (LanguageManager.isHindi()) exam.titleHi else exam.titleEn,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    StatusBadge(
                                        text = "${exam.status}",
                                        containerColor = SoftIndigoBg,
                                        contentColor = NavyPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${exam.subject} • ${exam.totalQuestions} Questions • ${exam.durationMinutes} min • +${exam.marksPerCorrect} / -${exam.negativeMarks}",
                                    fontSize = 12.sp,
                                    color = ExamGray
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                // Tab 2: Announcements & Audit Logs
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Button(
                            onClick = { showAnnouncementDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_post_announcement"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ExamOrange)
                        ) {
                            Icon(imageVector = Icons.Default.Campaign, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(LanguageManager.text("Broadcast New Announcement", "नई घोषणा प्रसारित करें"), fontWeight = FontWeight.Bold)
                        }
                    }

                    item {
                        Text(
                            text = LanguageManager.text("Real-Time Audit Log Events", "रीयल-टाइम ऑडिट लॉग इवेंट्स"),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    items(auditLogs) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(NavyPrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${log.action} • by ${log.performedBy}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = NavyPrimary
                                    )
                                    Text(
                                        text = log.details,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Exam Dialog
    if (showCreateExamDialog) {
        CreateExamDialog(
            onDismiss = { showCreateExamDialog = false },
            onCreate = { titleEn, titleHi, cat, subj, dur, pos, neg, qList ->
                onCreateExam(titleEn, titleHi, cat, subj, dur, pos, neg, qList)
                showCreateExamDialog = false
                Toast.makeText(context, "New Live Exam created & scheduled!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Post Announcement Dialog
    if (showAnnouncementDialog) {
        AnnouncementDialog(
            onDismiss = { showAnnouncementDialog = false },
            onPost = { tEn, tHi, mEn, mHi, prio ->
                onPostAnnouncement(tEn, tHi, mEn, mHi, prio)
                showAnnouncementDialog = false
                Toast.makeText(context, "Announcement broadcasted!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // CSV Viewer Dialog
    if (exportedCsvText != null) {
        Dialog(onDismissRequest = { exportedCsvText = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Exported Student Results (CSV)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = ExamGrayLight,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = exportedCsvText ?: "",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { exportedCsvText = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun AdminCounterCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontWeight = FontWeight.Black, fontSize = 16.sp, color = color)
            Text(text = title, fontSize = 10.sp, color = ExamGray)
        }
    }
}

@Composable
fun CreateExamDialog(
    onDismiss: () -> Unit,
    onCreate: (titleEn: String, titleHi: String, category: String, subject: String, duration: Int, posMarks: Double, negMarks: Double, questions: List<QuestionEntity>) -> Unit
) {
    var titleEn by remember { mutableStateOf("UP Special Police Constable Test") }
    var titleHi by remember { mutableStateOf("उत्तर प्रदेश विशेष पुलिस कांस्टेबल टेस्ट") }
    var subject by remember { mutableStateOf("GK & Law Aptitude") }
    var category by remember { mutableStateOf("UP_POLICE") }
    var durationMinutes by remember { mutableStateOf("15") }
    var positiveMarks by remember { mutableStateOf("2.0") }
    var negativeMarks by remember { mutableStateOf("0.5") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Create New Examination",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = titleEn,
                    onValueChange = { titleEn = it },
                    label = { Text("Exam Title (English)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = titleHi,
                    onValueChange = { titleHi = it },
                    label = { Text("Exam Title (हिन्दी)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject / Section") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = durationMinutes,
                        onValueChange = { durationMinutes = it },
                        label = { Text("Duration (mins)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = positiveMarks,
                        onValueChange = { positiveMarks = it },
                        label = { Text("+ Marks") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = negativeMarks,
                        onValueChange = { negativeMarks = it },
                        label = { Text("- Marks") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val dur = durationMinutes.toIntOrNull() ?: 15
                            val pos = positiveMarks.toDoubleOrNull() ?: 2.0
                            val neg = negativeMarks.toDoubleOrNull() ?: 0.5

                            // Create 5 sample questions
                            val sampleQuestions = listOf(
                                QuestionEntity(
                                    questionId = "custom_q1",
                                    examId = "",
                                    orderIndex = 1,
                                    questionEn = "Which High Court exercises jurisdiction over Uttar Pradesh?",
                                    questionHi = "उत्तर प्रदेश पर किस उच्च न्यायालय का क्षेत्राधिकार है?",
                                    optionAEn = "Allahabad High Court",
                                    optionAHi = "इलाहाबाद उच्च न्यायालय",
                                    optionBEn = "Lucknow High Court",
                                    optionBHi = "लखनऊ उच्च न्यायालय",
                                    optionCEn = "Delhi High Court",
                                    optionCHi = "दिल्ली उच्च न्यायालय",
                                    optionDEn = "Patna High Court",
                                    optionDHi = "पटना उच्च न्यायालय",
                                    correctOption = "A",
                                    explanationEn = "Allahabad High Court is the high court based in Prayagraj with a bench in Lucknow.",
                                    explanationHi = "इलाहाबाद उच्च न्यायालय की मुख्य पीठ प्रयागराज तथा खंडपीठ लखनऊ में स्थित है।",
                                    subject = subject,
                                    topic = "Judiciary",
                                    marks = pos,
                                    negativeMarks = neg
                                ),
                                QuestionEntity(
                                    questionId = "custom_q2",
                                    examId = "",
                                    orderIndex = 2,
                                    questionEn = "What is the capital city of Uttar Pradesh?",
                                    questionHi = "उत्तर प्रदेश की राजधानी कौन सा शहर है?",
                                    optionAEn = "Kanpur",
                                    optionAHi = "कानपुर",
                                    optionBEn = "Lucknow",
                                    optionBHi = "लखनऊ",
                                    optionCEn = "Varanasi",
                                    optionCHi = "वाराणसी",
                                    optionDEn = "Prayagraj",
                                    optionDHi = "प्रयागराज",
                                    correctOption = "B",
                                    explanationEn = "Lucknow is the administrative capital of Uttar Pradesh.",
                                    explanationHi = "लखनऊ उत्तर प्रदेश की प्रशासनिक राजधानी है।",
                                    subject = subject,
                                    topic = "State Capital",
                                    marks = pos,
                                    negativeMarks = neg
                                )
                            )

                            onCreate(titleEn, titleHi, category, subject, dur, pos, neg, sampleQuestions)
                        }
                    ) {
                        Text("Save & Publish")
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementDialog(
    onDismiss: () -> Unit,
    onPost: (titleEn: String, titleHi: String, msgEn: String, msgHi: String, priority: String) -> Unit
) {
    var titleEn by remember { mutableStateOf("Server Maintenance Complete") }
    var titleHi by remember { mutableStateOf("सर्वर रखरखाव पूर्ण हुआ") }
    var msgEn by remember { mutableStateOf("The platform is running at optimal speeds for the upcoming live exam.") }
    var msgHi by remember { mutableStateOf("आगामी लाइव परीक्षा के लिए प्लेटफॉर्म पूरी क्षमता से सक्रिय है।") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Broadcast Announcement",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = NavyPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = titleEn,
                    onValueChange = { titleEn = it },
                    label = { Text("Title (English)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = titleHi,
                    onValueChange = { titleHi = it },
                    label = { Text("Title (हिन्दी)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = msgEn,
                    onValueChange = { msgEn = it },
                    label = { Text("Message (English)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = msgHi,
                    onValueChange = { msgHi = it },
                    label = { Text("Message (हिन्दी)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onPost(titleEn, titleHi, msgEn, msgHi, "NORMAL") }) {
                        Text("Broadcast")
                    }
                }
            }
        }
    }
}
