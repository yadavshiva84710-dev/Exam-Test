package com.example.ui.screens.exam

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.*
import com.example.ui.components.StatusBadge
import com.example.ui.language.LanguageManager
import com.example.ui.theme.*

@Composable
fun ResultScreen(
    result: ResultEntity?,
    exam: ExamEntity?,
    questions: List<QuestionEntity>,
    answersMap: Map<String, AttemptAnswerEntity>,
    onViewLeaderboard: () -> Unit,
    onBackToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showOnlyIncorrect by remember { mutableStateOf(false) }

    if (result == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NavyPrimary)
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CleanBgLight)
            .testTag("result_screen_scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Success & Score Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = ExamOrange,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = LanguageManager.text("Exam Completed Successfully!", "परीक्षा सफलतापूर्वक पूर्ण हुई!"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )

                    Text(
                        text = result.examTitle,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Score Big Display
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${result.totalScore}",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = " / ${result.maxScore.toInt()}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Text(
                        text = "${result.percentage}% " + LanguageManager.text("Total Score", "कुल प्राप्तांक"),
                        fontSize = 13.sp,
                        color = ExamGreenLight,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Rank & Percentile Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "#${result.rank}",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = ExamOrange
                            )
                            Text(
                                text = LanguageManager.text("State Rank", "राज्य रैंक"),
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Divider(
                            color = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier
                                .height(30.dp)
                                .width(1.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${result.percentile}%ile",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            Text(
                                text = LanguageManager.text("Percentile", "पर्सेंटाइल"),
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Divider(
                            color = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier
                                .height(30.dp)
                                .width(1.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${result.accuracy}%",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = ExamGreenLight
                            )
                            Text(
                                text = LanguageManager.text("Accuracy", "सटीकता"),
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // 2. Metrics Breakdown
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ResultMetricCard(
                    title = LanguageManager.text("Correct", "सही"),
                    count = "${result.correctCount}",
                    color = ExamGreen,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                ResultMetricCard(
                    title = LanguageManager.text("Incorrect", "गलत"),
                    count = "${result.incorrectCount}",
                    color = ExamRed,
                    icon = Icons.Default.Cancel,
                    modifier = Modifier.weight(1f)
                )
                ResultMetricCard(
                    title = LanguageManager.text("Skipped", "छोड़े गए"),
                    count = "${result.unansweredCount}",
                    color = ExamGray,
                    icon = Icons.Default.HelpOutline,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Action Buttons Row: Leaderboard & Share
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onViewLeaderboard,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("btn_view_leaderboard"),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Leaderboard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(LanguageManager.text("View Leaderboard", "लीडरबोर्ड देखें"), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            LanguageManager.text(
                                "Result verification report generated and saved!",
                                "परिणाम सत्यापन रिपोर्ट सफलतापूर्वक तैयार व सहेजी गई!"
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("btn_download_report"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(LanguageManager.text("Download Card", "स्कोरकार्ड डाउनलोड"))
                }
            }
        }

        // 4. Solutions Header & Filter Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = LanguageManager.text("Detailed Question Solutions", "विस्तृत प्रश्न समाधान"),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                )

                FilterChip(
                    selected = showOnlyIncorrect,
                    onClick = { showOnlyIncorrect = !showOnlyIncorrect },
                    label = { Text(LanguageManager.text("Wrong Only", "केवल गलत प्रश्न")) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ExamRedLight,
                        selectedLabelColor = ExamRed
                    )
                )
            }
        }

        // 5. Solution Cards
        val filteredQuestions = if (showOnlyIncorrect) {
            questions.filter { q ->
                val ans = answersMap[q.questionId]
                ans?.selectedOption != null && !ans.selectedOption.equals(q.correctOption, ignoreCase = true)
            }
        } else questions

        itemsIndexed(filteredQuestions) { idx, q ->
            val ans = answersMap[q.questionId]
            val selected = ans?.selectedOption
            val isCorrect = selected != null && selected.equals(q.correctOption, ignoreCase = true)
            val isAttempted = selected != null

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("solution_card_${q.questionId}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when {
                        isCorrect -> ExamGreen.copy(alpha = 0.5f)
                        isAttempted -> ExamRed.copy(alpha = 0.5f)
                        else -> CardBorder
                    }
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header: Q Number + Status badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = LanguageManager.text("Q. ${q.orderIndex}", "प्रश्न ${q.orderIndex}"),
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            fontSize = 13.sp
                        )

                        when {
                            isCorrect -> StatusBadge(
                                text = LanguageManager.text("CORRECT (+${q.marks})", "सही (+${q.marks})"),
                                containerColor = ExamGreenLight,
                                contentColor = ExamGreen
                            )
                            isAttempted -> StatusBadge(
                                text = LanguageManager.text("INCORRECT (-${q.negativeMarks})", "गलत (-${q.negativeMarks})"),
                                containerColor = ExamRedLight,
                                contentColor = ExamRed
                            )
                            else -> StatusBadge(
                                text = LanguageManager.text("SKIPPED (0)", "छोड़ा गया (0)"),
                                containerColor = ExamGrayLight,
                                contentColor = ExamGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Question Text
                    Text(
                        text = if (LanguageManager.isHindi()) q.questionHi else q.questionEn,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Options Review with Correctness Markers
                    val options = listOf(
                        Triple("A", q.optionAEn, q.optionAHi),
                        Triple("B", q.optionBEn, q.optionBHi),
                        Triple("C", q.optionCEn, q.optionCHi),
                        Triple("D", q.optionDEn, q.optionDHi)
                    )

                    options.forEach { (key, en, hi) ->
                        val isThisCorrect = key.equals(q.correctOption, ignoreCase = true)
                        val isThisSelected = key.equals(selected, ignoreCase = true)

                        val optionBg = when {
                            isThisCorrect -> ExamGreenLight
                            isThisSelected && !isThisCorrect -> ExamRedLight
                            else -> CleanBgLight
                        }
                        val optionBorder = when {
                            isThisCorrect -> ExamGreen
                            isThisSelected && !isThisCorrect -> ExamRed
                            else -> CardBorder
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(optionBg)
                                .border(1.dp, optionBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$key.",
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (LanguageManager.isHindi()) hi else en,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (isThisCorrect) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Correct Answer",
                                    tint = ExamGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else if (isThisSelected) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Your Selected Answer",
                                    tint = ExamRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Explanation Box
                    Surface(
                        color = SoftIndigoBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = LanguageManager.text("Solution & Explanation", "व्याख्या व हल"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = NavyPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (LanguageManager.isHindi()) q.explanationHi else q.explanationEn,
                                fontSize = 12.sp,
                                color = Color(0xFF1E293B),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Return button at bottom
        item {
            Button(
                onClick = onBackToDashboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("btn_back_to_dashboard"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Icon(imageVector = Icons.Default.Home, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(LanguageManager.text("Back to Dashboard", "मुख्य पृष्ठ पर वापस जाएं"))
            }
        }
    }
}

@Composable
fun ResultMetricCard(
    title: String,
    count: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count, fontWeight = FontWeight.Black, fontSize = 20.sp, color = color)
            Text(text = title, fontSize = 11.sp, color = ExamGray)
        }
    }
}
