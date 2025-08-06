package net.matsudamper.normalize_share_image.core

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageConverter(private val context: Context) {
    
    suspend fun convertImages(
        uris: List<Uri>,
        contentResolver: ContentResolver,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<Uri> = withContext(Dispatchers.IO) {
        val convertedUris = mutableListOf<Uri>()
        
        uris.forEachIndexed { index, uri ->
            onProgress(index + 1, uris.size)
            
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val mimeType = contentResolver.getType(uri)
                
                // すべての画像をEXIF対応で処理
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmap?.let { originalBitmap ->
                    // EXIF情報を読み取って回転を適用
                    val rotatedBitmap = applyExifRotation(uri, originalBitmap, contentResolver)
                    
                    // JPEG/PNGで回転が不要ならそのまま、必要なら変換
                    val needsConversion = mimeType != "image/jpeg" && mimeType != "image/png" || rotatedBitmap != originalBitmap
                    
                    if (needsConversion) {
                        val convertedUri = convertToPng(rotatedBitmap)
                        convertedUri?.let { convertedUris.add(it) }
                    } else {
                        // 回転が不要な場合は元のURIをそのまま使用
                        convertedUris.add(uri)
                    }
                    
                    // 新しいBitmapを作成した場合はメモリを解放
                    if (rotatedBitmap != originalBitmap) {
                        rotatedBitmap.recycle()
                    }
                    originalBitmap.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        convertedUris
    }
    
    private fun applyExifRotation(uri: Uri, bitmap: Bitmap, contentResolver: ContentResolver): Bitmap {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    ExifInterface.ORIENTATION_TRANSPOSE -> {
                        matrix.postRotate(90f)
                        matrix.postScale(-1f, 1f)
                    }
                    ExifInterface.ORIENTATION_TRANSVERSE -> {
                        matrix.postRotate(270f)
                        matrix.postScale(-1f, 1f)
                    }
                    else -> return bitmap // 回転不要
                }
                
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } ?: bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
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
}