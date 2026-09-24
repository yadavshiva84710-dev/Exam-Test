package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "results")
data class ResultEntity(
    @PrimaryKey
    val resultId: String,
    val examId: String,
    val examTitle: String,
    val attemptId: String,
    val studentId: String,
    val studentName: String,
    val totalScore: Double,
    val maxScore: Double,
    val percentage: Double,
    val correctCount: Int,
    val incorrectCount: Int,
    val unansweredCount: Int,
    val accuracy: Double,
    val rank: Int,
    val totalParticipants: Int,
    val percentile: Double,
    val submittedAt: Long = System.currentTimeMillis()
)

data class LeaderboardEntry(
    val rank: Int,
    val studentName: String,
    val studentId: String,
    val marks: Double,
    val maxMarks: Double,
    val percentage: Double,
    val submissionTimeFormatted: String,
    val state: String = "UP",
    val isCurrentUser: Boolean = false
)
