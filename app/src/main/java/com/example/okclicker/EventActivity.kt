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
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.okclicker.databinding.ActivityEventBinding
import com.example.okclicker.service.MyAccessibilityService
import kotlin.random.Random

class EventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using view binding
        binding = ActivityEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

         val scannerButton : AppCompatButton = binding.eventactivityScannner
         val executeButton : AppCompatButton = binding.eventactivitySavebutton
         val eventName = binding.eventactivityEventname.text?.toString()
         val higherLimitInMs = binding.eventactivityHigherlimitinms.text?.toString()
         val lowerLimitInMs = binding.eventactivityLowerlimitinms.text?.toString()
         val fixedDurationInMs = binding.eventactivityFixeddurationinms.text?.toString()





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
            // Fetch the latest values of the input fields
            val hourHigherLimitText = binding.eventactivityHourhigherlimit.text?.toString()
            val hourLowerLimitText = binding.eventactivityHourlowerlimit.text?.toString()
            val lowerLimitText = binding.eventactivityLowerlimitinms.text?.toString()
            val higherLimitText = binding.eventactivityHigherlimitinms.text?.toString()

            // Validate user input for hours
            val randomTime = validateAndGenerateRandomHour(hourLowerLimitText, hourHigherLimitText, this)
                ?: return@setOnClickListener

            Log.d("Hour", "$randomTime")
            val totalDurationInMs = randomTime * 60 * 60 * 1000L

            // Validate and parse the millisecond limits
            if (lowerLimitText.isNullOrBlank()) {
                Toast.makeText(this, "Lower limit cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val lowerLimitInMs = try {
                lowerLimitText.trim().toInt()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Invalid millisecond lower limit.", Toast.LENGTH_SHORT).show()
                Log.e("Error", "Lower limit parsing failed", e)
                return@setOnClickListener
            }

            if (higherLimitText.isNullOrBlank()) {
                Toast.makeText(this, "Higher limit cannot be empty.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val higherLimitInMs = try {
                higherLimitText.trim().toInt()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Invalid millisecond higher limit.", Toast.LENGTH_SHORT).show()
                Log.e("Error", "Higher limit parsing failed", e)
                return@setOnClickListener
            }

            if (lowerLimitInMs >= higherLimitInMs) {
                Toast.makeText(this, "Lower limit must be less than upper limit.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
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
                            service.startRandomClicking(savedCoordinates.first, savedCoordinates.second, totalDurationInMs, lowerLimitInMs, higherLimitInMs)
                        } else {
                            Toast.makeText(this, "No saved coordinates found.", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        Toast.makeText(this, "Accessibility service is not initialized.", Toast.LENGTH_SHORT).show()
                        Log.d("Execute click", "Accessibility service is not initialized.")
                    }
                }, 0)

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

    fun getRandomHour(hourLowerLimit: Int, hourHigherLimit: Int): Int {
        // Ensure the limits are within 1 to 12
        require(hourLowerLimit in 1..12) { "hourLowerLimit must be between 1 and 12." }
        require(hourHigherLimit in 1..12) { "hourHigherLimit must be between 1 and 12." }

        // Enforce that the lower limit is less than or equal to the upper limit
        require(hourLowerLimit < hourHigherLimit) {
            "hourLowerLimit must be less than or equal to hourHigherLimit."
        }

        // Generate a random number within the valid range
        return Random.nextInt(hourLowerLimit, hourHigherLimit + 1)
    }

    private fun validateAndGenerateRandomHour(
        hourLowerLimitText: String?,
        hourHigherLimitText: String?,
        context: Context
    ): Int? {
        if (hourLowerLimitText.isNullOrEmpty() || hourHigherLimitText.isNullOrEmpty()) {
            Toast.makeText(context, "Please enter both lower and upper limits.", Toast.LENGTH_SHORT).show()
            return null
        }

        val hourLowerLimit: Int
        val hourHigherLimit: Int
        try {
            hourLowerLimit = hourLowerLimitText.toInt()
            hourHigherLimit = hourHigherLimitText.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Invalid number format. Please enter valid integers.", Toast.LENGTH_SHORT).show()
            return null
        }

        // Validate the range
        if (hourLowerLimit !in 1..12 || hourHigherLimit !in 1..12) {
            Toast.makeText(context, "Limits must be between 1 and 12.", Toast.LENGTH_SHORT).show()
            return null
        }

        if (hourLowerLimit >= hourHigherLimit) {
            Toast.makeText(context, "Lower limit must be less than upper limit.", Toast.LENGTH_SHORT).show()
            return null
        }

        // Generate and return a random hour within the range
        return getRandomHour(hourLowerLimit, hourHigherLimit)
    }


    fun subtractFrom24(randomTime: Int): Int {
        return 24 - randomTime
    }








}
