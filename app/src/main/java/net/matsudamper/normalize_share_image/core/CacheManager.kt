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