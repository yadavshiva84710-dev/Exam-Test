package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    STUDENT,
    EXAM_ADMIN,
    SUPER_ADMIN
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val studentId: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val role: UserRole = UserRole.STUDENT,
    val state: String = "Uttar Pradesh",
    val district: String = "Lucknow",
    val preferredCategory: String = "UP Police",
    val avatarUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE"
)
