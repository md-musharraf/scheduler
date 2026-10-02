# Room, kotlinx.serialization, Navigation3 and Compose ship their own consumer rules;
# receivers and the activity are kept through the manifest. Only app-specific rules go here.

# Strip verbose logging from release builds.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
