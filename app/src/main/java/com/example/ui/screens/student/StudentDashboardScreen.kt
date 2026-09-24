package com.example.ui.screens.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.ui.components.LiveDotIndicator
import com.example.ui.components.StatusBadge
import com.example.ui.language.LanguageManager
import com.example.ui.theme.*

@Composable
fun StudentDashboardScreen(
    currentUser: UserEntity?,
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    exams: List<ExamEntity>,
    announcements: List<AnnouncementEntity>,
    onSelectCategory: (String?) -> Unit,
    onStartExam: (String) -> Unit,
    onViewLeaderboard: (String) -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CleanBgLight)
            .testTag("student_dashboard_scroll"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.junior_testbook_banner_1790236873685),
                    contentDescription = "Test Preparation Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    NavyPrimary.copy(alpha = 0.4f),
                                    NavyPrimary.copy(alpha = 0.88f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = LanguageManager.text(
                                en = "Welcome, ${currentUser?.fullName ?: "Candidate"}",
                                hi = "स्वागत है, ${currentUser?.fullName ?: "अभ्यर्थी"}"
                            ),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = currentUser?.studentId ?: "JT-2026",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Text(
                        text = LanguageManager.text(
                            en = "UP Police, TGT, PGT & UGC NET Live Examination Portal",
                            hi = "यूपी पुलिस, टीजीटी, पीजीटी व यूजीसी नेट लाइव परीक्षा पोर्टल"
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    )
                }
            }
        }

        // 2. Student Quick Stats Cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardStatCard(
                    title = LanguageManager.text("Tests Taken", "कुल टेस्ट"),
                    value = "12",
                    icon = Icons.Default.AssignmentTurnedIn,
                    color = NavyPrimary,
                    modifier = Modifier.weight(1f)
                )
                DashboardStatCard(
                    title = LanguageManager.text("Avg Score", "औसत अंक"),
                    value = "84.5%",
                    icon = Icons.Default.TrendingUp,
                    color = ExamGreen,
                    modifier = Modifier.weight(1f)
                )
                DashboardStatCard(
                    title = LanguageManager.text("Best Rank", "सर्वश्रेष्ठ रैंक"),
                    value = "#4",
                    icon = Icons.Default.EmojiEvents,
                    color = ExamOrange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Urgent Announcements
        if (announcements.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ExamOrangeLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ExamOrange.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = ExamOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (LanguageManager.isHindi()) announcements.first().titleHi else announcements.first().titleEn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = ExamOrange
                            )
                            Text(
                                text = if (LanguageManager.isHindi()) announcements.first().messageHi else announcements.first().messageEn,
                                fontSize = 11.sp,
                                color = Color(0xFF78350F),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 4. Category Filter Chips
        item {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = LanguageManager.text("Target Examination Categories", "लक्ष्य परीक्षा श्रेणियां"),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { onSelectCategory(null) },
                            label = { Text(LanguageManager.text("All Exams", "सभी परीक्षाएं")) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.categoryId,
                            onClick = { onSelectCategory(cat.categoryId) },
                            label = { Text(if (LanguageManager.isHindi()) cat.nameHi else cat.nameEn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NavyPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // 5. Live & Active Exams Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = LanguageManager.text("Available Live Exams", "सक्रिय लाइव परीक्षाएं"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LiveDotIndicator()
                }

                Text(
                    text = "${exams.size} " + LanguageManager.text("Available", "उपलब्ध"),
                    style = MaterialTheme.typography.labelSmall.copy(color = ExamGray)
                )
            }
        }

        // 6. Exam Cards List
        if (exams.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = ExamGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = LanguageManager.text("No exams found in this category", "इस श्रेणी में कोई परीक्षा नहीं मिली"),
                            color = ExamGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            items(exams) { exam ->
                ExamCardItem(
                    exam = exam,
                    onStartExam = { onStartExam(exam.examId) },
                    onViewLeaderboard = { onViewLeaderboard(exam.examId) }
                )
            }
        }
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = color
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = ExamGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ExamCardItem(
    exam: ExamEntity,
    onStartExam: () -> Unit,
    onViewLeaderboard: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("exam_card_${exam.examId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Category and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    text = exam.categoryId.replace("_", " "),
                    containerColor = SoftIndigoBg,
                    contentColor = NavyPrimary
                )

                if (exam.status == ExamStatus.LIVE) {
                    StatusBadge(
                        text = LanguageManager.text("LIVE TEST", "लाइव टेस्ट"),
                        containerColor = ExamRedLight,
                        contentColor = ExamRed
                    )
                } else {
                    StatusBadge(
                        text = LanguageManager.text("SCHEDULED", "निर्धारित"),
                        containerColor = ExamOrangeLight,
                        contentColor = ExamOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title
            Text(
                text = if (LanguageManager.isHindi()) exam.titleHi else exam.titleEn,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = exam.subject,
                style = MaterialTheme.typography.bodySmall.copy(color = ExamGray),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Specs Row: Questions, Marks, Duration
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ExamGrayLight)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SpecItem(
                    label = LanguageManager.text("Questions", "प्रश्न"),
                    value = "${exam.totalQuestions}"
                )
                SpecItem(
                    label = LanguageManager.text("Marks", "पूर्णांक"),
                    value = "${exam.maxMarks.toInt()}"
                )
                SpecItem(
                    label = LanguageManager.text("Duration", "समय"),
                    value = "${exam.durationMinutes} " + LanguageManager.text("min", "मिनट")
                )
                SpecItem(
                    label = LanguageManager.text("Negative", "नकारात्मक"),
                    value = "-${exam.negativeMarks}"
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Participation Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = ExamGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${exam.registeredCount} " + LanguageManager.text("candidates joined", "अभ्यर्थी पंजीकृत"),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ExamGreen,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // Leaderboard quick link
                Text(
                    text = LanguageManager.text("Leaderboard", "लीडरबोर्ड"),
                    color = IndigoAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onViewLeaderboard() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CTA Button
            Button(
                onClick = onStartExam,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("start_test_btn_${exam.examId}"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LanguageManager.text("Join Live Test", "लाइव टेस्ट में शामिल हों"),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SpecItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
        Text(text = label, fontSize = 10.sp, color = ExamGray)
    }
}
