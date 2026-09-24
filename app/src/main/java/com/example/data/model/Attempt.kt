package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey
    val attemptId: String,
    val examId: String,
    val studentId: String,
    val startTimestamp: Long,
    val deadlineTimestamp: Long,
    val submittedTimestamp: Long = 0L,
    val isSubmitted: Boolean = false,
    val currentQuestionIndex: Int = 0
)

@Entity(
    tableName = "attempt_answers",
    indices = [Index(value = ["attemptId", "questionId"], unique = true)]
)
data class AttemptAnswerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val attemptId: String,
    val questionId: String,
    val selectedOption: String? = null, // "A", "B", "C", "D"
    val isMarkedForReview: Boolean = false,
    val isVisited: Boolean = false,
    val savedTimestamp: Long = System.currentTimeMillis(),
    val isSyncedToServer: Boolean = true
) {
    fun computeStatus(): QuestionStatus {
        return when {
            !isVisited -> QuestionStatus.NOT_VISITED
            selectedOption != null && isMarkedForReview -> QuestionStatus.ANSWERED_AND_MARKED
            selectedOption != null -> QuestionStatus.ANSWERED
            isMarkedForReview -> QuestionStatus.MARKED_FOR_REVIEW
            else -> QuestionStatus.NOT_ANSWERED
        }
    }
}
