# Privacy Policy for RoutineForge

*Last Updated: September 14, 2026*

**RoutineForge** ("we", "our", or "the App") is committed to protecting your privacy. This Privacy Policy explains our practices regarding user information.

---

### 1. 100% Offline & No Data Collection
- RoutineForge is designed from the ground up to be **completely offline and private**.
- We do **NOT** collect, transmit, store, or sell any personal data, usage analytics, location data, or device identifiers.
- All routines, categories, history sessions, notes, and calendar tasks are stored **locally on your device** in private app storage (`SharedPreferences`).

---

### 2. Device Permissions & Why We Need Them

RoutineForge requests only the minimum permissions necessary to deliver its core timer and alert functionality:

| Permission | Purpose |
|---|---|
| `android.permission.VIBRATE` | Triggers tactile haptic feedback during countdown transitions and reminder alarms. |
| `android.permission.POST_NOTIFICATIONS` | Displays active countdowns and heads-up reminder notifications on Android 13+. |
| `android.permission.SCHEDULE_EXACT_ALARM` & `USE_EXACT_ALARM` | Required to schedule punctual dual-stage calendar alerts (1-minute pre-alert and on-time alarms). |
| `android.permission.WAKE_LOCK` | Momentarily ensures that haptic alarms fire reliably when the phone screen is locked or sleeping. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Reschedules your pending calendar tasks when you reboot your phone. |
| `android.permission.FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE` | Keeps the visual timer and audio/vibration alerts running smoothly in the background and in Picture-in-Picture (PiP) mode. |

None of these permissions are used to collect personal data or transmit data over the internet.

---

### 3. Third-Party Services & Advertising
- RoutineForge contains **NO third-party advertising SDKs**, tracking cookies, or external analytics frameworks (e.g., Google Analytics, Facebook SDK, or Firebase Tracking).
- No network requests are made by the application.

---

### 4. Children's Privacy
Because RoutineForge does not collect any personal information whatsoever, it is safe for users of all ages, including children under 13.

---

### 5. Data Deletion & Retention
You maintain complete ownership and control of your data. You can delete individual routines or clear your entire history and database at any time using the in-app **Clear All Data** option in the app bar, or by uninstalling the application. Uninstalling the app permanently removes all local data.

---

### 6. Contact & Inquiries
If you have any questions or suggestions regarding this Privacy Policy, please open an issue on our GitHub repository:
https://github.com/md-musharraf/scheduler
