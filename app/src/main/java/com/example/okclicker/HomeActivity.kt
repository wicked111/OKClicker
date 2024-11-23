package com.example.okclicker

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.okclicker.adapters.ClickSessionAdapter
import com.example.okclicker.databinding.ActivityHomeBinding
import com.example.okclicker.service.MyAccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var clickSessionAdapter: ClickSessionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Load sessions from shared preferences
        val sessions = PreferencesHelper.loadSessionsFromPreferences(this).toMutableList() // Mutable list

        if (sessions.isEmpty()) {
            binding.homeactivityCreateeventindicator.visibility = View.VISIBLE
        }

        // Initialize RecyclerView
        recyclerView = binding.homeactivityRecyclerview
        recyclerView.layoutManager = LinearLayoutManager(this)
        clickSessionAdapter = ClickSessionAdapter(
            sessions = sessions,
            onDeleteClicked = { session ->
                sessions.remove(session)
                PreferencesHelper.saveSessionsToPreferences(this, sessions)
                clickSessionAdapter.notifyDataSetChanged()
            },
            onRunClicked = { session ->
                proceedToHomeScreen()

                // Retrieve data from the session object
                val totalDurationInMs = session.totalDurationInMs
                val lowerLimitInMs = session.delayLowerLimitMs
                val higherLimitInMs = session.delayUpperLimitMs

                // Initialize and display the floating window
                val executeFloatingWindow = MyAccessibilityService.instance?.setupExecuteOverlay()

                // Find buttons from the floating window
                val playButton = executeFloatingWindow?.findViewById<ImageButton>(R.id.play_button)
                val cancelButton = executeFloatingWindow?.findViewById<ImageButton>(R.id.cancel_button)

                playButton?.setOnClickListener {
                   startClickingCycle(lowerLimitInMs,higherLimitInMs,totalDurationInMs)

                    // Hide the play button after it's clicked
                    playButton.visibility = View.GONE
                }

                cancelButton?.setOnClickListener {
                    MyAccessibilityService.instance?.stopContinuousClicking()

                    // Remove the floating window
                    val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                    windowManager.removeView(executeFloatingWindow)
                    startActivity(Intent(this, HomeActivity::class.java))
                }
            }
        )

        recyclerView.adapter = clickSessionAdapter

        // Set up Tutorial button
        binding.Tutorialbutton.setOnClickListener {
            val intent = Intent(this, TutorialActivity::class.java)
            startActivity(intent)
        }

        // Set up Create Event button
        binding.createeventbutton.setOnClickListener {
            val intent = Intent(this, EventActivity::class.java)
            startActivity(intent)
        }
    }
    private fun proceedToHomeScreen() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        this.startActivity(intent)
    }
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
