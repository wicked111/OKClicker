package com.example.okclicker

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object PreferencesHelper {
    private const val PREFS_NAME = "click_sessions"
    private const val SESSIONS_KEY = "sessions"

    // Save the list of sessions to shared preferences
    fun saveSessionsToPreferences(context: Context, sessions: List<clickItem>) {
        val prefs = context.getSharedPreferences("ClickSessions", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json = Gson().toJson(sessions) // Convert the list to JSON format
        editor.putString("sessions", json)
        editor.apply()
    }

    // Load the list of sessions from shared preferences
    fun loadSessionsFromPreferences(context: Context): List<clickItem> {
        val prefs = context.getSharedPreferences("ClickSessions", Context.MODE_PRIVATE)
        val json = prefs.getString("sessions", null)
        return if (json != null) {
            val type = object : TypeToken<List<clickItem>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList() // Return an empty list if no data is saved
        }
    }
}
