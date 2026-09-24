package com.example.ui.screens.exam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.components.StatusBadge
import com.example.ui.language.LanguageManager
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveExamScreen(
    exam: ExamEntity?,
    questions: List<QuestionEntity>,
    currentIndex: Int,
    answersMap: Map<String, AttemptAnswerEntity>,
    timeRemainingSeconds: Long,
    isSaving: Boolean,
    isSubmitting: Boolean,
    onSelectOption: (questionId: String, option: String) -> Unit,
    onClearOption: (questionId: String) -> Unit,
    onToggleMarkForReview: (questionId: String) -> Unit,
    onGoToQuestion: (Int) -> Unit,
    onNextQuestion: () -> Unit,
    onPreviousQuestion: () -> Unit,
    onSubmitExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showPaletteSheet by remember { mutableStateOf(false) }
    var showSubmitConfirmation by remember { mutableStateOf(false) }

    if (exam == null || questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NavyPrimary)
        }
        return
    }

    val currentQuestion = questions.getOrNull(currentIndex) ?: questions[0]
    val currentAnswer = answersMap[currentQuestion.questionId]

    // Timer formatting
    val minutes = timeRemainingSeconds / 60
    val seconds = timeRemainingSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)
    val isTimeUrgent = timeRemainingSeconds < 120

    // Palette statistics
    var answeredCount = 0
    var notAnsweredCount = 0
    var markedReviewCount = 0
    var notVisitedCount = 0

    questions.forEach { q ->
        val ans = answersMap[q.questionId]
        val status = ans?.computeStatus() ?: QuestionStatus.NOT_VISITED
        when (status) {
            QuestionStatus.ANSWERED -> answeredCount++
            QuestionStatus.ANSWERED_AND_MARKED, QuestionStatus.MARKED_FOR_REVIEW -> markedReviewCount++
            QuestionStatus.NOT_ANSWERED -> notAnsweredCount++
            QuestionStatus.NOT_VISITED -> notVisitedCount++
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = NavyPrimary,
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (LanguageManager.isHindi()) exam.titleHi else exam.titleEn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Sync indicator
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSaving) ExamOrange else ExamGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSaving) LanguageManager.text("Saving...", "सहेजा जा रहा है...")
                                           else LanguageManager.text("Saved & Synced", "सुरक्षित व सिंक"),
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Countdown Timer Box
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isTimeUrgent) ExamRed else Color.White.copy(alpha = 0.18f),
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Timer",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = formattedTime,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Palette Toggle Button
                        IconButton(
                            onClick = { showPaletteSheet = true },
                            modifier = Modifier.testTag("open_palette_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Question Palette",
                                tint = Color.White
                            )
                        }

                        // Submit Button
                        Button(
                            onClick = { showSubmitConfirmation = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ExamGreen),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("top_submit_exam_btn")
                        ) {
                            Text(
                                text = LanguageManager.text("Submit", "सबमिट"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Exam Navigation Action Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    OutlinedButton(
                        onClick = onPreviousQuestion,
                        enabled = currentIndex > 0,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_prev_question")
                    ) {
                        Icon(imageVector = Icons.Default.NavigateBefore, contentDescription = null)
                        Text(LanguageManager.text("Prev", "पिछला"))
                    }

                    // Clear Response Button
                    TextButton(
                        onClick = { onClearOption(currentQuestion.questionId) },
                        enabled = currentAnswer?.selectedOption != null,
                        modifier = Modifier.testTag("btn_clear_response")
                    ) {
                        Text(
                            text = LanguageManager.text("Clear", "हटाएं"),
                            color = if (currentAnswer?.selectedOption != null) ExamRed else ExamGray,
                            fontSize = 12.sp
                        )
                    }

                    // Mark for Review Button
                    IconButton(
                        onClick = { onToggleMarkForReview(currentQuestion.questionId) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (currentAnswer?.isMarkedForReview == true) ExamPurpleLight else Color.Transparent)
                            .testTag("btn_mark_review")
                    ) {
                        Icon(
                            imageVector = if (currentAnswer?.isMarkedForReview == true) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Mark for Review",
                            tint = if (currentAnswer?.isMarkedForReview == true) ExamPurple else ExamGray
                        )
                    }

                    // Save & Next Button
                    Button(
                        onClick = {
                            if (currentIndex < questions.size - 1) {
                                onNextQuestion()
                            } else {
                                showSubmitConfirmation = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_save_next")
                    ) {
                        Text(
                            text = if (currentIndex == questions.size - 1) LanguageManager.text("Review & Finish", "समाप्त करें")
                                   else LanguageManager.text("Save & Next", "सहेजें व आगे बढ़ें"),
                            fontWeight = FontWeight.Bold
                        )
                        Icon(imageVector = Icons.Default.NavigateNext, contentDescription = null)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CleanBgLight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Question Subheader: Number & Marks
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NavyPrimary)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = LanguageManager.text("Question", "प्रश्न") + " ${currentIndex + 1}/${questions.size}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentQuestion.subject,
                        color = ExamGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Marks Tag
                Row {
                    StatusBadge(
                        text = "+${exam.marksPerCorrect}",
                        containerColor = ExamGreenLight,
                        contentColor = ExamGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    StatusBadge(
                        text = "-${exam.negativeMarks}",
                        containerColor = ExamRedLight,
                        contentColor = ExamRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Question Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Question text (Hindi and English)
                    Text(
                        text = if (LanguageManager.isHindi()) currentQuestion.questionHi else currentQuestion.questionEn,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Secondary translation preview if in Hindi
                    if (LanguageManager.isHindi() && currentQuestion.questionEn.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentQuestion.questionEn,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ExamGray,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = LanguageManager.text("Select Correct Option:", "सही विकल्प चुनें:"),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ExamGray,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Options List
            val options = listOf(
                Triple("A", currentQuestion.optionAEn, currentQuestion.optionAHi),
                Triple("B", currentQuestion.optionBEn, currentQuestion.optionBHi),
                Triple("C", currentQuestion.optionCEn, currentQuestion.optionCHi),
                Triple("D", currentQuestion.optionDEn, currentQuestion.optionDHi)
            )

            options.forEach { (key, enText, hiText) ->
                val isSelected = currentAnswer?.selectedOption == key
                OptionItemCard(
                    optionKey = key,
                    text = if (LanguageManager.isHindi()) hiText else enText,
                    secondaryText = if (LanguageManager.isHindi()) enText else "",
                    isSelected = isSelected,
                    onSelect = { onSelectOption(currentQuestion.questionId, key) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    // Question Palette Bottom Sheet
    if (showPaletteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPaletteSheet = false },
            containerColor = Color.White,
            modifier = Modifier.testTag("palette_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = LanguageManager.text("Question Palette", "प्रश्न पैलेट"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = NavyPrimary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Legend Counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PaletteCountBadge(label = LanguageManager.text("Answered", "उत्तरित"), count = answeredCount, color = ExamGreen)
                    PaletteCountBadge(label = LanguageManager.text("Review", "समीक्षा"), count = markedReviewCount, color = ExamPurple)
                    PaletteCountBadge(label = LanguageManager.text("Unanswered", "अनुत्तरित"), count = notAnsweredCount, color = ExamRed)
                    PaletteCountBadge(label = LanguageManager.text("Not Visited", "शेष"), count = notVisitedCount, color = ExamGray)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Questions Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    itemsIndexed(questions) { idx, q ->
                        val ans = answersMap[q.questionId]
                        val status = ans?.computeStatus() ?: QuestionStatus.NOT_VISITED
                        val isCurrent = idx == currentIndex

                        PaletteNumberItem(
                            number = idx + 1,
                            status = status,
                            isCurrent = isCurrent,
                            onClick = {
                                onGoToQuestion(idx)
                                showPaletteSheet = false
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Submit Confirmation Dialog
    if (showSubmitConfirmation) {
        Dialog(onDismissRequest = { if (!isSubmitting) showSubmitConfirmation = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("submit_confirm_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FactCheck,
                        contentDescription = null,
                        tint = NavyPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = LanguageManager.text("Exam Summary & Submission", "परीक्षा सारांश व अंतिम सबमिशन"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Summary Table
                    Surface(
                        color = ExamGrayLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            SummaryRow(LanguageManager.text("Total Questions", "कुल प्रश्न"), "${questions.size}", NavyPrimary)
                            SummaryRow(LanguageManager.text("Answered", "उत्तर दिए गए"), "$answeredCount", ExamGreen)
                            SummaryRow(LanguageManager.text("Marked for Review", "समीक्षा हेतु चिह्नित"), "$markedReviewCount", ExamPurple)
                            SummaryRow(LanguageManager.text("Not Answered", "अनुत्तरित प्रश्न"), "${questions.size - answeredCount}", ExamRed)
                            SummaryRow(LanguageManager.text("Time Remaining", "बचा हुआ समय"), formattedTime, if (isTimeUrgent) ExamRed else ExamGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = LanguageManager.text(
                            "Are you sure you want to submit? Answers once submitted will be final and evaluated automatically.",
                            "क्या आप सुनिश्चित हैं कि आप परीक्षा सबमिट करना चाहते हैं? एक बार सबमिट करने के बाद उत्तर अंतिम माने जाएंगे।"
                        ),
                        fontSize = 12.sp,
                        color = ExamGray,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showSubmitConfirmation = false },
                            enabled = !isSubmitting
                        ) {
                            Text(LanguageManager.text("Resume Test", "वापस जाएं"))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSubmitExam()
                            },
                            enabled = !isSubmitting,
                            colors = ButtonDefaults.buttonColors(containerColor = ExamGreen),
                            modifier = Modifier.testTag("confirm_submit_test_btn")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(LanguageManager.text("Yes, Submit Test", "हां, टेस्ट सबमिट करें"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OptionItemCard(
    optionKey: String,
    text: String,
    secondaryText: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) NavyPrimary else CardBorder,
        label = "border_color"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) SoftIndigoBg else Color.White,
        label = "bg_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("option_card_$optionKey"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) NavyPrimary else ExamGrayLight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = optionKey,
                    color = if (isSelected) Color.White else NavyPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )
                )
                if (secondaryText.isNotBlank() && secondaryText != text) {
                    Text(
                        text = secondaryText,
                        fontSize = 11.sp,
                        color = ExamGray
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = NavyPrimary)
            )
        }
    }
}

@Composable
fun PaletteCountBadge(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$count", fontWeight = FontWeight.Black, fontSize = 16.sp, color = color)
        Text(text = label, fontSize = 10.sp, color = ExamGray)
    }
}

@Composable
fun PaletteNumberItem(
    number: Int,
    status: QuestionStatus,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when (status) {
        QuestionStatus.ANSWERED -> ExamGreen
        QuestionStatus.MARKED_FOR_REVIEW -> ExamPurple
        QuestionStatus.ANSWERED_AND_MARKED -> ExamPurple
        QuestionStatus.NOT_ANSWERED -> ExamRed
        QuestionStatus.NOT_VISITED -> ExamGrayLight
    }
    val textColor = when (status) {
        QuestionStatus.NOT_VISITED -> NavyPrimary
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(48.dp) // Minimum interactive target
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (isCurrent) 2.5.dp else 1.dp,
                color = if (isCurrent) NavyPrimary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .testTag("palette_item_$number"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$number",
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        // Dot for answered & marked
        if (status == QuestionStatus.ANSWERED_AND_MARKED) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(ExamGreen)
            )
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = ExamGray)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
