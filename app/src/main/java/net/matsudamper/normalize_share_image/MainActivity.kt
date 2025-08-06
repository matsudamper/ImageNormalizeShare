package net.matsudamper.normalize_share_image

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.matsudamper.normalize_share_image.core.CacheManager
import net.matsudamper.normalize_share_image.core.ImageProcessor
import net.matsudamper.normalize_share_image.core.PermissionManager
import net.matsudamper.normalize_share_image.ui.ImageConverterScreen
import net.matsudamper.normalize_share_image.ui.theme.NormalizeImageShareTheme

class MainActivity : ComponentActivity() {
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var permissionManager: PermissionManager
    private lateinit var cacheManager: CacheManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        imageProcessor = ImageProcessor(this)
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
                    onLaunch = { handleIntent(intent) },
                    onImageSelected = { uris: List<Uri> -> processAndShareImages(uris) }
                )
            }
        }
        
        // 起動時のインテント処理
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                imageUri?.let { processAndShareImages(listOf(it)) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                imageUris?.let { processAndShareImages(it) }
            }
        }
    }
    
    private fun processAndShareImages(uris: List<Uri>) {
        imageProcessor.processAndShareImages(
            uris = uris,
            contentResolver = contentResolver
        ) { shareIntent ->
            imageProcessor.shareImages(shareIntent)
        }
    }
    
    internal fun checkPermissions(): Boolean {
        return permissionManager.checkPermissions()
    }
}