package com.example

import com.example.data.model.AttemptAnswerEntity
import com.example.data.model.QuestionStatus
import com.example.ui.language.AppLanguage
import com.example.ui.language.LanguageManager
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testLanguageManager() {
        LanguageManager.setLanguage(AppLanguage.HINDI)
        assertTrue(LanguageManager.isHindi())
        assertEquals("परीक्षण", LanguageManager.text("Test", "परीक्षण"))

        LanguageManager.setLanguage(AppLanguage.ENGLISH)
        assertFalse(LanguageManager.isHindi())
        assertEquals("Test", LanguageManager.text("Test", "परीक्षण"))
    }

    @Test
    fun testQuestionStatusComputation() {
        val notVisited = AttemptAnswerEntity(
            attemptId = "att1",
            questionId = "q1",
            selectedOption = null,
            isMarkedForReview = false,
            isVisited = false
        )
        assertEquals(QuestionStatus.NOT_VISITED, notVisited.computeStatus())

        val notAnswered = AttemptAnswerEntity(
            attemptId = "att1",
            questionId = "q1",
            selectedOption = null,
            isMarkedForReview = false,
            isVisited = true
        )
        assertEquals(QuestionStatus.NOT_ANSWERED, notAnswered.computeStatus())

        val answered = AttemptAnswerEntity(
            attemptId = "att1",
            questionId = "q1",
            selectedOption = "B",
            isMarkedForReview = false,
            isVisited = true
        )
        assertEquals(QuestionStatus.ANSWERED, answered.computeStatus())

        val markedReview = AttemptAnswerEntity(
            attemptId = "att1",
            questionId = "q1",
            selectedOption = null,
            isMarkedForReview = true,
            isVisited = true
        )
        assertEquals(QuestionStatus.MARKED_FOR_REVIEW, markedReview.computeStatus())

        val answeredAndMarked = AttemptAnswerEntity(
            attemptId = "att1",
            questionId = "q1",
            selectedOption = "A",
            isMarkedForReview = true,
            isVisited = true
        )
        assertEquals(QuestionStatus.ANSWERED_AND_MARKED, answeredAndMarked.computeStatus())
    }
}
