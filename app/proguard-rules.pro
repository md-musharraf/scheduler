# ============================================================================
# RoutineForge - Google Play Production ProGuard / R8 Optimization Rules
# ============================================================================

# --- Jetpack Compose ---
-keepattributes *Annotation*,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.ReadOnlyComposable *;
}
-dontwarn androidx.compose.**

# --- RoutineForge Data Models (JSON serialization & persistence) ---
-keep class com.example.routineforge.data.** { *; }
-keepclassmembers class com.example.routineforge.data.** {
    <fields>;
    <methods>;
}

# --- Background Services & Broadcast Receivers ---
-keep class com.example.routineforge.service.** extends android.content.BroadcastReceiver { *; }
-keep class com.example.routineforge.service.** extends android.app.Service { *; }
-keep class com.example.routineforge.widget.NothingTimerWidgetProvider { *; }

# --- Kotlin Coroutines & Reflection ---
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# --- AndroidX Core & Navigation ---
-keep class androidx.navigation3.** { *; }
-dontwarn androidx.navigation3.**
-keep class androidx.lifecycle.** { *; }

# --- Optimizations: Strip Log.d and Log.v in Release Builds ---
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
