package com.caumarket.app.core.model
import com.google.firebase.Timestamp
data class Message(val messageId: String, val senderId: String, val type: String,
 val text: String?, val imageUrls: List<String>, val createdAt: Timestamp, val sequence: Long)
