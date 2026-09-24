package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserById(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = 'STUDENT' ORDER BY fullName ASC")
    fun getAllStudents(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY startTimestamp DESC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE categoryId = :categoryId ORDER BY startTimestamp DESC")
    fun getExamsByCategory(categoryId: String): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE examId = :examId")
    suspend fun getExamById(examId: String): ExamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<ExamEntity>)

    @Query("DELETE FROM exams WHERE examId = :examId")
    suspend fun deleteExam(examId: String)
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions WHERE examId = :examId ORDER BY orderIndex ASC")
    fun getQuestionsForExamFlow(examId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE examId = :examId ORDER BY orderIndex ASC")
    suspend fun getQuestionsForExam(examId: String): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE questionId = :questionId")
    suspend fun deleteQuestion(questionId: String)
}

@Dao
interface AttemptDao {
    @Query("SELECT * FROM attempts WHERE attemptId = :attemptId")
    suspend fun getAttempt(attemptId: String): AttemptEntity?

    @Query("SELECT * FROM attempts WHERE examId = :examId AND studentId = :studentId ORDER BY startTimestamp DESC LIMIT 1")
    suspend fun getLatestAttempt(examId: String, studentId: String): AttemptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: AttemptEntity)

    @Update
    suspend fun updateAttempt(attempt: AttemptEntity)

    @Query("SELECT * FROM attempt_answers WHERE attemptId = :attemptId")
    fun getAnswersForAttemptFlow(attemptId: String): Flow<List<AttemptAnswerEntity>>

    @Query("SELECT * FROM attempt_answers WHERE attemptId = :attemptId")
    suspend fun getAnswersForAttempt(attemptId: String): List<AttemptAnswerEntity>

    @Query("SELECT * FROM attempt_answers WHERE attemptId = :attemptId AND questionId = :questionId")
    suspend fun getAnswer(attemptId: String, questionId: String): AttemptAnswerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAnswer(answer: AttemptAnswerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(answers: List<AttemptAnswerEntity>)
}

@Dao
interface ResultDao {
    @Query("SELECT * FROM results WHERE attemptId = :attemptId")
    suspend fun getResultForAttempt(attemptId: String): ResultEntity?

    @Query("SELECT * FROM results WHERE studentId = :studentId ORDER BY submittedAt DESC")
    fun getResultsForStudentFlow(studentId: String): Flow<List<ResultEntity>>

    @Query("SELECT * FROM results WHERE examId = :examId ORDER BY totalScore DESC, submittedAt ASC")
    fun getResultsForExamFlow(examId: String): Flow<List<ResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: ResultEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements ORDER BY timestamp DESC")
    fun getAllAnnouncementsFlow(): Flow<List<AnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAllAuditLogsFlow(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)
}
