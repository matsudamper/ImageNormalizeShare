package net.matsudamper.normalize_share_image

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.matsudamper.normalize_share_image.core.CacheManager
import net.matsudamper.normalize_share_image.core.ConvertedImage
import net.matsudamper.normalize_share_image.ui.ImageConverterScreen
import net.matsudamper.normalize_share_image.ui.theme.NormalizeImageShareTheme

class PickerActivity : ComponentActivity() {
    private lateinit var cacheManager: CacheManager
    private var pendingSelectedUris = mutableStateOf<List<Uri>>(emptyList())
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            requestImageSelection()
        } else {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            pendingSelectedUris.value = uris
        } else {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    externalSelectedUris = pendingSelectedUris.value,
                    onExternalUrisConsumed = {
                        pendingSelectedUris.value = emptyList()
                    },
                    onRequestSelectImages = { requestImageSelection() },
                    onShareImages = { convertedImages -> returnConvertedImages(convertedImages) }
                )
            }
        }

        requestImageSelection()
    }
    
    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestPermissions() {
        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
    }
    
    private fun requestImageSelection() {
        if (checkPermissions()) {
            imagePickerLauncher.launch("image/*")
        } else {
            requestPermissions()
        }
    }

    private fun returnConvertedImages(convertedImages: List<ConvertedImage>) {
        val uris = convertedImages.map { it.uri }
        val resultIntent = Intent().apply {
            if (uris.size == 1) {
                data = uris.first()
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
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
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
