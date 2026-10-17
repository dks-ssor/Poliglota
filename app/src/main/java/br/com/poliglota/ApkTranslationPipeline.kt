package br.com.poliglota

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/** Local APK preparation utilities. Only handles APKs selected or installed by the user. */
object ApkTranslationPipeline {
    data class ApkReport(
        val fileName: String,
        val sizeBytes: Long,
        val resourceFiles: List<String>,
        val hasAssets: Boolean,
        val hasNativeLibraries: Boolean
    )

    fun exportInstalledApk(context: Context, packageName: String, destination: Uri): Boolean {
        return try {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.contentResolver.openOutputStream(destination)?.use { output ->
                File(info.sourceDir).inputStream().use { input -> input.copyTo(output) }
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun inspectApk(context: Context, apkUri: Uri): ApkReport? {
        return try {
            val cacheFile = File.createTempFile("poliglota-apk-", ".apk", context.cacheDir)
            context.contentResolver.openInputStream(apkUri)?.use { input ->
                FileOutputStream(cacheFile).use { output -> input.copyTo(output) }
            } ?: return null
            val resources = mutableListOf<String>()
            var hasAssets = false
            var hasNative = false
            ZipInputStream(cacheFile.inputStream().buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name.startsWith("res/values") && name.endsWith(".xml")) resources.add(name)
                    if (name.startsWith("assets/")) hasAssets = true
                    if (name.startsWith("lib/") && name.endsWith(".so")) hasNative = true
                    entry = zip.nextEntry
                }
            }
            ApkReport(cacheFile.name, cacheFile.length(), resources.take(300), hasAssets, hasNative)
        } catch (_: Exception) {
            null
        }
    }
}
