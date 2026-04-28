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

enum class ImageFormat(val displayName: String, val extension: String, val mimeType: String) {
    PNG("PNG", "png", "image/png"),
    WEBP("WebP", "webp", "image/webp");
}

enum class ImageQuality(val displayName: String, val value: Int) {
    LOW("低画質 (25%)", 25),
    MEDIUM("中画質 (50%)", 50),
    HIGH("高画質 (75%)", 75),
    VERY_HIGH("最高画質 (100%)", 100);
}

data class ConvertedImage(
    val uri: Uri,
    val file: File,
    val fileSize: Long,
    val format: ImageFormat,
    val quality: ImageQuality
)

data class PerImageOption(
    val format: ImageFormat = ImageFormat.PNG,
    val quality: ImageQuality = ImageQuality.VERY_HIGH
)

class ImageConverter(private val context: Context) {
    
    suspend fun convertImages(
        uris: List<Uri>,
        contentResolver: ContentResolver,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<Uri> = withContext(Dispatchers.IO) {
        convertImagesWithOptions(
            uris = uris,
            contentResolver = contentResolver,
            format = ImageFormat.PNG,
            quality = ImageQuality.VERY_HIGH,
            onProgress = onProgress
        ).map { it.uri }
    }

    suspend fun convertImagesWithOptions(
        uris: List<Uri>,
        contentResolver: ContentResolver,
        format: ImageFormat = ImageFormat.PNG,
        quality: ImageQuality = ImageQuality.VERY_HIGH,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<ConvertedImage> = withContext(Dispatchers.IO) {
        val options = uris.map { PerImageOption(format, quality) }
        convertImagesWithPerImageOptions(uris, contentResolver, options, onProgress)
    }

    suspend fun convertImagesWithPerImageOptions(
        uris: List<Uri>,
        contentResolver: ContentResolver,
        options: List<PerImageOption>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<ConvertedImage> = withContext(Dispatchers.IO) {
        val convertedImages = mutableListOf<ConvertedImage>()
        
        uris.forEachIndexed { index, uri ->
            onProgress(index + 1, uris.size)
            val option = options.getOrElse(index) { PerImageOption() }
            
            try {
                val inputStream = contentResolver.openInputStream(uri)
                
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmap?.let { originalBitmap ->
                    val rotatedBitmap = applyExifRotation(uri, originalBitmap, contentResolver)
                    
                    val converted = convertImage(rotatedBitmap, option.format, option.quality, index)
                    converted?.let { convertedImages.add(it) }
                    
                    if (rotatedBitmap != originalBitmap) {
                        rotatedBitmap.recycle()
                    }
                    originalBitmap.recycle()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        convertedImages
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
                    else -> return bitmap
                }
                
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } ?: bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }
    
    private fun convertImage(bitmap: Bitmap, format: ImageFormat, quality: ImageQuality, index: Int): ConvertedImage? {
        return try {
            val timestamp = System.currentTimeMillis()
            val fileName = "converted_${format.extension}_q${quality.value}_${timestamp}_$index.${format.extension}"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            
            val compressFormat = when (format) {
                ImageFormat.PNG -> Bitmap.CompressFormat.PNG
                // WebPは最高画質(100%)のときのみロスレス圧縮を使用し、
                // それ以外はロッシー圧縮（quality値でサイズと画質のバランスを調整）
                ImageFormat.WEBP -> {
                    if (quality == ImageQuality.VERY_HIGH) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    }
                }
            }
            
            bitmap.compress(compressFormat, quality.value, outputStream)
            outputStream.flush()
            outputStream.close()
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            ConvertedImage(
                uri = uri,
                file = file,
                fileSize = file.length(),
                format = format,
                quality = quality
            )
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}