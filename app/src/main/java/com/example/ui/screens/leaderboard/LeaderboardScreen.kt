package com.example.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamEntity
import com.example.data.model.LeaderboardEntry
import com.example.ui.components.LiveDotIndicator
import com.example.ui.components.StatusBadge
import com.example.ui.language.LanguageManager
import com.example.ui.theme.*

@Composable
fun LeaderboardScreen(
    exam: ExamEntity?,
    leaderboardEntries: List<LeaderboardEntry>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CleanBgLight)
            .testTag("leaderboard_screen_scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Top Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = LanguageManager.text("LIVE LEADERBOARD", "लाइव लीडरबोर्ड"),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            LiveDotIndicator()
                        }

                        IconButton(onClick = { /* Refresh */ }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (exam != null) {
                            if (LanguageManager.isHindi()) exam.titleHi else exam.titleEn
                        } else "Junior Testbook Ranking",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )

                    Text(
                        text = LanguageManager.text(
                            en = "Live provisional ranking updated automatically as verified submissions arrive.",
                            hi = "सत्यापित सबमिशन प्राप्त होने पर रीयल-टाइम रैंक स्वचालित रूप से अपडेट होती है।"
                        ),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // 2. Tie-Breaking & Disclaimers Card
        item {
            Surface(
                color = SoftIndigoBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NavyPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LanguageManager.text(
                            en = "Tie-breaking rules: 1st by Total Marks obtained, 2nd by Fastest Submission Time. This rank is specific to the Junior Testbook platform.",
                            hi = "टाई-ब्रेकिंग नियम: 1. कुल प्राप्तांक, 2. सबसे तीव्र सबमिशन समय। यह रैंक जूनियर टेस्टबुक परीक्षा पर आधारित है।"
                        ),
                        fontSize = 11.sp,
                        color = NavyPrimary,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // 3. Top 3 Podium Cards if available
        if (leaderboardEntries.size >= 3) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Rank 2 (Silver)
                    PodiumCard(entry = leaderboardEntries[1], rank = 2, color = Color(0xFF94A3B8), modifier = Modifier.weight(1f))
                    // Rank 1 (Gold)
                    PodiumCard(entry = leaderboardEntries[0], rank = 1, color = Color(0xFFF59E0B), isFirst = true, modifier = Modifier.weight(1.15f))
                    // Rank 3 (Bronze)
                    PodiumCard(entry = leaderboardEntries[2], rank = 3, color = Color(0xFFD97706), modifier = Modifier.weight(1f))
                }
            }
        }

        // 4. Detailed Ranking Table
        items(leaderboardEntries) { entry ->
            val isUser = entry.isCurrentUser
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("leaderboard_row_${entry.rank}"),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) SoftIndigoBg else Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isUser) 2.dp else 1.dp,
                    color = if (isUser) NavyPrimary else CardBorder
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank badge
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                when (entry.rank) {
                                    1 -> Color(0xFFFEF3C7)
                                    2 -> Color(0xFFF1F5F9)
                                    3 -> Color(0xFFFFEDD5)
                                    else -> CleanBgLight
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${entry.rank}",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = when (entry.rank) {
                                1 -> Color(0xFFD97706)
                                2 -> Color(0xFF475569)
                                3 -> Color(0xFFB45309)
                                else -> NavyPrimary
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.studentName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isUser) NavyPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isUser) {
                                Spacer(modifier = Modifier.width(4.dp))
                                StatusBadge(
                                    text = "YOU",
                                    containerColor = NavyPrimary,
                                    contentColor = Color.White
                                )
                            }
                        }
                        Text(
                            text = "${entry.studentId} • ${entry.state}",
                            fontSize = 11.sp,
                            color = ExamGray
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${entry.marks} / ${entry.maxMarks.toInt()}",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = if (isUser) NavyPrimary else ExamGreen
                        )
                        Text(
                            text = "${entry.percentage}% • ${entry.submissionTimeFormatted}",
                            fontSize = 10.sp,
                            color = ExamGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumCard(
    entry: LeaderboardEntry,
    rank: Int,
    color: Color,
    isFirst: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.heightIn(min = if (isFirst) 130.dp else 115.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFirst) 4.dp else 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.studentName.take(12),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1
            )
            Text(
                text = "${entry.marks} M",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = NavyPrimary
            )
            Text(
                text = "${entry.percentage}%",
                fontSize = 10.sp,
                color = ExamGreen,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
