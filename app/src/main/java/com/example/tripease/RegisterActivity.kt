package com.example.tripease

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnSignUp: MaterialButton
    private lateinit var btnGoogleSignIn: MaterialButton
    
    companion object {
        private const val TAG = "RegisterActivity"
        private const val USERS_COLLECTION = "users"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        // Initialize Firebase Auth and Firestore
        auth = Firebase.auth
        firestore = Firebase.firestore
        
        // Initialize views
        initViews()
        
        // Set click listeners
        setClickListeners()
    }
    
    private fun initViews() {
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
    }
    
    private fun setClickListeners() {
        btnSignUp.setOnClickListener {
            registerUser()
        }
        
        btnGoogleSignIn.setOnClickListener {
            // TODO: Implement Google Sign-In
            Toast.makeText(this, "Google Sign-In coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<android.widget.TextView>(R.id.tvLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
    
    private fun registerUser() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        
        if (fullName.isEmpty()) {
            etFullName.error = "Full name is required"
            return
        }
        
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return
        }
        
        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            return
        }
        
        if (password.length < 6) {
            etPassword.error = "Password must be at least 6 characters"
            return
        }
        
        // Show loading state
        btnSignUp.text = "Creating Account..."
        btnSignUp.isEnabled = false
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registration success, update user profile and save to Firestore
                    val user = auth.currentUser
                    user?.let { firebaseUser ->
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()
                        
                        firebaseUser.updateProfile(profileUpdates)
                            .addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    // Save user data to Firestore
                                    saveUserToFirestore(firebaseUser.uid, fullName, email)
                                } else {
                                    Log.e(TAG, "Failed to update profile", profileTask.exception)
                                    resetButtonState()
                                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    // Registration failed
                    Log.e(TAG, "Registration failed", task.exception)
                    resetButtonState()
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
    
    private fun saveUserToFirestore(uid: String, fullName: String, email: String) {
        val user = User(
            uid = uid,
            fullName = fullName,
            email = email,
            isEmailVerified = auth.currentUser?.isEmailVerified ?: false,
            accountStatus = "active"
        )
        
        firestore.collection(USERS_COLLECTION)
            .document(uid)
            .set(user.toMap())
            .addOnSuccessListener {
                Log.d(TAG, "User successfully saved to Firestore")
                resetButtonState()
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                
                // Navigate to MainActivity
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error saving user to Firestore", exception)
                resetButtonState()
                Toast.makeText(this, "Failed to save user data: ${exception.message}", Toast.LENGTH_LONG).show()
                
                // Even if Firestore fails, the user is still created in Auth
                // So we can still navigate to MainActivity
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
    }
    
    private fun resetButtonState() {
        btnSignUp.text = "Sign Up"
        btnSignUp.isEnabled = true
    }
}