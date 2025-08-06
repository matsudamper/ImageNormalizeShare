package net.matsudamper.normalize_share_image.core

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CacheManager(private val context: Context) {
    
    suspend fun cleanupAllCache() = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val deleted = file.delete()
                    if (deleted) {
                        println("Deleted cache file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun cleanupConvertedImages() = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            
            cacheDir.listFiles()?.forEach { file ->
                if (file.isFile && file.name.startsWith("converted_") && file.name.endsWith(".png")) {
                    val deleted = file.delete()
                    if (deleted) {
                        println("Deleted converted image: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            calculateDirectorySize(cacheDir)
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
    
    
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        directory.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirectorySize(file)
            } else {
                file.length()
            }
        }
        return size
    }
}