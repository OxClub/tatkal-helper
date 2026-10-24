package com.example.tatkalhelper

data class Passenger(
    val name: String,
    val age: String,
    val gender: String,
    val berth: String
)

data class Config(
    val from: String = "",
    val to: String = "",
    val date: String = "",
    val travelClass: String = "",
    val trainNo: String = "",
    val paymentLabel: String = "UPI",
    val otpSenders: String = "IRCTC,CRIS",
    val username: String = "",
    val password: String = "",
    val passengers: List<Passenger> = emptyList(),
    /** Children under 5 travelling without a berth (IRCTC "infant" section). */
    val children: List<Passenger> = emptyList(),
    /** Paytm, PhonePe, Google Pay, BHIM or "UPI ID" (type your UPI ID only, no app tile). */
    val upiApp: String = "PhonePe",
    val upiId: String = ""
)
