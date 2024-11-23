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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.random.Random
import com.example.okclicker.HomeActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using view binding
        binding = ActivityEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val scannerButton: AppCompatButton = binding.eventactivityScannner
        val executeButton: AppCompatButton = binding.eventactivitySavebutton
        val draftButton: AppCompatButton = binding.eventactivityDraftbutton
        val eventName = binding.eventactivityEventname.text?.toString()
        val higherLimitInMs = binding.eventactivityHigherlimitinms.text?.toString()
        val lowerLimitInMs = binding.eventactivityLowerlimitinms.text?.toString()


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
                Toast.makeText(this, "Please enable the accessibility service.", Toast.LENGTH_LONG)
                    .show()
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
            val fixedDurationInMs = binding.eventactivityFixeddurationinms.text?.toString()
            val savedCoordinates = MyAccessibilityService.instance?.getSavedCoordinates()

            // Validate user input for hours
            val randomTime =
                validateAndGenerateRandomHour(hourLowerLimitText, hourHigherLimitText, this)
                    ?: return@setOnClickListener

            Log.d("Hour", "$randomTime")
            val totalDurationInMs = randomTime * 60 * 60 * 1000L

            // Validate and parse the millisecond limits
            if (lowerLimitText.isNullOrBlank() || higherLimitText.isNullOrBlank()) {
                Toast.makeText(this, "Enter the random range correctly in ms.", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            /*  if (fixedDurationInMs.isNullOrBlank()) {
                Toast.makeText(this, "Enter the fixed duration in ms.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }*/


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
                Toast.makeText(
                    this,
                    "Lower limit must be less than upper limit.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (savedCoordinates == null) {
                Toast.makeText(this, "No saved coordinates found.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

            val newSession = clickItem(
                eventName = binding.eventactivityEventname.text.toString(),
                eventDate = currentDate, // Convert timestamp to formatted date if needed
                hourLowerLimit = hourLowerLimitText!!.toInt(),
                hourUpperLimit = hourHigherLimitText!!.toInt(),
                delayLowerLimitMs = lowerLimitInMs,
                delayUpperLimitMs = higherLimitInMs,
                totalDurationInMs = totalDurationInMs,
                coordinates = Pair(savedCoordinates!!.first, savedCoordinates.second),
                timestamp = System.currentTimeMillis()
                //fixedLimitInMs = fixedDurationInMs!!.toInt(),
            )

            // Save the session to shared preferences
            // Load existing sessions
            val currentSessions =
                PreferencesHelper.loadSessionsFromPreferences(this).toMutableList()

            // Add the new session
            currentSessions.add(newSession)

            // Save updated list back to preferences
            PreferencesHelper.saveSessionsToPreferences(this, currentSessions)

            Toast.makeText(this, "Session saved!", Toast.LENGTH_SHORT).show()


            // Open home screen again
            proceedToHomeScreen()

            // Initialize and display the floating window
            val executeFloatingWindow = MyAccessibilityService.instance?.setupExecuteOverlay()

            // Find buttons from the floating window
            val playButton = executeFloatingWindow?.findViewById<ImageButton>(R.id.play_button)
            val cancelButton = executeFloatingWindow?.findViewById<ImageButton>(R.id.cancel_button)

            playButton?.setOnClickListener {
                startClickingCycle(lowerLimitInMs, higherLimitInMs, totalDurationInMs)

                // Hide the play button after it's clicked
                playButton.visibility = View.GONE
            }

            cancelButton?.setOnClickListener {
                MyAccessibilityService.instance?.stopContinuousClicking()

                // Remove the floating window
                val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                windowManager.removeView(executeFloatingWindow)

                // Optionally go back to the main activity
                startActivity(Intent(this, HomeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }

        draftButton.setOnClickListener {

            val hourHigherLimitText = binding.eventactivityHourhigherlimit.text?.toString()
            val hourLowerLimitText = binding.eventactivityHourlowerlimit.text?.toString()
            val lowerLimitText = binding.eventactivityLowerlimitinms.text?.toString()
            val higherLimitText = binding.eventactivityHigherlimitinms.text?.toString()
            val fixedDurationInMs = binding.eventactivityFixeddurationinms.text?.toString()

            // Validate user input for hours
            val randomTime =
                validateAndGenerateRandomHour(hourLowerLimitText, hourHigherLimitText, this)
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
                Toast.makeText(
                    this,
                    "Lower limit must be less than upper limit.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val savedCoordinates = MyAccessibilityService.instance?.getSavedCoordinates()
            val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

            val newSession = clickItem(
                eventName = binding.eventactivityEventname.text.toString(),
                eventDate = currentDate, // Convert timestamp to formatted date if needed
                hourLowerLimit = hourLowerLimitText!!.toInt(),
                hourUpperLimit = hourHigherLimitText!!.toInt(),
                delayLowerLimitMs = lowerLimitInMs,
                delayUpperLimitMs = higherLimitInMs,
                totalDurationInMs = totalDurationInMs,
                coordinates = Pair(savedCoordinates!!.first, savedCoordinates.second),
                timestamp = System.currentTimeMillis()
                // fixedLimitInMs = fixedDurationInMs!!.toInt(),
            )

            // Save the session to shared preferences
            // Load existing sessions
            val currentSessions =
                PreferencesHelper.loadSessionsFromPreferences(this).toMutableList()

            // Add the new session
            currentSessions.add(newSession)

            // Save updated list back to preferences
            PreferencesHelper.saveSessionsToPreferences(this, currentSessions)

            Toast.makeText(this, "Session saved!", Toast.LENGTH_SHORT).show()

            startActivity(Intent(this, HomeActivity::class.java))

        }


    }


    private fun proceedToHomeScreen() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        this.startActivity(intent)
    }

    private fun isAccessibilityServiceEnabled(
        context: Context,
        service: Class<out AccessibilityService>
    ): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )
        if (accessibilityEnabled == 1) {
            val services = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
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
            Toast.makeText(context, "Please enter both lower and upper limits.", Toast.LENGTH_SHORT)
                .show()
            return null
        }

        val hourLowerLimit: Int
        val hourHigherLimit: Int
        try {
            hourLowerLimit = hourLowerLimitText.toInt()
            hourHigherLimit = hourHigherLimitText.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(
                context,
                "Invalid number format. Please enter valid integers.",
                Toast.LENGTH_SHORT
            ).show()
            return null
        }

        // Validate the range
        if (hourLowerLimit !in 1..12 || hourHigherLimit !in 1..12) {
            Toast.makeText(context, "Limits must be between 1 and 12.", Toast.LENGTH_SHORT).show()
            return null
        }

        if (hourLowerLimit >= hourHigherLimit) {
            Toast.makeText(
                context,
                "Lower limit must be less than upper limit.",
                Toast.LENGTH_SHORT
            ).show()
            return null
        }

        // Generate and return a random hour within the range
        return getRandomHour(hourLowerLimit, hourHigherLimit)
    }

/*
    fun subtractFrom24(randomTime: Int): Int {
        return 24 - randomTime
    }*/

    fun startClickingCycle(
       // hourLowerLimit: Int,
      //  hourUpperLimit: Int,
        lowerLimitInMs: Int,
        higherLimitInMs: Int,
        totalDurationInMs: Long
    ) {

        // Start the clicking process
           MyAccessibilityService.instance?.let { service ->
            val savedCoordinates = service.getSavedCoordinates()
            if (savedCoordinates != null) {
                service.startRandomClicking(
                    savedCoordinates.first,
                    savedCoordinates.second,
                    totalDurationInMs,
                    lowerLimitInMs,
                    higherLimitInMs
                )

                // Schedule the idle phase after the clicking process completes
                Handler(Looper.getMainLooper()).postDelayed({
                    val idleDurationMs = 24 * 60 * 60 * 1000L - totalDurationInMs
                    Log.d("Idle Phase", "Halting for $idleDurationMs ms (${idleDurationMs / (60 * 60 * 1000)} hours)")

                    // Start the next cycle after the idle duration
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d("Next Cycle", "Starting next cycle")
                        startClickingCycle(lowerLimitInMs, higherLimitInMs,totalDurationInMs)
                    }, idleDurationMs)
                }, totalDurationInMs)
            } else {
                Toast.makeText(service, "No saved coordinates found.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Log.e("Error", "Accessibility service is not initialized.")
        }
    }



}
