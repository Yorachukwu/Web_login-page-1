package com.autotap.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The part of the app that can see and control the screen. Android starts it once the user
 * enables "AutoTap automation" under Settings > Accessibility. Only one task runs at a time.
 */
class AutomationService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentTask: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    // Tasks poll the screen themselves, so incoming events need no handling yet.
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = stopTask()

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        stopTask()
        scope.cancel()
        instance = null
        super.onDestroy()
    }

    val isRunning: Boolean
        get() = currentTask?.isActive == true

    fun runTask(name: String, steps: suspend Actions.() -> Unit) {
        currentTask?.cancel()
        RunLog.start(name)
        currentTask = scope.launch {
            try {
                Actions(this@AutomationService).steps()
                RunLog.add("Done")
            } catch (e: CancellationException) {
                RunLog.add("Stopped")
                throw e
            } catch (e: Exception) {
                RunLog.add("Failed: ${e.message}")
            }
        }
    }

    fun stopTask() {
        currentTask?.cancel()
        currentTask = null
    }

    companion object {
        /** The connected service, or null while it is turned off in Accessibility settings. */
        @Volatile
        var instance: AutomationService? = null
            private set
    }
}
