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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.matsudamper.normalize_share_image.core.CacheManager
import net.matsudamper.normalize_share_image.core.ImageConverter
import net.matsudamper.normalize_share_image.ui.theme.NormalizeImageShareTheme

class PickerActivity : ComponentActivity() {
    private lateinit var imageConverter: ImageConverter
    private lateinit var cacheManager: CacheManager
    private var isProcessing by mutableStateOf(false)
    private var progress by mutableStateOf(0 to 0) // current to total
    
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
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            processAndReturnImages(uris)
        } else {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        imageConverter = ImageConverter(this)
        cacheManager = CacheManager(this)
        
        // 初回起動時のみキャッシュクリーンアップを実行
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                cacheManager.cleanupAllCache()
            }
        }
        
        setContent {
            NormalizeImageShareTheme {
                if (isProcessing) {
                    ProcessingScreen(
                        current = progress.first,
                        total = progress.second
                    )
                }
            }
        }
        
        // 権限チェック
        if (checkPermissions()) {
            launchImagePicker()
        } else {
            requestPermissions()
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
    
    private fun launchImagePicker() {
        imagePickerLauncher.launch("image/*")
    }
    
    private fun processAndReturnImages(uris: List<Uri>) {
        isProcessing = true
        
        lifecycleScope.launch {
            try {
                val convertedUris = imageConverter.convertImages(
                    uris = uris,
                    contentResolver = contentResolver
                ) { current, total ->
                    progress = current to total
                }
                
                // PICKの場合は単一画像、GET_CONTENTの場合は複数対応
                val resultIntent = Intent().apply {
                    if (convertedUris.size == 1) {
                        data = convertedUris.first()
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        // 複数画像の場合はEXTRA_STREAMに配列で設定
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(convertedUris))
                        // 複数画像の場合は各URIに権限を付与
                        convertedUris.forEach { uri ->
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            // clipDataも設定して互換性を向上
                            if (clipData == null) {
                                clipData = android.content.ClipData.newRawUri("", uri)
                            } else {
                                clipData?.addItem(android.content.ClipData.Item(uri))
                            }
                        }
                    }
                }
                
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }
    }
}

@Composable
private fun ProcessingScreen(
    current: Int,
    total: Int
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "画像を変換中...",
                style = MaterialTheme.typography.titleLarge
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (total > 1) {
                CircularProgressIndicator(
                    progress = { current.toFloat() / total.toFloat() }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "$current / $total",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                CircularProgressIndicator()
            }
        }
    }
}