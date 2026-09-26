package net.matsudamper.normalize_share_image.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.matsudamper.normalize_share_image.R
import net.matsudamper.normalize_share_image.core.ImageFormat
import net.matsudamper.normalize_share_image.core.ImageQuality
import net.matsudamper.normalize_share_image.core.PerImageOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    defaultOption: PerImageOption,
    onClickBack: () -> Unit,
    onClickGitHubReleases: () -> Unit,
    onDefaultFormatChanged: (ImageFormat) -> Unit,
    onDefaultQualityChanged: (ImageQuality) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "設定") },
                navigationIcon = {
                    IconButton(onClick = onClickBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "戻る"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                DefaultConversionSettingsSection(
                    defaultOption = defaultOption,
                    onFormatChanged = onDefaultFormatChanged,
                    onQualityChanged = onDefaultQualityChanged,
                )
            }
            item {
                HorizontalDivider()
            }
            item {
                ListItem(
                    modifier = Modifier.clickable(onClick = onClickGitHubReleases),
                    headlineContent = { Text(text = "GitHub リリース") },
                    supportingContent = { Text(text = "最新バージョンを確認する") },
                    trailingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_open_in_new),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun DefaultConversionSettingsSection(
    defaultOption: PerImageOption,
    onFormatChanged: (ImageFormat) -> Unit,
    onQualityChanged: (ImageQuality) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "デフォルトの変換設定",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        FormatSelector(
            label = "出力形式",
            selectedFormat = defaultOption.format,
            onFormatChanged = onFormatChanged
        )

        Spacer(modifier = Modifier.height(16.dp))

        QualitySelector(
            label = "画質",
            selectedQuality = defaultOption.quality,
            format = defaultOption.format,
            enabled = defaultOption.format != ImageFormat.PNG,
            onQualityChanged = onQualityChanged
        )
    }
}
