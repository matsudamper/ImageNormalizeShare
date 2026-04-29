package net.matsudamper.normalize_share_image

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.matsudamper.normalize_share_image.core.CacheManager
import net.matsudamper.normalize_share_image.core.ConvertedImage
import net.matsudamper.normalize_share_image.core.PermissionManager
import net.matsudamper.normalize_share_image.ui.ImageConverterScreen
import net.matsudamper.normalize_share_image.ui.theme.NormalizeImageShareTheme

class MainActivity : ComponentActivity() {
    private lateinit var permissionManager: PermissionManager
    private lateinit var cacheManager: CacheManager
    private var pendingSharedUris by mutableStateOf<List<Uri>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionManager = PermissionManager(this)
        cacheManager = CacheManager(this)
        
        // 初回起動時のみキャッシュクリーンアップを実行
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                cacheManager.cleanupAllCache()
            }
        }
        
        setContent {
            NormalizeImageShareTheme {
                ImageConverterScreen(
                    externalSelectedUris = pendingSharedUris,
                    onExternalUrisConsumed = {
                        pendingSharedUris = emptyList()
                    },
                    onShareImages = { convertedImages -> shareConvertedImages(convertedImages) }
                )
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                imageUri?.let { pendingSharedUris = listOf(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                imageUris?.let { pendingSharedUris = it }
            }
        }
    }

    private fun shareConvertedImages(convertedImages: List<ConvertedImage>) {
        val uris = convertedImages.map { it.uri }
        val shareIntent = if (uris.size == 1) {
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uris.first())
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                uris.forEachIndexed { index, uri ->
                    if (index == 0) {
                        clipData = android.content.ClipData.newRawUri("", uri)
                    } else {
                        clipData?.addItem(android.content.ClipData.Item(uri))
                    }
                }
            }
        }
        val chooserIntent = Intent.createChooser(shareIntent, "画像を共有")
        startActivity(chooserIntent)
    }
    
    internal fun checkPermissions(): Boolean {
        return permissionManager.checkPermissions()
    }
}
