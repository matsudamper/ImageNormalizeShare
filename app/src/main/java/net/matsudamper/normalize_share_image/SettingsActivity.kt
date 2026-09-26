package net.matsudamper.normalize_share_image

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.matsudamper.normalize_share_image.core.DefaultConversionSettingsRepository
import net.matsudamper.normalize_share_image.core.ImageFormat
import net.matsudamper.normalize_share_image.core.ImageQuality
import net.matsudamper.normalize_share_image.core.PerImageOption
import net.matsudamper.normalize_share_image.ui.SettingsScreen
import net.matsudamper.normalize_share_image.ui.theme.NormalizeImageShareTheme

class SettingsActivity : ComponentActivity() {
    private lateinit var defaultConversionSettingsRepository: DefaultConversionSettingsRepository
    private var defaultConversionOption by mutableStateOf(PerImageOption())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        defaultConversionSettingsRepository = DefaultConversionSettingsRepository(this)
        defaultConversionOption = defaultConversionSettingsRepository.load()

        setContent {
            NormalizeImageShareTheme {
                SettingsScreen(
                    defaultOption = defaultConversionOption,
                    onClickBack = { finish() },
                    onClickGitHubReleases = { openGitHubReleases() },
                    onDefaultFormatChanged = { format -> changeDefaultFormat(format) },
                    onDefaultQualityChanged = { quality ->
                        updateDefaultOption(defaultConversionOption.copy(quality = quality))
                    },
                )
            }
        }
    }

    private fun changeDefaultFormat(format: ImageFormat) {
        val current = defaultConversionOption
        val quality = if (format == ImageFormat.JPEG && current.quality == ImageQuality.LOSSLESS) {
            ImageQuality.VERY_HIGH
        } else {
            current.quality
        }
        updateDefaultOption(current.copy(format = format, quality = quality))
    }

    private fun updateDefaultOption(option: PerImageOption) {
        defaultConversionOption = option
        defaultConversionSettingsRepository.save(option)
    }

    private fun openGitHubReleases() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_RELEASES_URL)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "URL を開けるアプリが見つかりません", Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val GITHUB_RELEASES_URL = "https://github.com/matsudamper/ImageNormalizeShare/releases"
    }
}
