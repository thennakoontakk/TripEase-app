package com.example.tripease

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var openTripMapService: OpenTripMapService
    
    // UI Components
    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerViewPlaces: RecyclerView
    private lateinit var placesAdapter: PlaceAdapter
    private lateinit var bottomNavigation: BottomNavigationView
    
    private var currentLocation: LatLng? = null
    private var selectedPlace: TravelPlace? = null
    private val travelPlaces = mutableListOf<TravelPlace>()
    
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "MapActivity"
        // Sri Lanka coordinates
        private val SRI_LANKA_CENTER = LatLng(7.8731, 80.7718)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        
        Log.d(TAG, "MapActivity onCreate started")
        
        // Check Google Play Services first
        if (!checkGooglePlayServices()) {
            showError("Google Play Services is not available. Please update Google Play Services.")
            return
        }
        
        try {
            initViews()
            setupToolbar()
            setupRetrofit()
            setupMap()
            setupLocationClient()
            Log.d(TAG, "MapActivity initialization completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during MapActivity initialization", e)
            showError("Failed to initialize map activity: ${e.message}")
        }
    }
    
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        progressBar = findViewById(R.id.progressBar)
        recyclerViewPlaces = findViewById(R.id.recyclerViewPlaces)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        
        setupRecyclerView()
        setupBottomNavigation()
    }
    
    private fun setupRecyclerView() {
        placesAdapter = PlaceAdapter(
            places = travelPlaces,
            onPlaceClick = { place ->
                selectedPlace = place
                showPlaceCard(place)
                // Move camera to selected place
                val latLng = LatLng(place.latitude, place.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
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
        
        recyclerViewPlaces.apply {
            layoutManager = LinearLayoutManager(this@MapActivity)
            adapter = placesAdapter
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun setupRetrofit() {
        val retrofit = Retrofit.Builder()
            .baseUrl(OpenTripMapService.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        openTripMapService = retrofit.create(OpenTripMapService::class.java)
    }
    
    private fun setupMap() {
        try {
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.mapFragment) as? SupportMapFragment
            
            if (mapFragment != null) {
                mapFragment.getMapAsync(this)
            } else {
                showError("Map fragment not found. Please check your layout.")
            }
        } catch (e: Exception) {
            showError("Failed to initialize map: ${e.message}")
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e("MapActivity", message)
    }
    
    private fun checkGooglePlayServices(): Boolean {
        return try {
            val googleApiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
            
            if (resultCode == com.google.android.gms.common.ConnectionResult.SUCCESS) {
                Log.d(TAG, "Google Play Services is available")
                true
            } else {
                Log.e(TAG, "Google Play Services not available. Result code: $resultCode")
                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    googleApiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
                }
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Google Play Services", e)
            false
        }
    }
    
    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onMapReady(map: GoogleMap) {
        try {
            Log.d(TAG, "onMapReady called - Map is ready!")
            googleMap = map
            googleMap.setOnMarkerClickListener(this)
            
            // Set initial camera position to Sri Lanka with better zoom
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(SRI_LANKA_CENTER, 8f))
            
            // Load Sri Lankan travel places immediately
            loadSamplePlaces()
            
            // Check location permission for user location (optional)
            if (checkLocationPermission()) {
                enableUserLocation()
            } else {
                requestLocationPermission()
            }
            
            Log.d(TAG, "Map setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onMapReady", e)
            showError("Failed to setup map: ${e.message}")
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
    
    private fun enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation()
            }
            // Places are already loaded, no need to reload
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
        
        googleMap.isMyLocationEnabled = true
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLocation = LatLng(location.latitude, location.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 10f))
                loadTravelPlaces(location.latitude, location.longitude)
            } else {
                // Fallback to Sri Lanka center
                loadTravelPlaces(SRI_LANKA_CENTER.latitude, SRI_LANKA_CENTER.longitude)
            }
        }
    }
    
    private fun loadTravelPlaces(latitude: Double, longitude: Double) {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val response = openTripMapService.getPlacesInRadius(
                    longitude = longitude,
                    latitude = latitude,
                    apiKey = OpenTripMapService.API_KEY
                )
                
                if (response.isSuccessful) {
                    response.body()?.let { mapResponse ->
                        processTravelPlaces(mapResponse, latitude, longitude)
                    }
                } else {
                    Log.e(TAG, "API Error: ${response.code()}")
                    loadSamplePlaces() // Fallback to sample data
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network Error: ${e.message}")
                loadSamplePlaces() // Fallback to sample data
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun processTravelPlaces(mapResponse: OpenTripMapResponse, userLat: Double, userLon: Double) {
        travelPlaces.clear()
        googleMap.clear()
        
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
                
                travelPlaces.add(travelPlace)
                addMarkerToMap(travelPlace)
            }
        }
        
        // Update RecyclerView with new places
        placesAdapter.notifyDataSetChanged()
    }
    
    private fun loadSamplePlaces() {
        Log.d(TAG, "Loading sample Sri Lankan places...")
        
        // Sample Sri Lankan travel places as fallback
        val samplePlaces = listOf(
            TravelPlace(
                id = "1",
                name = "Sigiriya Rock Fortress",
                description = "Ancient rock fortress and palace ruins",
                category = "Historic",
                latitude = 7.9570,
                longitude = 80.7603,
                rating = 4.8
            ),
            TravelPlace(
                id = "2",
                name = "Temple of the Tooth",
                description = "Sacred Buddhist temple in Kandy",
                category = "Religious",
                latitude = 7.2936,
                longitude = 80.6414,
                rating = 4.6
            ),
            TravelPlace(
                id = "3",
                name = "Galle Fort",
                description = "Historic fortified city",
                category = "Historic",
                latitude = 6.0329,
                longitude = 80.2168,
                rating = 4.5
            ),
            TravelPlace(
                id = "4",
                name = "Ella Rock",
                description = "Scenic hiking destination",
                category = "Natural",
                latitude = 6.8667,
                longitude = 81.0500,
                rating = 4.7
            ),
            TravelPlace(
                id = "5",
                name = "Yala National Park",
                description = "Wildlife sanctuary and national park",
                category = "Natural",
                latitude = 6.3725,
                longitude = 81.5185,
                rating = 4.4
            )
        )
        
        travelPlaces.clear()
        travelPlaces.addAll(samplePlaces)
        
        Log.d(TAG, "Added ${samplePlaces.size} sample places to list")
        
        currentLocation?.let { location ->
            travelPlaces.forEach { place ->
                place.distance = calculateDistance(
                    location.latitude, location.longitude,
                    place.latitude, place.longitude
                )
            }
            Log.d(TAG, "Calculated distances from current location")
        } ?: Log.d(TAG, "No current location available for distance calculation")
        
        googleMap.clear()
        Log.d(TAG, "Cleared existing markers from map")
        
        travelPlaces.forEach { place ->
            addMarkerToMap(place)
            Log.d(TAG, "Added marker for: ${place.name} at (${place.latitude}, ${place.longitude})")
        }
        
        Log.d(TAG, "Finished loading ${travelPlaces.size} sample places")
    }
    
    private fun addMarkerToMap(place: TravelPlace) {
        Log.d(TAG, "Creating marker for ${place.name} at (${place.latitude}, ${place.longitude})")
        
        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(place.latitude, place.longitude))
                .title(place.name)
                .snippet(place.category)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )
        marker?.tag = place
        
        Log.d(TAG, "Marker created successfully: ${marker != null}")
    }
    
    override fun onMarkerClick(marker: Marker): Boolean {
        val place = marker.tag as? TravelPlace
        place?.let {
            selectedPlace = it
            showPlaceCard(it)
        }
        return true
    }
    
    private fun showPlaceCard(place: TravelPlace) {
        // Since we're using RecyclerView now, we can scroll to the selected place
        // or highlight it in the list instead of showing a separate card
        val position = travelPlaces.indexOf(place)
        if (position != -1) {
            recyclerViewPlaces.scrollToPosition(position)
        }
    }
    
    private fun getCategoryFromKinds(kinds: String): String {
        return when {
            kinds.contains("museums") -> "Museum"
            kinds.contains("historic") -> "Historic"
            kinds.contains("natural") -> "Natural"
            kinds.contains("cultural") -> "Cultural"
            kinds.contains("architecture") -> "Architecture"
            kinds.contains("tourist_attractions") -> "Attraction"
            else -> "Place"
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    private fun openInGoogleMaps(latitude: Double, longitude: Double, placeName: String) {
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($placeName)")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback to web browser
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            startActivity(webIntent)
        }
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_map
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_map -> {
                    // Already on map activity
                    true
                }
                R.id.nav_itinerary -> {
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
}