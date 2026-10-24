package com.example.tatkalhelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * Picks the OTP out of an incoming SMS, only while autofill is armed and only from the
 * sender keywords you configured. The code is kept in memory for 3 minutes, never logged or saved.
 */
class SmsReceiver : BroadcastReceiver() {
    private val otpRegex = Regex("(?<!\\d)\\d{4,8}(?!\\d)")

    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!AutofillState.isArmed()) return

        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val sender = msgs.firstOrNull()?.originatingAddress ?: return
        val body = msgs.joinToString("") { it.messageBody ?: "" }
        if (!body.contains("otp", ignoreCase = true)) return

        val keys = Store(ctx).load().otpSenders.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (keys.none { sender.contains(it, ignoreCase = true) }) return

        otpRegex.find(body)?.value?.let { AutofillState.setOtp(it) }
    }
}
