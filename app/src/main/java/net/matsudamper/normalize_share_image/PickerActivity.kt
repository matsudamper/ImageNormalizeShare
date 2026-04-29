package net.matsudamper.normalize_share_image

import android.Manifest
import android.app.Activity
import android.content.ClipData
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
    private var hasImagesSelected = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchImagePicker()
        } else {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private val singleImagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            hasImagesSelected = true
            pendingSelectedUris.value = listOf(uri)
        } else if (!hasImagesSelected) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private val multipleImagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            hasImagesSelected = true
            pendingSelectedUris.value = uris
        } else if (!hasImagesSelected) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        // else: 再選択キャンセル → 既存の選択状態を維持
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

        if (savedInstanceState == null) {
            requestImageSelection()
        }
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
            launchImagePicker()
        } else {
            requestPermissions()
        }
    }

    private fun launchImagePicker() {
        val allowMultiple = intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        if (allowMultiple) {
            multipleImagePickerLauncher.launch("image/*")
        } else {
            singleImagePickerLauncher.launch("image/*")
        }
    }

    private fun returnConvertedImages(convertedImages: List<ConvertedImage>) {
        val uris = convertedImages.map { it.uri }
        val resultIntent = Intent().apply {
            if (uris.size == 1) {
                data = uris.first()
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                data = uris.first()
                clipData = ClipData.newRawUri("", uris.first()).also { clip ->
                    uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
