package com.autotap.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.delay

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var packageInput: EditText
    private lateinit var targetInput: EditText
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        packageInput = findViewById(R.id.packageInput)
        targetInput = findViewById(R.id.targetInput)
        logView = findViewById(R.id.logView)

        findViewById<Button>(R.id.enableButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.runButton).setOnClickListener { runTapTest() }
        findViewById<Button>(R.id.dumpButton).setOnClickListener { runScreenDump() }
    }

    override fun onResume() {
        super.onResume()
        status.text = if (AutomationService.instance != null) {
            "Automation service: ON"
        } else {
            "Automation service: OFF. Tap the button below, find \"AutoTap automation\" " +
                "(it may be under \"Installed apps\" or \"Downloaded apps\") and turn it on."
        }
        logView.text = RunLog.text
        RunLog.listener = { logView.text = it }
    }

    override fun onPause() {
        RunLog.listener = null
        super.onPause()
    }

    private fun runTapTest() {
        val service = requireService() ?: return
        val pkg = packageInput.text.toString().trim()
        val target = targetInput.text.toString().trim()
        service.runTask("Tap \"$target\" in $pkg") {
            openApp(pkg)
            tapText(target)
            delay(2_000) // leave time to see the result before coming back
            returnToAutoTap()
        }
    }

    private fun runScreenDump() {
        val service = requireService() ?: return
        val pkg = packageInput.text.toString().trim()
        service.runTask("List on-screen text in $pkg") {
            openApp(pkg)
            delay(1_500) // let the first screen finish loading
            describeScreen().forEach { log(it) }
            returnToAutoTap()
        }
    }

    private fun requireService(): AutomationService? {
        val service = AutomationService.instance
        if (service == null) {
            Toast.makeText(this, "Turn on the AutoTap automation service first", Toast.LENGTH_LONG).show()
        }
        return service
    }
}
