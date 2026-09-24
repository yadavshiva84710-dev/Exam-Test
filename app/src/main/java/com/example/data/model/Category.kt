package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val categoryId: String,
    val nameEn: String,
    val nameHi: String,
    val iconName: String,
    val activeTestsCount: Int = 0
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey
    val id: String,
    val titleEn: String,
    val titleHi: String,
    val messageEn: String,
    val messageHi: String,
    val timestamp: Long = System.currentTimeMillis(),
    val priority: String = "NORMAL" // "URGENT", "NORMAL", "EXAM_ALERT"
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val logId: Long = 0L,
    val action: String,
    val performedBy: String,
    val target: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
