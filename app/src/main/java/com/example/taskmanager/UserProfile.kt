package com.example.taskmanager

import java.util.UUID

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val createdAt: Long = System.currentTimeMillis()
)