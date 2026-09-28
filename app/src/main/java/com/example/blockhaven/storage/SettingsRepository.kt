package com.example.blockhaven.storage

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("blockhaven_prefs", Context.MODE_PRIVATE)

    var renderDistance: Int
        get() = prefs.getInt("render_distance", 4)
        set(value) = prefs.edit().putInt("render_distance", value).apply()

    var fov: Float
        get() = prefs.getFloat("fov", 75f)
        set(value) = prefs.edit().putFloat("fov", value).apply()

    var lookSensitivity: Float
        get() = prefs.getFloat("look_sens", 1.0f)
        set(value) = prefs.edit().putFloat("look_sens", value).apply()

    var invertY: Boolean
        get() = prefs.getBoolean("invert_y", false)
        set(value) = prefs.edit().putBoolean("invert_y", value).apply()

    var buttonOpacity: Float
        get() = prefs.getFloat("button_opacity", 0.65f)
        set(value) = prefs.edit().putFloat("button_opacity", value).apply()

    var masterVolume: Float
        get() = prefs.getFloat("master_vol", 0.8f)
        set(value) = prefs.edit().putFloat("master_vol", value).apply()

    var sfxVolume: Float
        get() = prefs.getFloat("sfx_vol", 0.8f)
        set(value) = prefs.edit().putFloat("sfx_vol", value).apply()

    var musicVolume: Float
        get() = prefs.getFloat("music_vol", 0.5f)
        set(value) = prefs.edit().putFloat("music_vol", value).apply()

    var hapticFeedback: Boolean
        get() = prefs.getBoolean("haptic_feedback", true)
        set(value) = prefs.edit().putBoolean("haptic_feedback", value).apply()

    var thermalThrottling: Boolean
        get() = prefs.getBoolean("thermal_throttle", true)
        set(value) = prefs.edit().putBoolean("thermal_throttle", value).apply()

    var powerModeEnabled: Boolean
        get() = prefs.getBoolean("power_mode_enabled", false)
        set(value) = prefs.edit().putBoolean("power_mode_enabled", value).apply()
}
