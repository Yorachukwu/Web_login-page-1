package com.autotap.app

import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** In-memory log of the latest task, shown on the main screen. */
object RunLog {
    private val lines = mutableListOf<String>()
    private val main = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    /** Called on the main thread whenever the log changes. */
    var listener: ((String) -> Unit)? = null

    val text: String
        get() = synchronized(lines) { lines.joinToString("\n") }

    fun start(taskName: String) {
        synchronized(lines) { lines.clear() }
        add("Task: $taskName")
    }

    fun add(message: String) {
        synchronized(lines) { lines += "${timeFormat.format(Date())}  $message" }
        main.post { listener?.invoke(text) }
    }
}
