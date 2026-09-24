package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ExamStatus {
    SCHEDULED,
    LIVE,
    COMPLETED
}

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey
    val examId: String,
    val titleEn: String,
    val titleHi: String,
    val categoryId: String, // e.g., "UP_POLICE", "SSC", "TGT", "PGT", "UGC_NET"
    val subject: String,
    val descriptionEn: String,
    val descriptionHi: String,
    val instructionsEn: String,
    val instructionsHi: String,
    val durationMinutes: Int,
    val totalQuestions: Int,
    val maxMarks: Double,
    val marksPerCorrect: Double = 2.0,
    val negativeMarks: Double = 0.5,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val status: ExamStatus = ExamStatus.LIVE,
    val isPublic: Boolean = true,
    val allowRevisit: Boolean = true,
    val showSolutionsAfterSubmit: Boolean = true,
    val registeredCount: Int = 1450,
    val activeCount: Int = 380,
    val submittedCount: Int = 1070
)
