package com.example.sup // Adjust to your package name

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null
)