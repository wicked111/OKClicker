package com.example.okclicker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.PixelFormat
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.LayoutInflater
import android.widget.ImageButton
import com.example.okclicker.EventActivity
import com.example.okclicker.R
import kotlin.random.Random

class MyAccessibilityService : AccessibilityService() {

    private var clickCoordinates: Pair<Int, Int>? = null
    private var overlayView: View? = null
    private var floatingWindow: View? = null
    private val clickHandler = Handler(Looper.getMainLooper())
    private var clickRunnable: Runnable? = null

    companion object {
        var instance: MyAccessibilityService? = null
            private set
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("DESTROY", "Service destroyed")
        removeFloatingWindow()
        instance = null
    }

    override fun onServiceConnected() {

        super.onServiceConnected()
        instance = this
        startActivity(Intent(this, EventActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        Log.d("MyAccessibilityService", "Service connected")
    }

    fun setupTouchOverlay() {
        overlayView = View(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT) // or TRANSPARENT depending on your use case
            setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    clickCoordinates = Pair(event.x.toInt(), event.y.toInt())
                    // saveCoordinates(clickCoordinates!!)
                    Log.d("MyAccessibilityService", "Coordinates saved: $clickCoordinates")
                    Toast.makeText(this@MyAccessibilityService, "Coordinates clicked on: $clickCoordinates", Toast.LENGTH_SHORT).show()

                    // Call performClick() to ensure accessibility services are triggered
                    view.performClick()


                }
                true
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(overlayView, params)

        floatingWindow = (getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.scan_overlay, null)

        val floatingParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = android.view.Gravity.TOP and android.view.Gravity.END
            x = 10
            y = 10
        }

        windowManager.addView(floatingWindow, floatingParams)

        // Handle Button Clicks
        val saveButton = floatingWindow?.findViewById<ImageButton>(R.id.save_button)
        val discardButton = floatingWindow?.findViewById<ImageButton>(R.id.discard_button)
        val dragButton = floatingWindow?.findViewById<ImageButton>(R.id.dragging_button)

        saveButton?.setOnClickListener {
            // Handle save action
            saveCoordinates(clickCoordinates!!)
            Toast.makeText(this, "Coordinates saved $clickCoordinates", Toast.LENGTH_SHORT).show()
            returnToApp() // Save and return to app
        }

        discardButton?.setOnClickListener {
            // Handle discard action
            Toast.makeText(this, "Scan discarded", Toast.LENGTH_SHORT).show()
            removeFloatingWindow()
            removeOverlay()
            returnToApp()
        }

        dragButton?.apply {
            setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var touchX = 0f
                private var touchY = 0f

                override fun onTouch(view: View?, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = floatingParams.x
                            initialY = floatingParams.y
                            touchX = event.rawX
                            touchY = event.rawY
                            // Call performClick to handle accessibility and click behavior
                            view?.performClick()
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            floatingParams.x = initialX + (event.rawX - touchX).toInt()
                            floatingParams.y = initialY + (event.rawY - touchY).toInt()
                            windowManager.updateViewLayout(floatingWindow, floatingParams)
                            return true
                        }
                    }
                    return false
                }
            })
        }
    }


    private fun saveCoordinates(coordinates: Pair<Int, Int>) {
        val sharedPref = getSharedPreferences("AutoclickerPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("click_x", coordinates.first)
            putInt("click_y", coordinates.second)
            Log.d("savecoordinates", "${coordinates.first} ${coordinates.second}")
            apply()
        }
    }

    fun getSavedCoordinates(): Pair<Int, Int>? {
        val sharedPref = getSharedPreferences("AutoclickerPrefs", Context.MODE_PRIVATE)
        val x = sharedPref.getInt("click_x", -1)
        val y = sharedPref.getInt("click_y", -1)
        Log.d("getsavedcoordinates", "$x $y")
        return if (x != -1 && y != -1) Pair(x, y) else null
    }

    fun performClick(x: Int, y: Int) {
        Log.d("performClick", "Attempting to click at coordinates: $x, $y")

        val path = Path().apply {
            moveTo(x.toFloat(), y.toFloat())
        }

        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 100, 100)) // Adjusted delay and duration
            .build()

        Log.d("performClick", "Gesture description created, dispatching gesture...")

        dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.d("performClick", "Gesture completed successfully at ($x, $y)")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.d("performClick", "Gesture cancelled")
            }
        }, null)

        Log.d("performClick", "Gesture dispatched")
    }

   /* fun startContinuousClicking(x: Int, y: Int, interval: Long) {
        // Initialize the handler and runnable for continuous clicking
        clickHandler = Handler(Looper.getMainLooper())
        clickRunnable = object : Runnable {
            override fun run() {
                performClick(x, y)
                clickHandler?.postDelayed(this, interval) // Schedule the next click after the specified interval
            }
        }
        clickHandler?.post(clickRunnable!!) // Start the first click immediately
    }*/

    fun stopContinuousClicking() {
        clickHandler.removeCallbacksAndMessages(null)
        clickRunnable = null
        Log.d("StopClicking", "Continuous clicking has been stopped.")
    }


    private fun returnToApp() {
        val returnIntent = Intent(this, EventActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(returnIntent)
        removeOverlay()
        removeFloatingWindow()
    }

    private fun removeOverlay() {
        overlayView?.let {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.removeView(it)
            overlayView = null
            Log.d("Overlay", "Overlay removed")
        }
    }

    private fun removeFloatingWindow() {
        if (floatingWindow != null) {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.removeView(floatingWindow)
            floatingWindow = null // Nullify the reference
        }
    }

    fun setupExecuteOverlay(): View {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val executeFloatingWindow = inflater.inflate(R.layout.execute_overlay, null)

        val floatingParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.END
            x = 10 // Optional padding from the right edge
            y = 10 // Optional padding from the top edge
        }

        // Add the floating window to the WindowManager
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(executeFloatingWindow, floatingParams)

        return executeFloatingWindow
    }

    override fun onInterrupt() {
        // Handle interruptions, e.g., when another service takes control
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for this service, but must be overridden
    }

    fun startRandomClicking(x: Int, y: Int, totalDurationMs: Long, lowerDelay: Int, higherDelay: Int) {

        var elapsedTime = 0L

        // Start the clicking process
         clickRunnable = object : Runnable {
            override fun run() {
                if (elapsedTime < totalDurationMs) {
                    // Generate a random delay once
                    val delay = randomClickDelaySelector(higherDelay, lowerDelay)


                    // Perform the click
                    performClickWithProximity(x, y,20)

                    // Increment the elapsed time using the same delay
                    elapsedTime += delay.toLong()

                    // Schedule the next click using the same delay
                    clickHandler.postDelayed(this, delay.toLong())
                } else {
                    Log.d("Clicking", "Finished clicking for $totalDurationMs ms")
                }
            }
        }

        // Start the first click
        clickHandler.post(clickRunnable!!)
    }
    fun randomClickDelaySelector(higherLimitInMs: Int, lowerLimitInMs: Int): Int {
        require(higherLimitInMs > lowerLimitInMs) {"higherLimitInMs must be greater than lowerLimitInMs"}
        return Random.nextInt(lowerLimitInMs, higherLimitInMs + 1)
    }

    fun performClickWithProximity(x: Int, y: Int, radius: Int) {
        // Generate random offset within the circle
        val offset = generateRandomOffsetWithinCircle(radius)
        val newX = x + offset.first
        val newY = y + offset.second

        Log.d("performClickWithProximity", "Clicking at coordinates: $newX, $newY")

        val path = Path().apply {
            moveTo(newX.toFloat(), newY.toFloat())
        }

        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 100, 100)) // Adjusted delay and duration
            .build()

        dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.d("performClickWithProximity", "Gesture completed successfully at ($newX, $newY)")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.d("performClickWithProximity", "Gesture cancelled")
            }
        }, null)
    }

    // Helper function to generate random offsets within a circle
    private fun generateRandomOffsetWithinCircle(radius: Int): Pair<Int, Int> {
        while (true) {
            val dx = Random.nextInt(-radius, radius + 1)
            val dy = Random.nextInt(-radius, radius + 1)
            if (dx * dx + dy * dy <= radius * radius) {
                return Pair(dx, dy)
            }
        }
    }


}