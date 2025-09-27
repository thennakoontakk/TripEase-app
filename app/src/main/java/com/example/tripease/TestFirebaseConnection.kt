package com.example.tripease

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Test class to verify Firebase connection and add sample data
 */
class TestFirebaseConnection {
    private val firestore = FirebaseFirestore.getInstance()
    private val placesCollection = firestore.collection("places")
    
    companion object {
        private const val TAG = "TestFirebaseConnection"
    }
    
    /**
     * Test basic Firebase connection
     */
    suspend fun testConnection(): Boolean {
        return try {
            Log.d(TAG, "Testing Firebase connection...")
            val snapshot = placesCollection.limit(1).get().await()
            Log.d(TAG, "Connection successful! Documents found: ${snapshot.size()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            false
        }
    }
    
    /**
     * Add sample place data for testing
     */
    suspend fun addSampleData() {
        try {
            Log.d(TAG, "Adding sample place data...")
            
            val samplePlaces = listOf(
                mapOf(
                    "name" to "Sigiriya Lion Rock",
                    "province" to "Central",
                    "category" to "Heritage",
                    "description" to "An ancient rock fortress and palace built on a massive column of rock, famous for its stunning frescoes and panoramic summit views.",
                    "detailedInfo" to "Often called the eighth wonder of the world, Sigiriya is a UNESCO World Heritage Site. Built by King Kashyapa in the 5th century, the site features ancient landscaped gardens, a gateway in the form of a giant lion, and the famous 'Sigiri Apsaras' frescoes. The climb to the top is rewarding, offering breathtaking views of the surrounding jungle and plains.",
                    "bestTimeToVisit" to "Early morning (7:00 AM) or late afternoon (3:00 PM) to avoid the midday heat.",
                    "location" to com.google.firebase.firestore.GeoPoint(7.9568, 80.7604),
                    "imageURL" to "gs://tripease-app.firebasestorage.app/Sigiriya-in-Sri-Lanka.jpg",
                    "tags" to listOf("UNESCO", "history", "hiking")
                )
            )
            
            samplePlaces.forEachIndexed { index, place ->
                placesCollection.document("sample_place_$index").set(place).await()
                Log.d(TAG, "Added sample place: ${place["name"]}")
            }
            
            Log.d(TAG, "Sample data added successfully!")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error adding sample data", e)
        }
    }
}