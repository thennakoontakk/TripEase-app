package com.example.tripease

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class FirebasePlaceService {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val placesCollection = firestore.collection("places")
    
    companion object {
        private const val TAG = "FirebasePlaceService"
        private const val PLACES_COLLECTION = "places"
    }
    
    /**
     * Fetch all places from Firestore
     */
    suspend fun getAllPlaces(): List<FirebasePlace> {
        return try {
            val snapshot = placesCollection.get().await()
            val places = snapshot.toObjects(FirebasePlace::class.java)
            Log.d(TAG, "Successfully fetched ${places.size} places from Firestore")
            places
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching places from Firestore", e)
            emptyList()
        }
    }
    
    /**
     * Fetch places with limit for carousel display
     */
    suspend fun getPlacesForCarousel(limit: Long = 10): List<FirebasePlace> {
        return try {
            val snapshot = placesCollection
                .limit(limit)
                .get()
                .await()
            
            Log.d(TAG, "Raw snapshot size: ${snapshot.size()}")
            Log.d(TAG, "Raw snapshot isEmpty: ${snapshot.isEmpty}")
            
            // Log raw document data
            snapshot.documents.forEachIndexed { index, document ->
                Log.d(TAG, "Document $index ID: ${document.id}")
                Log.d(TAG, "Document $index data: ${document.data}")
            }
            
            val places = snapshot.toObjects(FirebasePlace::class.java)
            Log.d(TAG, "Successfully fetched ${places.size} places for carousel")
            
            // Log converted places
            places.forEachIndexed { index, place ->
                Log.d(TAG, "Place $index: name=${place.name}, imageURL=${place.imageURL}, province=${place.province}")
            }
            
            places
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching places for carousel", e)
            emptyList()
        }
    }
    
    /**
     * Fetch places by province
     */
    suspend fun getPlacesByProvince(province: String): List<FirebasePlace> {
        return try {
            val snapshot = placesCollection
                .whereEqualTo("province", province)
                .get()
                .await()
            val places = snapshot.toObjects(FirebasePlace::class.java)
            Log.d(TAG, "Successfully fetched ${places.size} places from $province")
            places
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching places by province", e)
            emptyList()
        }
    }
    
    /**
     * Fetch places by tags
     */
    suspend fun getPlacesByTag(tag: String): List<FirebasePlace> {
        return try {
            val snapshot = placesCollection
                .whereArrayContains("tags", tag)
                .get()
                .await()
            val places = snapshot.toObjects(FirebasePlace::class.java)
            Log.d(TAG, "Successfully fetched ${places.size} places with tag: $tag")
            places
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching places by tag", e)
            emptyList()
        }
    }
    
    /**
     * Get a single place by ID
     */
    suspend fun getPlaceById(placeId: String): FirebasePlace? {
        return try {
            val snapshot = placesCollection.document(placeId).get().await()
            val place = snapshot.toObject(FirebasePlace::class.java)
            Log.d(TAG, "Successfully fetched place: ${place?.name}")
            place
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching place by ID", e)
            null
        }
    }
}