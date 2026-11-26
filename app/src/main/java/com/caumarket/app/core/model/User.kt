package com.caumarket.app.core.model

import com.google.firebase.Timestamp

/** README section 6 user document. userId is the Firebase Auth UID. */
data class User(
    val userId: String,
    val email: String,
    val nickname: String,
    val createdAt: Timestamp,
)
