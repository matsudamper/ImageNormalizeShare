package net.matsudamper.normalize_share_image.core

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageProcessor(private val context: Context) {
    
    fun processAndShareImages(
        uris: List<Uri>,
        contentResolver: ContentResolver,
        onComplete: (Intent) -> Unit
    ) {
        val processedUris = mutableListOf<Uri>()
        
        uris.forEach { uri ->
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val mimeType = contentResolver.getType(uri)
                
                // JPEG/PNGはそのまま、その他はPNG変換
                if (mimeType == "image/jpeg" || mimeType == "image/png") {
                    processedUris.add(uri)
                } else {
                    // PNG変換処理
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val convertedUri = convertToPng(bitmap)
                    convertedUri?.let { processedUris.add(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // 変換した画像を共有
        val shareIntent = createShareIntent(processedUris)
        onComplete(shareIntent)
    }
    
    private fun convertToPng(bitmap: Bitmap): Uri? {
        return try {
            val file = File(context.cacheDir, "converted_${System.currentTimeMillis()}.png")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    private fun createShareIntent(uris: List<Uri>): Intent {
        return if (uris.size == 1) {
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
                // ClipDataも設定して互換性を向上
                uris.forEachIndexed { index, uri ->
                    if (index == 0) {
                        clipData = android.content.ClipData.newRawUri("", uri)
                    } else {
                        clipData?.addItem(android.content.ClipData.Item(uri))
                    }
                }
            }
        }
    }
    
    fun shareImages(shareIntent: Intent) {
        val chooserIntent = Intent.createChooser(shareIntent, "画像を共有")
        context.startActivity(chooserIntent)
    }
}