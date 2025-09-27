package com.example.tripease

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.viewpager2.widget.ViewPager2

class AutoSlidingCarousel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val viewPager: ViewPager2
    private val handler = Handler(Looper.getMainLooper())
    private var autoSlideRunnable: Runnable? = null
    private var isAutoSliding = false
    private var autoSlideDelay = 3000L // 3 seconds
    private var userInteracting = false

    init {
        // Create and add ViewPager2
        viewPager = ViewPager2(context)
        addView(viewPager, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        
        // Set up page transformer for smooth transitions
        viewPager.setPageTransformer { page, position ->
            when {
                position < -1 -> {
                    page.alpha = 0f
                }
                position <= 1 -> {
                    page.alpha = 1f
                    page.scaleX = 0.85f + (1 - kotlin.math.abs(position)) * 0.15f
                    page.scaleY = 0.85f + (1 - kotlin.math.abs(position)) * 0.15f
                }
                else -> {
                    page.alpha = 0f
                }
            }
        }

        // Register page change callback to reset auto-slide timer
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (isAutoSliding && !userInteracting) {
                    resetAutoSlideTimer()
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> {
                        userInteracting = true
                        stopAutoSlide()
                    }
                    ViewPager2.SCROLL_STATE_IDLE -> {
                        userInteracting = false
                        if (isAutoSliding) {
                            startAutoSlide()
                        }
                    }
                }
            }
        })
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> {
                userInteracting = true
                stopAutoSlide()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                userInteracting = false
                if (isAutoSliding) {
                    handler.postDelayed({
                        if (!userInteracting && isAutoSliding) {
                            startAutoSlide()
                        }
                    }, 1000) // Wait 1 second before resuming auto-slide
                }
            }
        }
        return super.onTouchEvent(ev)
    }

    fun startAutoSlide() {
        if (!isAutoSliding || userInteracting) return
        
        stopAutoSlide()
        autoSlideRunnable = Runnable {
            if (viewPager.adapter != null && viewPager.adapter!!.itemCount > 1 && !userInteracting) {
                val nextItem = (viewPager.currentItem + 1) % viewPager.adapter!!.itemCount
                viewPager.setCurrentItem(nextItem, true)
                startAutoSlide() // Schedule next slide
            }
        }
        handler.postDelayed(autoSlideRunnable!!, autoSlideDelay)
    }

    fun stopAutoSlide() {
        autoSlideRunnable?.let {
            handler.removeCallbacks(it)
            autoSlideRunnable = null
        }
    }

    private fun resetAutoSlideTimer() {
        stopAutoSlide()
        startAutoSlide()
    }

    fun enableAutoSlide(enable: Boolean) {
        isAutoSliding = enable
        if (enable) {
            startAutoSlide()
        } else {
            stopAutoSlide()
        }
    }

    fun setAutoSlideDelay(delayMs: Long) {
        autoSlideDelay = delayMs
        if (isAutoSliding) {
            resetAutoSlideTimer()
        }
    }

    // Delegate ViewPager2 methods
    fun setAdapter(adapter: androidx.recyclerview.widget.RecyclerView.Adapter<*>?) {
        viewPager.adapter = adapter
    }

    fun getCurrentItem(): Int = viewPager.currentItem

    fun setCurrentItem(item: Int, smoothScroll: Boolean = true) {
        viewPager.setCurrentItem(item, smoothScroll)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isAutoSliding) {
            startAutoSlide()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAutoSlide()
    }
}