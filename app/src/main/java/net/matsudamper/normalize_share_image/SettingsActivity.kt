package net.matsudamper.normalize_share_image

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import net.matsudamper.normalize_share_image.ui.SettingsScreen
import net.matsudamper.normalize_share_image.ui.theme.NormalizeImageShareTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NormalizeImageShareTheme {
                SettingsScreen(
                    onClickBack = { finish() },
                    onClickGitHubReleases = { openGitHubReleases() },
                )
            }
        }
    }

    private fun openGitHubReleases() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_RELEASES_URL)))
    }

    private companion object {
        const val GITHUB_RELEASES_URL = "https://github.com/matsudamper/ImageNormalizeShare/releases"
    }
}
