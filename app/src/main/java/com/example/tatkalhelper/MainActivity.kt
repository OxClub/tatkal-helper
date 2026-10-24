package com.example.tatkalhelper

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var store: Store
    private lateinit var status: TextView
    private lateinit var fFrom: EditText
    private lateinit var fTo: EditText
    private lateinit var fDate: EditText
    private lateinit var fClass: EditText
    private lateinit var fTrain: EditText
    private lateinit var fPayment: EditText
    private lateinit var fSenders: EditText
    private lateinit var fPassengers: EditText
    private lateinit var fUser: EditText
    private lateinit var fPass: EditText
    private lateinit var fChildren: EditText
    private var alarmText = "No alarm set"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        store = Store(this)

        status = findViewById(R.id.status)
        fFrom = findViewById(R.id.fFrom); fTo = findViewById(R.id.fTo)
        fDate = findViewById(R.id.fDate); fClass = findViewById(R.id.fClass)
        fTrain = findViewById(R.id.fTrain); fPayment = findViewById(R.id.fPayment)
        fSenders = findViewById(R.id.fSenders); fPassengers = findViewById(R.id.fPassengers)
        fUser = findViewById(R.id.fUser); fPass = findViewById(R.id.fPass)
        fChildren = findViewById(R.id.fChildren)

        val c = store.load()
        fFrom.setText(c.from); fTo.setText(c.to); fDate.setText(c.date)
        fClass.setText(c.travelClass); fTrain.setText(c.trainNo)
        fPayment.setText(c.paymentLabel); fSenders.setText(c.otpSenders)
        fPassengers.setText(Store.passengersToText(c.passengers))
        fUser.setText(c.username); fPass.setText(c.password)
        fChildren.setText(Store.passengersToText(c.children))

        val perms = mutableListOf(Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        ActivityCompat.requestPermissions(this, perms.toTypedArray(), 1)

        findViewById<Button>(R.id.btnSave).setOnClickListener { save(); toast("Saved") }
        findViewById<Button>(R.id.btnAc).setOnClickListener { setAlarm(9, 55) }
        findViewById<Button>(R.id.btnSl).setOnClickListener { setAlarm(10, 55) }
        findViewById<Button>(R.id.btnCancelAlarm).setOnClickListener {
            Scheduler.cancel(this); alarmText = "No alarm set"; refresh()
        }
        findViewById<Button>(R.id.btnA11y).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.btnArm).setOnClickListener {
            save()
            AutofillState.arm()
            startActivity(Irctc.appIntent(this))
        }
        findViewById<Button>(R.id.btnArmWeb).setOnClickListener {
            save()
            AutofillState.arm()
            startActivity(Irctc.websiteIntent())
        }
        findViewById<Button>(R.id.btnDump).setOnClickListener {
            AutofillState.dumpNext = true
            toast("Open an IRCTC screen; check Logcat for tag TatkalHelper")
        }
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun save() {
        store.save(
            Config(
                from = fFrom.text.toString().trim(), to = fTo.text.toString().trim(),
                date = fDate.text.toString().trim(), travelClass = fClass.text.toString().trim(),
                trainNo = fTrain.text.toString().trim(),
                paymentLabel = fPayment.text.toString().trim(),
                otpSenders = fSenders.text.toString().trim(),
                username = fUser.text.toString().trim(), password = fPass.text.toString(),
                passengers = Store.parsePassengers(fPassengers.text.toString()),
                children = Store.parsePassengers(fChildren.text.toString())
            )
        )
    }

    private fun setAlarm(h: Int, m: Int) {
        save()
        val at = Scheduler.schedule(this, h, m)
        alarmText = "Alarm: " + DateFormat.format("EEE d MMM, h:mm a", at)
        refresh()
    }

    private fun a11yEnabled(): Boolean {
        val s = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return s.contains("$packageName/")
    }

    private fun refresh() {
        status.text = alarmText + "\nAccessibility: " + (if (a11yEnabled()) "ON" else "OFF") +
            "\nAutofill: " + (if (AutofillState.isArmed()) "ARMED" else "not armed")
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
