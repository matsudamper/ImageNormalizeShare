package net.matsudamper.normalize_share_image.core

import android.content.Context

class DefaultConversionSettingsRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): PerImageOption {
        val fallback = PerImageOption()
        val formatName = preferences.getString(KEY_FORMAT, null)
        val qualityName = preferences.getString(KEY_QUALITY, null)
        return PerImageOption(
            format = ImageFormat.entries.firstOrNull { it.name == formatName } ?: fallback.format,
            quality = ImageQuality.entries.firstOrNull { it.name == qualityName } ?: fallback.quality,
        )
    }

    fun save(option: PerImageOption) {
        preferences.edit()
            .putString(KEY_FORMAT, option.format.name)
            .putString(KEY_QUALITY, option.quality.name)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "default_conversion_settings"
        const val KEY_FORMAT = "format"
        const val KEY_QUALITY = "quality"
    }
}
