package com.idsr_project.Model

import com.google.gson.annotations.SerializedName

data class NotificationsResponse(
    val success: Boolean,
    val unread_count: Int,
    val total_records: Int,
    val total_pages: Int,
    val page: Int,
    val data: List<NotificationData>
)

data class NotificationData(
    val id: Int,
    val user_id: Int,
    val title: String,
    val body: String,
    val type: String,
    val reference_id: Int?,
    val reference_type: String?,
    val is_read: Boolean,
    val created_at: String
)