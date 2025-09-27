package com.example.tripease

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class User(
    @DocumentId
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val phoneNumber: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val location: String = "",
    val bio: String = "",
    val isEmailVerified: Boolean = false,
    val accountStatus: String = "active", // active, suspended, deleted
    val preferences: Map<String, Any> = emptyMap(),
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    // No-argument constructor for Firestore
    constructor() : this(
        uid = "",
        fullName = "",
        email = "",
        profileImageUrl = "",
        phoneNumber = "",
        dateOfBirth = "",
        gender = "",
        location = "",
        bio = "",
        isEmailVerified = false,
        accountStatus = "active",
        preferences = emptyMap(),
        createdAt = null,
        updatedAt = null
    )
    
    // Convert to Map for Firestore
    fun toMap(): Map<String, Any> {
        return mapOf(
            "uid" to uid,
            "fullName" to fullName,
            "email" to email,
            "profileImageUrl" to profileImageUrl,
            "phoneNumber" to phoneNumber,
            "dateOfBirth" to dateOfBirth,
            "gender" to gender,
            "location" to location,
            "bio" to bio,
            "isEmailVerified" to isEmailVerified,
            "accountStatus" to accountStatus,
            "preferences" to preferences,
            "createdAt" to (createdAt ?: Timestamp.now()),
            "updatedAt" to Timestamp.now()
        )
    }
}