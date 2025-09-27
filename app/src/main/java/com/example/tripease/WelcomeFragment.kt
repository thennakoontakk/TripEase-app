package com.example.tripease

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class WelcomeFragment : Fragment() {
    
    companion object {
        private const val ARG_POSITION = "position"
        
        fun newInstance(position: Int): WelcomeFragment {
            val fragment = WelcomeFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val position = arguments?.getInt(ARG_POSITION) ?: 0
        
        return when (position) {
            0 -> inflater.inflate(R.layout.welcome_screen_01, container, false)
            1 -> inflater.inflate(R.layout.welcome_screen_02, container, false)
            2 -> inflater.inflate(R.layout.welcome_screen_03, container, false)
            else -> inflater.inflate(R.layout.welcome_screen_01, container, false)
        }
    }
}