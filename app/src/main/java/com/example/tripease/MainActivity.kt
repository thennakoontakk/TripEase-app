package com.example.tripease

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var openTripMapService: OpenTripMapService
    
    // UI Components for nearest places
    private lateinit var recyclerViewNearestPlaces: RecyclerView
    private lateinit var progressBarPlaces: ProgressBar
    private lateinit var tvNoPlaces: TextView
    private lateinit var tvViewAllPlaces: TextView
    private lateinit var placesAdapter: PlaceAdapter
    
    // UI Components for Firebase places carousel
    private lateinit var carouselFeaturedPlaces: AutoSlidingCarousel
    private lateinit var progressBarCarousel: ProgressBar
    private lateinit var carouselAdapter: PlaceCarouselAdapter
    private lateinit var firebasePlaceService: FirebasePlaceService
    
    // UI Components for user greeting
    private lateinit var tvUserGreeting: TextView
    
    private val nearestPlaces = mutableListOf<TravelPlace>()
    private val featuredPlaces = mutableListOf<FirebasePlace>()
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Firebase Analytics
        firebaseAnalytics = Firebase.analytics
        
        // Initialize Firebase Auth
        auth = Firebase.auth
        
        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()
        
        // Initialize Firebase Place Service
        firebasePlaceService = FirebasePlaceService()
        
        // Log an event to test Firebase connection
        val bundle = Bundle()
        bundle.putString("app_opened", "main_activity")
        firebaseAnalytics.logEvent("app_start", bundle)
        
        // Initialize location client and API service
        setupLocationClient()
        setupApiService()
        
        // Initialize UI components
        initViews()
        
        // Initialize bottom navigation
        setupBottomNavigation()
        
        // Load nearest places
        loadNearestPlaces()
        
        // Test Firebase connection and add sample data if needed
        testFirebaseAndLoadPlaces()
        
        // Load featured places for carousel
        loadFeaturedPlaces()
    }
    
    private fun initViews() {
        // Initialize user greeting
        tvUserGreeting = findViewById(R.id.tvUserGreeting)
        
        recyclerViewNearestPlaces = findViewById(R.id.recyclerViewNearestPlaces)
        progressBarPlaces = findViewById(R.id.progressBarPlaces)
        tvNoPlaces = findViewById(R.id.tvNoPlaces)
        tvViewAllPlaces = findViewById(R.id.tvViewAllPlaces)
        
        // Initialize carousel components
        carouselFeaturedPlaces = findViewById(R.id.carouselFeaturedPlaces)
        progressBarCarousel = findViewById(R.id.progressBarCarousel)
        
        setupRecyclerView()
        setupCarousel()
        
        // Load user profile and display greeting
        loadUserProfile()
        
        // Setup view all places click
        tvViewAllPlaces.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }
    }
    
    private fun setupRecyclerView() {
        placesAdapter = PlaceAdapter(
            places = nearestPlaces,
            onPlaceClick = { place ->
                // Navigate to map activity with selected place
                val intent = Intent(this, MapActivity::class.java)
                intent.putExtra("selected_place_id", place.id)
                startActivity(intent)
            },
            onFavoriteClick = { place ->
                // Handle favorite toggle
                place.isFavorite = !place.isFavorite
                placesAdapter.notifyDataSetChanged()
                Toast.makeText(this, 
                    if (place.isFavorite) "Added to favorites" else "Removed from favorites", 
                    Toast.LENGTH_SHORT).show()
            }
        )
        
        recyclerViewNearestPlaces.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = placesAdapter
        }
    }
    
    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }
    
    private fun setupApiService() {
        val retrofit = Retrofit.Builder()
            .baseUrl(OpenTripMapService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        openTripMapService = retrofit.create(OpenTripMapService::class.java)
    }
    
    private fun loadNearestPlaces() {
        if (checkLocationPermission()) {
            getCurrentLocationAndLoadPlaces()
        } else {
            requestLocationPermission()
        }
    }
    
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocationAndLoadPlaces()
            } else {
                // Permission denied, load sample places
                loadSamplePlaces()
            }
        }
    }
    
    /**
     * Load user profile from Firestore and display greeting
     */
    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in, get their profile from Firestore
            firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val fullName = document.getString("fullName")
                        if (!fullName.isNullOrEmpty()) {
                            tvUserGreeting.text = "Hello $fullName"
                        } else {
                            // Fallback to display name or email
                            val displayName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "User"
                            tvUserGreeting.text = "Hello $displayName"
                        }
                    } else {
                        // Document doesn't exist, use Firebase Auth display name or email
                        val displayName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "User"
                        tvUserGreeting.text = "Hello $displayName"
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error getting user profile", exception)
                    // Fallback to Firebase Auth display name or email
                    val displayName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "User"
                    tvUserGreeting.text = "Hello $displayName"
                }
        } else {
            // User is not signed in, show generic greeting
            tvUserGreeting.text = "Hello Guest"
        }
    }
    
    private fun getCurrentLocationAndLoadPlaces() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        progressBarPlaces.visibility = View.VISIBLE
        tvNoPlaces.visibility = View.GONE
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                Log.d(TAG, "Got location: ${location.latitude}, ${location.longitude}")
                loadTravelPlaces(location.latitude, location.longitude)
            } else {
                Log.d(TAG, "Location is null, loading sample places")
                loadSamplePlaces()
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to get location", exception)
            loadSamplePlaces()
        }
    }
    
    private fun loadTravelPlaces(latitude: Double, longitude: Double) {
        lifecycleScope.launch {
            try {
                val response = openTripMapService.getPlacesInRadius(
                    longitude = longitude,
                    latitude = latitude,
                    limit = 5, // Only get 5 nearest places
                    apiKey = OpenTripMapService.API_KEY
                )
                
                if (response.isSuccessful) {
                    response.body()?.let { mapResponse ->
                        processTravelPlaces(mapResponse, latitude, longitude)
                    }
                } else {
                    Log.e(TAG, "API Error: ${response.code()}")
                    loadSamplePlaces()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network Error: ${e.message}")
                loadSamplePlaces()
            } finally {
                progressBarPlaces.visibility = View.GONE
            }
        }
    }
    
    private fun processTravelPlaces(mapResponse: OpenTripMapResponse, userLat: Double, userLon: Double) {
        nearestPlaces.clear()
        
        mapResponse.features.forEach { feature ->
            val coordinates = feature.geometry.coordinates
            if (coordinates.size >= 2) {
                val longitude = coordinates[0]
                val latitude = coordinates[1]
                
                val distance = calculateDistance(userLat, userLon, latitude, longitude)
                
                val travelPlace = TravelPlace(
                    id = feature.properties.xid,
                    name = feature.properties.name.ifEmpty { "Unknown Place" },
                    description = "",
                    category = getCategoryFromKinds(feature.properties.kinds),
                    latitude = latitude,
                    longitude = longitude,
                    rating = feature.properties.rate.toDouble(),
                    distance = distance
                )
                
                nearestPlaces.add(travelPlace)
            }
        }
        
        // Sort by distance and take only 5
        nearestPlaces.sortBy { it.distance }
        if (nearestPlaces.size > 5) {
            nearestPlaces.subList(5, nearestPlaces.size).clear()
        }
        
        // Update UI
        updatePlacesUI()
    }
    
    private fun loadSamplePlaces() {
        Log.d(TAG, "Loading sample places for home screen")
        
        val samplePlaces = listOf(
            TravelPlace(
                id = "1",
                name = "Sigiriya Rock Fortress",
                description = "Ancient rock fortress and palace ruins",
                category = "Historic",
                latitude = 7.9570,
                longitude = 80.7603,
                rating = 4.8,
                distance = 120.5
            ),
            TravelPlace(
                id = "2",
                name = "Temple of the Tooth",
                description = "Sacred Buddhist temple in Kandy",
                category = "Religious",
                latitude = 7.2936,
                longitude = 80.6414,
                rating = 4.6,
                distance = 85.2
            ),
            TravelPlace(
                id = "3",
                name = "Galle Fort",
                description = "Historic fortified city",
                category = "Historic",
                latitude = 6.0329,
                longitude = 80.2168,
                rating = 4.5,
                distance = 150.8
            ),
            TravelPlace(
                id = "4",
                name = "Ella Rock",
                description = "Scenic hiking destination",
                category = "Natural",
                latitude = 6.8667,
                longitude = 81.0500,
                rating = 4.7,
                distance = 95.3
            ),
            TravelPlace(
                id = "5",
                name = "Yala National Park",
                description = "Wildlife sanctuary and national park",
                category = "Natural",
                latitude = 6.3725,
                longitude = 81.5185,
                rating = 4.4,
                distance = 180.7
            )
        )
        
        nearestPlaces.clear()
        nearestPlaces.addAll(samplePlaces)
        
        // Sort by distance and take only 5
        nearestPlaces.sortBy { it.distance }
        
        updatePlacesUI()
    }
    
    private fun updatePlacesUI() {
        progressBarPlaces.visibility = View.GONE
        
        if (nearestPlaces.isEmpty()) {
            tvNoPlaces.visibility = View.VISIBLE
            recyclerViewNearestPlaces.visibility = View.GONE
        } else {
            tvNoPlaces.visibility = View.GONE
            recyclerViewNearestPlaces.visibility = View.VISIBLE
            placesAdapter.notifyDataSetChanged()
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    private fun getCategoryFromKinds(kinds: String): String {
        return when {
            kinds.contains("museums") -> "Museum"
            kinds.contains("historic") -> "Historic"
            kinds.contains("natural") -> "Natural"
            kinds.contains("cultural") -> "Cultural"
            kinds.contains("architecture") -> "Architecture"
            kinds.contains("tourist_attractions") -> "Attraction"
            else -> "Other"
        }
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_home
        
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on home screen
                    true
                }
                R.id.nav_map -> {
                    startActivity(Intent(this, MapActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_itinerary -> {
                    // TODO: Navigate to itinerary activity
                    Toast.makeText(this, "Itinerary - Coming Soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupCarousel() {
        carouselAdapter = PlaceCarouselAdapter(
            places = featuredPlaces,
            onPlaceClick = { place ->
                // Handle place click - could navigate to details or map
                Toast.makeText(this, "Clicked on ${place.name}", Toast.LENGTH_SHORT).show()
                // TODO: Navigate to place details or map activity
            }
        )
        
        carouselFeaturedPlaces.setAdapter(carouselAdapter)
        carouselFeaturedPlaces.setAutoSlideDelay(3000) // 3 seconds
        carouselFeaturedPlaces.enableAutoSlide(true)
    }
    
    private fun loadFeaturedPlaces() {
        progressBarCarousel.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val places = firebasePlaceService.getPlacesForCarousel(5) // Get 5 places for carousel
                Log.d(TAG, "Fetched ${places.size} places from Firebase")
                places.forEach { place ->
                    Log.d(TAG, "Place: ${place.name}, Image URL: ${place.imageURL}")
                }
                
                featuredPlaces.clear()
                featuredPlaces.addAll(places)
                
                runOnUiThread {
                    progressBarCarousel.visibility = View.GONE
                    carouselAdapter.notifyDataSetChanged()
                    
                    if (places.isNotEmpty()) {
                        carouselFeaturedPlaces.startAutoSlide()
                    } else {
                        Log.w(TAG, "No places found in Firebase")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading featured places", e)
                runOnUiThread {
                    progressBarCarousel.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Failed to load featured places: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        // Resume auto-sliding when activity becomes visible
        if (::carouselFeaturedPlaces.isInitialized && featuredPlaces.isNotEmpty()) {
            carouselFeaturedPlaces.startAutoSlide()
        }
        
        // Check if user is signed in (non-null) and update UI accordingly
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // User is not signed in, redirect to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    
    override fun onStop() {
        super.onStop()
        // Pause auto-sliding when activity is not visible
        if (::carouselFeaturedPlaces.isInitialized) {
            carouselFeaturedPlaces.stopAutoSlide()
        }
    }
    
    /**
     * Test Firebase connection and add sample data if needed
     */
    private fun testFirebaseAndLoadPlaces() {
        lifecycleScope.launch {
            try {
                val testConnection = TestFirebaseConnection()
                
                // Test connection first
                val isConnected = testConnection.testConnection()
                Log.d(TAG, "Firebase connection test result: $isConnected")
                
                if (isConnected) {
                    // Check if we have any data
                    val places = firebasePlaceService.getPlacesForCarousel(1)
                    if (places.isEmpty()) {
                        Log.d(TAG, "No places found, adding sample data...")
                        testConnection.addSampleData()
                    } else {
                        Log.d(TAG, "Places already exist in database")
                    }
                } else {
                    Log.e(TAG, "Firebase connection failed")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in Firebase test", e)
            }
        }
    }
}