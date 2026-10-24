package com.example.tatkalhelper

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

/** Encrypted on-device storage (Android Keystore). Stores your IRCTC ID/password; never card numbers, UPI PINs or CAPTCHA. */
class Store(ctx: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        ctx,
        "tatkal_prefs",
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun save(c: Config) {
        val arr = JSONArray()
        c.passengers.forEach {
            arr.put(
                JSONObject()
                    .put("name", it.name).put("age", it.age)
                    .put("gender", it.gender).put("berth", it.berth)
            )
        }
        val o = JSONObject()
            .put("from", c.from).put("to", c.to).put("date", c.date)
            .put("class", c.travelClass).put("train", c.trainNo)
            .put("payment", c.paymentLabel).put("senders", c.otpSenders)
            .put("user", c.username).put("pass", c.password)
            .put("passengers", arr)
            .put("children", toJson(c.children))
            .put("upiApp", c.upiApp).put("upiId", c.upiId)
        prefs.edit().putString("config", o.toString()).apply()
    }

    fun load(): Config {
        val s = prefs.getString("config", null) ?: return Config()
        return try {
            val o = JSONObject(s)
            val arr = o.optJSONArray("passengers") ?: JSONArray()
            val list = (0 until arr.length()).map {
                val p = arr.getJSONObject(it)
                Passenger(p.optString("name"), p.optString("age"), p.optString("gender"), p.optString("berth"))
            }
            Config(
                from = o.optString("from"), to = o.optString("to"), date = o.optString("date"),
                travelClass = o.optString("class"), trainNo = o.optString("train"),
                paymentLabel = o.optString("payment", "UPI"),
                otpSenders = o.optString("senders", "IRCTC,CRIS"),
                username = o.optString("user"), password = o.optString("pass"),
                passengers = list,
                children = fromJson(o.optJSONArray("children")),
                upiApp = o.optString("upiApp", "PhonePe"), upiId = o.optString("upiId")
            )
        } catch (e: Exception) {
            Config()
        }
    }

    private fun toJson(list: List<Passenger>): JSONArray {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().put("name", it.name).put("age", it.age).put("gender", it.gender).put("berth", it.berth))
        }
        return a
    }

    private fun fromJson(a: JSONArray?): List<Passenger> =
        if (a == null) emptyList() else (0 until a.length()).map {
            val p = a.getJSONObject(it)
            Passenger(p.optString("name"), p.optString("age"), p.optString("gender"), p.optString("berth"))
        }

    companion object {
        /** "Name, Age, M/F, berth" per line. */
        fun parsePassengers(text: String): List<Passenger> =
            text.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { line ->
                val p = line.split(",").map { it.trim() }
                Passenger(p.getOrElse(0) { "" }, p.getOrElse(1) { "" }, p.getOrElse(2) { "" }, p.getOrElse(3) { "" })
            }

        fun passengersToText(list: List<Passenger>): String =
            list.joinToString("\n") { "${it.name}, ${it.age}, ${it.gender}, ${it.berth}" }
    }
}
