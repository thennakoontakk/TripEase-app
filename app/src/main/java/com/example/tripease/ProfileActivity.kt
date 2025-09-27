package com.example.tripease

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.imageview.ShapeableImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    // UI Components
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var ivProfileImage: ShapeableImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvMembershipStatus: TextView
    private lateinit var switchReminders: SwitchMaterial
    private lateinit var switchNotifications: SwitchMaterial
    
    // Card Views
    private lateinit var cardEditProfile: MaterialCardView
    private lateinit var cardReminders: MaterialCardView
    private lateinit var cardOfflineData: MaterialCardView
    private lateinit var cardNotifications: MaterialCardView
    private lateinit var cardPrivacyPolicy: MaterialCardView
    private lateinit var cardHelpSupport: MaterialCardView
    private lateinit var cardLogout: MaterialCardView

    companion object {
        private const val TAG = "ProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        initializeViews()
        
        // Load user data
        loadUserData()
        
        // Setup click listeners
        setupClickListeners()
        
        // Setup bottom navigation
        setupBottomNavigation()
    }

    private fun initializeViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvMembershipStatus = findViewById(R.id.tvMembershipStatus)
        switchReminders = findViewById(R.id.switchReminders)
        switchNotifications = findViewById(R.id.switchNotifications)
        
        cardEditProfile = findViewById(R.id.cardEditProfile)
        cardReminders = findViewById(R.id.cardReminders)
        cardOfflineData = findViewById(R.id.cardOfflineData)
        cardNotifications = findViewById(R.id.cardNotifications)
        cardPrivacyPolicy = findViewById(R.id.cardPrivacyPolicy)
        cardHelpSupport = findViewById(R.id.cardHelpSupport)
        cardLogout = findViewById(R.id.cardLogout)
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Set basic user info from Firebase Auth
            tvUserEmail.text = currentUser.email
            
            // Load additional user data from Firestore
            firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fullName = document.getString("fullName") ?: "User"
                        val accountStatus = document.getString("accountStatus") ?: "active"
                        
                        tvUserName.text = fullName
                        
                        // Set membership status based on account status
                        when (accountStatus) {
                            "premium" -> tvMembershipStatus.text = "Premium Member"
                            "active" -> tvMembershipStatus.text = "Free Member"
                            else -> tvMembershipStatus.text = "Member"
                        }
                        
                        Log.d(TAG, "User data loaded successfully")
                    } else {
                        // Use default values if no Firestore document
                        tvUserName.text = currentUser.displayName ?: "User"
                        tvMembershipStatus.text = "Free Member"
                        Log.w(TAG, "No user document found in Firestore")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error loading user data", exception)
                    // Use default values on error
                    tvUserName.text = currentUser.displayName ?: "User"
                    tvMembershipStatus.text = "Free Member"
                }
        }
    }

    private fun setupClickListeners() {
        cardEditProfile.setOnClickListener {
            // TODO: Navigate to Edit Profile activity
            Toast.makeText(this, "Edit Profile - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        cardOfflineData.setOnClickListener {
            // TODO: Navigate to Offline Data management
            Toast.makeText(this, "Offline Data - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        cardPrivacyPolicy.setOnClickListener {
            // TODO: Navigate to Privacy Policy
            Toast.makeText(this, "Privacy Policy - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        cardHelpSupport.setOnClickListener {
            // TODO: Navigate to Help & Support
            Toast.makeText(this, "Help & Support - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        cardLogout.setOnClickListener {
            showLogoutDialog()
        }

        // Switch listeners
        switchReminders.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Save reminder preference
            Toast.makeText(this, "Reminders ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Save notification preference
            Toast.makeText(this, "Notifications ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.nav_profile
        
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_map -> {
                    startActivity(Intent(this, MapActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_itinerary -> {
                    // TODO: Navigate to Itinerary activity
                    Toast.makeText(this, "Itinerary - Coming Soon", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_profile -> {
                    // Already on profile page
                    true
                }
                else -> false
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        try {
            auth.signOut()
            
            // Clear any cached data or preferences here if needed
            
            // Navigate to login screen
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "User logged out successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
            Toast.makeText(this, "Error logging out. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
}