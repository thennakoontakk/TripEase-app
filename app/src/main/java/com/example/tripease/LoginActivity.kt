package com.example.tripease

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoogleSignIn: MaterialButton
    
    companion object {
        private const val TAG = "LoginActivity"
        private const val USERS_COLLECTION = "users"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        // Initialize Firebase Auth and Firestore
        auth = Firebase.auth
        firestore = Firebase.firestore
        
        // Initialize views
        initViews()
        
        // Set click listeners
        setClickListeners()
    }
    
    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
    }
    
    private fun setClickListeners() {
        btnLogin.setOnClickListener {
            loginUser()
        }
        
        btnGoogleSignIn.setOnClickListener {
            // TODO: Implement Google Sign-In
            Toast.makeText(this, "Google Sign-In coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<android.widget.TextView>(R.id.tvSignUp).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        
        findViewById<android.widget.TextView>(R.id.tvForgotPassword).setOnClickListener {
            // TODO: Implement forgot password
            Toast.makeText(this, "Forgot password coming soon!", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return
        }
        
        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            return
        }
        
        // Show loading state
        btnLogin.text = "Logging in..."
        btnLogin.isEnabled = false
        
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, retrieve user data from Firestore
                    val user = auth.currentUser
                    user?.let { firebaseUser ->
                        retrieveUserFromFirestore(firebaseUser.uid)
                    }
                } else {
                    // Sign in failed
                    Log.e(TAG, "Login failed", task.exception)
                    resetButtonState()
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
    
    private fun retrieveUserFromFirestore(uid: String) {
        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                resetButtonState()
                
                if (document.exists()) {
                    try {
                        val user = document.toObject(User::class.java)
                        Log.d(TAG, "User data retrieved: ${user?.fullName}")
                        
                        // Check account status
                        if (user?.accountStatus == "suspended") {
                            Toast.makeText(this, "Your account has been suspended. Please contact support.", Toast.LENGTH_LONG).show()
                            auth.signOut()
                            return@addOnSuccessListener
                        }
                        
                        if (user?.accountStatus == "deleted") {
                            Toast.makeText(this, "This account no longer exists.", Toast.LENGTH_LONG).show()
                            auth.signOut()
                            return@addOnSuccessListener
                        }
                        
                        // Update last login timestamp
                        updateLastLogin(uid)
                        
                        Toast.makeText(this, "Welcome back, ${user?.fullName ?: "User"}!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user data", e)
                        Toast.makeText(this, "Error loading user data", Toast.LENGTH_SHORT).show()
                        // Still allow login even if Firestore data is corrupted
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                } else {
                    Log.w(TAG, "User document does not exist in Firestore")
                    // User exists in Auth but not in Firestore (maybe old user)
                    // Create a basic user document
                    createMissingUserDocument(uid)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error retrieving user from Firestore", exception)
                resetButtonState()
                Toast.makeText(this, "Error loading user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                
                // Still allow login even if Firestore fails
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
    }
    
    private fun updateLastLogin(uid: String) {
        val updates = mapOf(
            "updatedAt" to com.google.firebase.Timestamp.now(),
            "lastLoginAt" to com.google.firebase.Timestamp.now()
        )
        
        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Last login timestamp updated")
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Failed to update last login", exception)
            }
    }
    
    private fun createMissingUserDocument(uid: String) {
        val currentUser = auth.currentUser
        val user = User(
            uid = uid,
            fullName = currentUser?.displayName ?: "User",
            email = currentUser?.email ?: "",
            isEmailVerified = currentUser?.isEmailVerified ?: false,
            accountStatus = "active"
        )
        
        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .set(user.toMap())
            .addOnSuccessListener {
                Log.d(TAG, "Missing user document created")
                Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to create missing user document", exception)
                // Still allow login
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
    }
    
    private fun resetButtonState() {
        btnLogin.text = "Login"
        btnLogin.isEnabled = true
    }
}