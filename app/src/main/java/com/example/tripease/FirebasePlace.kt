package com.example.tripease

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

data class FirebasePlace(
    @DocumentId
    val id: String = "",
    
    @PropertyName("name")
    val name: String = "",
    
    @PropertyName("description")
    val description: String = "",
    
    @PropertyName("detailedInfo")
    val detailedInfo: String = "",
    
    @PropertyName("imageURL")
    val imageURL: String = "",
    
    @PropertyName("location")
    val location: GeoPoint? = null,
    
    @PropertyName("province")
    val province: String = "",
    
    @PropertyName("tags")
    val tags: List<String> = emptyList(),
    
    @PropertyName("category")
    val category: String = "",
    
    @PropertyName("bestTimeToVisit")
    val bestTimeToVisit: String = ""
) {
    // No-argument constructor for Firestore
    constructor() : this(
        id = "",
        name = "",
        description = "",
        detailedInfo = "",
        imageURL = "",
        location = null,
        province = "",
        tags = emptyList(),
        category = "",
        bestTimeToVisit = ""
    )
    
    // Helper properties for accessing coordinates
    val latitude: Double
        get() = location?.latitude ?: 0.0
    
    val longitude: Double
        get() = location?.longitude ?: 0.0
    
    // Convert gs:// URL to HTTP download URL
    val downloadImageURL: String
        get() = if (imageURL.startsWith("gs://")) {
            // Convert gs://bucket/path to https://firebasestorage.googleapis.com/v0/b/bucket/o/path?alt=media
            val bucketAndPath = imageURL.removePrefix("gs://")
            val parts = bucketAndPath.split("/", limit = 2)
            if (parts.size == 2) {
                val bucket = parts[0]
                val path = parts[1].replace("/", "%2F")
                "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$path?alt=media"
            } else {
                imageURL
            }
        } else {
            imageURL
        }
}