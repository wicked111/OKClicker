package com.example.okclicker

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.okclicker.databinding.ActivityEventBinding
import com.example.okclicker.service.MyAccessibilityService

class EventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using view binding
        binding = ActivityEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

         val scannerButton : AppCompatButton = binding.eventactivityScannner
         val executeButton : AppCompatButton = binding.eventactivitySavebutton


        // Adjust the insets for system bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        basicActivityVisibility()
        scannerButton.setOnClickListener {
            if (!isAccessibilityServiceEnabled(this, MyAccessibilityService::class.java)) {
                // If service is not enabled, direct the user to the accessibility settings
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "Please enable the accessibility service.", Toast.LENGTH_LONG).show()
            } else {
                // If service is enabled, proceed with the scan action
                proceedToHomeScreen()
                MyAccessibilityService.instance?.setupTouchOverlay()
            }
        }


        executeButton.setOnClickListener {
            // Open home screen again
            proceedToHomeScreen()

            // Initialize and display the floating window
            val executeFloatingWindow = MyAccessibilityService.instance?.setupExecuteOverlay()

            // Find buttons from the floating window
            val playButton = executeFloatingWindow?.findViewById<ImageButton>(R.id.play_button)
            val cancelButton = executeFloatingWindow?.findViewById<ImageButton>(R.id.cancel_button)

            playButton?.setOnClickListener {
                Handler(Looper.getMainLooper()).postDelayed({
                    MyAccessibilityService.instance?.let { service ->
                        val savedCoordinates = service.getSavedCoordinates()
                        Log.d("Execute click", "${savedCoordinates?.first} ${savedCoordinates?.second}")
                        if (savedCoordinates != null) {
                            // Start continuous clicking with a specified interval (e.g., 2000ms)
                            service.startContinuousClicking(savedCoordinates.first, savedCoordinates.second, 2000L)
                        } else {
                            Toast.makeText(this, "No saved coordinates found.", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        Toast.makeText(this, "Accessibility service is not initialized.", Toast.LENGTH_SHORT).show()
                        Log.d("Execute click", "Accessibility service is not initialized.")
                    }
                }, 2000)

                // Hide the play button after it's clicked
                playButton.visibility = View.GONE
            }

            cancelButton?.setOnClickListener {
                MyAccessibilityService.instance?.stopContinuousClicking()

                // Remove the floating window
                val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                windowManager.removeView(executeFloatingWindow)

                // Optionally go back to the main activity
                startActivity(Intent(this, EventActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the overlay when the activity is destroyed

    }


    private fun proceedToHomeScreen() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        this.startActivity(intent)
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out AccessibilityService>): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        if (accessibilityEnabled == 1) {
            val services = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            services?.split(":")?.forEach {
                if (it.equals("${context.packageName}/${service.name}", ignoreCase = true)) {
                    return true
                }
            }
        }
        return false
    }


    private fun basicActivityVisibility() {

        // Set up the scanner launcher on button click
        binding.eventactivityScannner.setOnClickListener {

        }
        // Set initial visibility for the fixed time duration input layout the layout
        binding.eventactivityFixedlayout.visibility = View.VISIBLE

        // Handle button clicks to show and hide the appropriate layouts
        binding.eventactivityFixedbutton.setOnClickListener {
            binding.eventactivityFixedbutton.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.Yellow))
            binding.eventactiviyRandombutton.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))

            binding.eventactivityFixedlayout.visibility = View.VISIBLE
            binding.eventactivityRandomlayout.visibility = View.GONE
        }

        binding.eventactiviyRandombutton.setOnClickListener {
            binding.eventactiviyRandombutton.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.Yellow))
            binding.eventactivityFixedbutton.backgroundTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))

            binding.eventactivityFixedlayout.visibility = View.GONE
            binding.eventactivityRandomlayout.visibility = View.VISIBLE
        }
    }
}
