package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class QuestionStatus {
    NOT_VISITED,
    NOT_ANSWERED,
    ANSWERED,
    MARKED_FOR_REVIEW,
    ANSWERED_AND_MARKED
}

@Entity(tableName = "questions")
data class QuestionEntity(
    @PrimaryKey
    val questionId: String,
    val examId: String,
    val orderIndex: Int,
    val questionEn: String,
    val questionHi: String,
    val optionAEn: String,
    val optionAHi: String,
    val optionBEn: String,
    val optionBHi: String,
    val optionCEn: String,
    val optionCHi: String,
    val optionDEn: String,
    val optionDHi: String,
    val correctOption: String, // "A", "B", "C", "D"
    val explanationEn: String,
    val explanationHi: String,
    val subject: String,
    val topic: String,
    val difficulty: String = "Medium",
    val marks: Double = 2.0,
    val negativeMarks: Double = 0.5
)
