# AutoTap

An Android app that runs automations by controlling the screen through Android's
Accessibility Service: it opens apps, finds buttons by their text and taps them.

## Status: step 1 of 5

Done:
- **`AutomationService`**: the Accessibility Service. It runs one task at a time and can stop it.
- **`Actions`**: the building blocks tasks are made of: `openApp`, `waitForText`, `tapText`,
  `tap`, `tapAt` (a real touch gesture), `pressBack`, `pressHome`, and `describeScreen`.
- **Test screen**: runs a task that opens any app and taps a button by its text. Another
  button lists every element on an app's first screen, which is how you find what to tap.

Still to do:
2. A kill switch: pressing the power button (screen off) stops the task, plus a floating stop button.
3. A step engine with retries, and the Snapchat flow.
4. Scheduling (AlarmManager plus a foreground service).
5. A task editor UI, then an optional AI mode.

## Getting the APK

Every push that changes `android-automation/` runs the **Build AutoTap APK** workflow on
GitHub Actions. To get the APK:

1. Open the run in the repository's **Actions** tab.
2. Download the **autotap-debug-apk** artifact and unzip it.
3. Install `app-debug.apk` on the phone. You'll need to allow installs from unknown sources.

To build locally instead, use Android Studio, or run `./gradlew assembleDebug` with the Android SDK installed.

## Trying it

1. Open AutoTap and tap **Open Accessibility settings**.
2. Turn on **AutoTap automation**. On Android 13 and later, sideloaded apps are blocked from
   this at first. If the switch is greyed out, go to **App info > ⋮ > Allow restricted
   settings**, then try again.
3. Back in AutoTap, leave the defaults (`com.android.settings` / `Battery`) and tap **Run test**.
   Settings should open, tap Battery by itself, then return to AutoTap with a log.
4. To see what you could tap in another app, enter its package name (Snapchat is
   `com.snapchat.android`) and tap **Open app and list its on-screen text**.
