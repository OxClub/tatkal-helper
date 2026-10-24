package com.example.tatkalhelper

import java.util.concurrent.ConcurrentHashMap

/** In-memory state shared by the service and receivers (same process). Nothing here is persisted. */
object AutofillState {
    private const val ARM_MINUTES = 20
    private const val OTP_TTL_MS = 3 * 60 * 1000L

    @Volatile var armedUntil = 0L
    @Volatile var generation = 0
    @Volatile var dumpNext = false

    // Diagnostics shown on the main screen
    @Volatile var events = 0
    @Volatile var lastPkg = ""
    @Volatile var irctcPageSeen = false
    @Volatile var lastDump = "")

    private var otp: String? = null
    private var otpAt = 0L

    /** Keys of things already filled/clicked this session, so we never fight the user. */
    val done: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun isArmed() = System.currentTimeMillis() < armedUntil

    fun arm() {
        done.clear()
        armedUntil = System.currentTimeMillis() + ARM_MINUTES * 60_000L
        generation++
    }

    fun disarm() { armedUntil = 0L }

    @Synchronized fun setOtp(code: String) { otp = code; otpAt = System.currentTimeMillis() }

    @Synchronized fun takeOtp(): String? {
        val c = otp ?: return null
        if (System.currentTimeMillis() - otpAt > OTP_TTL_MS) { otp = null; return null }
        return c
    }

    @Synchronized fun clearOtp() { otp = null }
}
