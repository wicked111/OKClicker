package com.example.okclicker

data class clickItem(
    val eventName: String,
    val eventDate: String,
    val totalDurationInMs: Long,
    val hourLowerLimit: Int,
    val hourUpperLimit: Int,
    val delayLowerLimitMs: Int,
    val delayUpperLimitMs: Int,
    val coordinates: Pair<Int, Int>,
    val timestamp: Long
   // val fixedLimitInMs: Int,

)
