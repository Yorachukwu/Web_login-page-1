package com.autotap.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class StepFailed(message: String) : Exception(message)

/** The building blocks a task is made of. Every step either succeeds or throws [StepFailed]. */
class Actions(private val service: AccessibilityService) {

    fun log(message: String) = RunLog.add(message)

    suspend fun openApp(packageName: String, timeoutMs: Long = 8_000) {
        val intent = service.packageManager.getLaunchIntentForPackage(packageName)
            ?: throw StepFailed("App not installed: $packageName")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        log("Opening $packageName")
        service.startActivity(intent)
        waitUntil("$packageName to open", timeoutMs) {
            service.rootInActiveWindow?.packageName?.toString() == packageName
        }
    }

    /** Brings AutoTap back to the front, e.g. so the user can read the log. */
    fun returnToAutoTap() {
        val intent = Intent(service, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        service.startActivity(intent)
    }

    /** Waits for an element whose text or content description matches [text], ignoring case. */
    suspend fun waitForText(text: String, timeoutMs: Long = 8_000): AccessibilityNodeInfo {
        var found: AccessibilityNodeInfo? = null
        waitUntil("\"$text\" to appear", timeoutMs) {
            found = findByText(text)
            found != null
        }
        return found!!
    }

    /** Exact matches win over partial ones, so "Send" doesn't hit "Send To..." when both exist. */
    fun findByText(text: String): AccessibilityNodeInfo? {
        val nodes = allNodes()
        fun AccessibilityNodeInfo.labels() = listOfNotNull(this.text, contentDescription).map { it.toString().trim() }
        return nodes.firstOrNull { node -> node.labels().any { it.equals(text, ignoreCase = true) } }
            ?: nodes.firstOrNull { node -> node.labels().any { it.contains(text, ignoreCase = true) } }
    }

    suspend fun tapText(text: String, timeoutMs: Long = 8_000) = tap(waitForText(text, timeoutMs))

    /** Clicks the element, or the nearest clickable parent; falls back to a real tap on its centre. */
    suspend fun tap(node: AccessibilityNodeInfo) {
        val label = node.text ?: node.contentDescription ?: node.viewIdResourceName ?: "element"
        var target: AccessibilityNodeInfo? = node
        while (target != null && !target.isClickable) target = target.parent
        if (target != null && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            log("Tapped \"$label\"")
            return
        }
        val bounds = Rect().also { node.getBoundsInScreen(it) }
        log("Tapping \"$label\" at (${bounds.centerX()}, ${bounds.centerY()})")
        tapAt(bounds.centerX().toFloat(), bounds.centerY().toFloat())
    }

    suspend fun tapAt(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
            .build()
        val completed = suspendCancellableCoroutine<Boolean> { cont ->
            val callback = object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) = cont.resume(true)
                override fun onCancelled(gestureDescription: GestureDescription?) = cont.resume(false)
            }
            if (!service.dispatchGesture(gesture, callback, null)) cont.resume(false)
        }
        if (!completed) throw StepFailed("Tap at ($x, $y) was rejected")
    }

    fun pressBack() = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)

    fun pressHome() = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)

    /** One line per element that has text, a description or an ID; used to find tap targets. */
    fun describeScreen(): List<String> = allNodes().mapNotNull { node ->
        val parts = listOfNotNull(
            node.text?.let { "text=\"$it\"" },
            node.contentDescription?.let { "desc=\"$it\"" },
            node.viewIdResourceName?.let { "id=${it.substringAfter(":id/")}" },
        )
        if (parts.isEmpty()) null
        else parts.joinToString(" ") + if (node.isClickable) " [clickable]" else ""
    }

    private fun allNodes(): List<AccessibilityNodeInfo> {
        val root = service.rootInActiveWindow ?: return emptyList()
        val result = mutableListOf<AccessibilityNodeInfo>()
        val queue = ArrayDeque(listOf(root))
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (!node.isVisibleToUser) continue
            result += node
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::addLast)
        }
        return result
    }

    private suspend fun waitUntil(what: String, timeoutMs: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() > deadline) throw StepFailed("Timed out waiting for $what")
            delay(250)
        }
    }
}
