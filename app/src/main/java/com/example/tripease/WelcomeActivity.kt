package com.example.tripease

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class WelcomeActivity : AppCompatActivity() {
    
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNext: Button
    private lateinit var btnSkip: TextView
    private lateinit var welcomeAdapter: WelcomeAdapter
    private lateinit var sharedPreferences: SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        
        // Check if onboarding was already completed
        sharedPreferences = getSharedPreferences("TripEasePrefs", MODE_PRIVATE)
        if (sharedPreferences.getBoolean("onboarding_completed", false)) {
            navigateToLogin()
            return
        }
        
        initViews()
        setupViewPager()
        setupClickListeners()
    }
    
    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)
    }
    
    private fun setupViewPager() {
        welcomeAdapter = WelcomeAdapter(this)
        viewPager.adapter = welcomeAdapter
        
        // Connect TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()
        
        // Listen for page changes
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateButtonText(position)
            }
        })
    }
    
    private fun setupClickListeners() {
        btnNext.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem < welcomeAdapter.itemCount - 1) {
                viewPager.currentItem = currentItem + 1
            } else {
                completeOnboarding()
            }
        }
        
        btnSkip.setOnClickListener {
            completeOnboarding()
        }
    }
    
    private fun updateButtonText(position: Int) {
        if (position == welcomeAdapter.itemCount - 1) {
            btnNext.text = "Start Exploring"
        } else {
            btnNext.text = "Next"
        }
    }
    
    private fun completeOnboarding() {
        // Mark onboarding as completed
        sharedPreferences.edit()
            .putBoolean("onboarding_completed", true)
            .apply()
        
        navigateToLogin()
    }
    
    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}