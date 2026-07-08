package com.krelinnbios.neodblite.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.krelinnbios.neodblite.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val versionName: String,
    val versionCode: Long?,
    val releaseNotes: String,
    val apkUrl: String,
    val releasePageUrl: String
)

/** APK 下载进度：当前来源、第几个源、已下载/总字节。totalBytes <= 0 表示未知。 */
data class AppDownloadProgress(
    val sourceLabel: String,
    val sourceIndex: Int,
    val sourceCount: Int,
    val downloadedBytes: Long,
    val totalBytes: Long
)

sealed interface AppUpdateCheckResult {
    data object NoUpdate : AppUpdateCheckResult
    data class UpdateAvailable(val info: AppUpdateInfo) : AppUpdateCheckResult
    /** [isRateLimited] 为 true 表示 GitHub 明确返回 403/429（限流），而非网络不通。 */
    data class Failed(val reason: String, val isRateLimited: Boolean = false) : AppUpdateCheckResult
}

object AppUpdateManager {
    const val RELEASES_PAGE_URL =
        "https://github.com/KrelinnBios/NeoDBLite/releases"
    const val RELEASES_API_URL =
        "https://api.github.com/repos/KrelinnBios/NeoDBLite/releases?per_page=20"

    // 下载源回退顺序：先 GitHub 直链，连不上再依次试镜像（前缀代理：把完整 GitHub 直链接在镜像域名后）。
    internal val DOWNLOAD_MIRROR_PREFIXES = listOf(
        "https://ghproxy.net/",
        "https://gh-proxy.com/",
        "https://gh.llkk.cc/"
    )

    /** 构造下载候选链：GitHub 直链优先，其后是各镜像前缀拼接的同一直链。 */
    internal fun buildDownloadCandidates(apkUrl: String): List<String> {
        val candidates = mutableListOf(apkUrl)
        if (apkUrl.startsWith("https://github.com/", ignoreCase = true)) {
            DOWNLOAD_MIRROR_PREFIXES.forEach { prefix -> candidates.add(prefix + apkUrl) }
        }
        return candidates.distinct()
    }

    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    private const val MAX_APK_SIZE_BYTES = 512L * 1024L * 1024L
    private const val APK_BASE_NAME = "NeoDB Lite"

    // 自动检查的最小间隔：GitHub 未认证接口限流约每小时 60 次（按 IP 计），频繁启动会触发 403。
    private const val AUTO_CHECK_MIN_INTERVAL_MS = 6L * 60 * 60 * 1000
    private const val PREFS_NAME = "app_update"
    private const val KEY_LAST_AUTO_CHECK = "last_auto_update_check_ms"
    private const val KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled"

    private val gson = Gson()

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.MINUTES)
            .callTimeout(5, TimeUnit.MINUTES)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    // 多源下载专用客户端：让失效/卡死的镜像快速失败，避免逐个长时间等待。
    private val downloadClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(4, TimeUnit.MINUTES)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * 启动时的自动检查：带节流。距上次自动检查不足 [AUTO_CHECK_MIN_INTERVAL_MS] 时返回 null（跳过），
     * 避免频繁启动把 GitHub 未认证接口的限流额度用尽（403）。
     * 只有拿到确定结论（有更新/无更新）才记录时间戳；网络失败不占用节流窗口，下次启动会重试。
     * 例外：GitHub 明确返回限流（403/429）时也记录时间戳——此时重试只会加剧限流，等满一个窗口再试。
     */
    suspend fun checkForUpdateAuto(context: Context): AppUpdateCheckResult? =
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val last = prefs.getLong(KEY_LAST_AUTO_CHECK, 0L)
            if (System.currentTimeMillis() - last < AUTO_CHECK_MIN_INTERVAL_MS) {
                return@withContext null
            }
            val result = checkForUpdate()
            if (result !is AppUpdateCheckResult.Failed || result.isRateLimited) {
                prefs.edit().putLong(KEY_LAST_AUTO_CHECK, System.currentTimeMillis()).apply()
            }
            result
        }

    fun isAutoUpdateEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_UPDATE_ENABLED, true)
    }

    fun setAutoUpdateEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_UPDATE_ENABLED, enabled)
            .apply()
    }

    suspend fun checkForUpdate(): AppUpdateCheckResult = withContext(Dispatchers.IO) {
        val endpoint = BuildConfig.APP_UPDATE_URL.trim().ifBlank { RELEASES_API_URL }
        val requestUrl = if ('?' in endpoint) {
            "$endpoint&_=${System.currentTimeMillis()}"
        } else {
            "$endpoint?_=${System.currentTimeMillis()}"
        }

        val request = Request.Builder()
            .url(requestUrl)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "NeoDBLite/${BuildConfig.VERSION_NAME}")
            .header("Cache-Control", "no-cache, no-store")
            .header("Pragma", "no-cache")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val rateLimited = response.code == 403 || response.code == 429
                    val reason = when {
                        rateLimited -> "GitHub 接口暂时不可用（${response.code}，通常是短时间内请求过于频繁触发限流），请稍后再试或前往 Releases 页面手动查看"
                        response.code == 404 -> "仓库暂无正式发布版本"
                        else -> "GitHub 返回 HTTP ${response.code}"
                    }
                    return@withContext AppUpdateCheckResult.Failed(reason, rateLimited)
                }

                val json = response.body?.string().orEmpty()
                val info = parseReleaseJson(json)
                    ?: return@withContext AppUpdateCheckResult.Failed(
                        "最新版本信息中没有可下载的 APK"
                    )

                if (compareVersions(info.versionName, BuildConfig.VERSION_NAME) <= 0) {
                    AppUpdateCheckResult.NoUpdate
                } else {
                    AppUpdateCheckResult.UpdateAvailable(info)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            AppUpdateCheckResult.Failed(error.message ?: "无法连接 GitHub")
        }
    }

    suspend fun downloadAndOpenInstaller(
        context: Context,
        info: AppUpdateInfo,
        onProgress: (AppDownloadProgress) -> Unit = {}
    ): Result<Unit> {
        val appContext = context.applicationContext
        return try {
            val apkFile = withContext(Dispatchers.IO) {
                downloadAndValidateApk(appContext, info, onProgress)
            }
            withContext(Dispatchers.Main) {
                openInstaller(appContext, apkFile)
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    fun openReleasesPage(context: Context): Result<Unit> = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_PAGE_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // ---- 发布信息解析（Gson，可在 JVM 单测中直接运行）----

    private data class GhAsset(
        val name: String? = null,
        val content_type: String? = null,
        val browser_download_url: String? = null,
        val versionCode: Long? = null
    )

    private data class GhRelease(
        val tag_name: String? = null,
        val version: String? = null,
        val versionName: String? = null,
        val body: String? = null,
        val releaseNotes: String? = null,
        val html_url: String? = null,
        val releaseUrl: String? = null,
        val draft: Boolean = false,
        val prerelease: Boolean = false,
        val assets: List<GhAsset>? = null,
        val downloadUrl: String? = null,
        val versionCode: Long? = null
    )

    internal fun parseReleaseJson(json: String): AppUpdateInfo? {
        val trimmed = json.trimStart()
        return try {
            if (trimmed.startsWith("[")) {
                val type = object : TypeToken<List<GhRelease>>() {}.type
                val releases: List<GhRelease> = gson.fromJson(json, type) ?: return null
                releases.mapNotNull { parseReleaseObject(it) }
                    .maxWithOrNull { a, b -> compareVersions(a.versionName, b.versionName) }
            } else {
                JsonParser.parseString(json) // 容错：非法 JSON 直接抛出
                parseReleaseObject(gson.fromJson(json, GhRelease::class.java))
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseReleaseObject(release: GhRelease?): AppUpdateInfo? {
        if (release == null) return null
        if (release.draft || release.prerelease) return null

        val latestVersion = (release.tag_name ?: release.versionName ?: release.version)
            ?.removePrefix("v")?.trim()?.takeIf { it.isNotBlank() }
            ?: return null

        var apkUrl: String? = null
        var versionCode: Long? = null
        release.assets?.forEach { asset ->
            if (apkUrl != null) return@forEach
            val name = asset.name.orEmpty()
            val contentType = asset.content_type.orEmpty()
            if (name.endsWith(".apk", true) || contentType.equals(APK_MIME_TYPE, true)) {
                apkUrl = asset.browser_download_url
                versionCode = asset.versionCode
            }
        }
        val selectedApkUrl = apkUrl
            ?: release.downloadUrl?.takeIf { it.endsWith(".apk", true) }
            ?: return null

        return AppUpdateInfo(
            versionName = latestVersion,
            versionCode = versionCode ?: release.versionCode,
            releaseNotes = release.body ?: release.releaseNotes ?: "",
            apkUrl = selectedApkUrl,
            releasePageUrl = release.html_url ?: release.releaseUrl ?: RELEASES_PAGE_URL
        )
    }

    // ---- 下载与校验 ----

    private fun downloadAndValidateApk(
        context: Context,
        info: AppUpdateInfo,
        onProgress: (AppDownloadProgress) -> Unit = {}
    ): File {
        val updateDir = File(context.externalCacheDir ?: context.cacheDir, "update")
        if (!updateDir.exists() && !updateDir.mkdirs()) {
            throw IOException("无法创建更新缓存目录")
        }

        updateDir.listFiles()
            ?.filter { it.name.startsWith("$APK_BASE_NAME-") || it.name == "$APK_BASE_NAME.apk" }
            ?.forEach { it.delete() }

        val safeVersionName = info.versionName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val targetFile = File(updateDir, "$APK_BASE_NAME-$safeVersionName.apk")
        val tempFile = File(updateDir, "${targetFile.name}.download")
        tempFile.delete()
        targetFile.delete()

        val candidates = buildDownloadCandidates(info.apkUrl)
        var lastError: Exception? = null

        candidates.forEachIndexed { index, url ->
            tempFile.delete()
            val label = downloadSourceLabel(url, info.apkUrl)
            try {
                downloadApkTo(url, tempFile) { downloaded, total ->
                    onProgress(
                        AppDownloadProgress(
                            sourceLabel = label,
                            sourceIndex = index + 1,
                            sourceCount = candidates.size,
                            downloadedBytes = downloaded,
                            totalBytes = total
                        )
                    )
                }
                validateApk(context, tempFile, info)
            } catch (error: CancellationException) {
                tempFile.delete()
                throw error
            } catch (error: Exception) {
                lastError = error
                tempFile.delete()
                return@forEachIndexed
            }

            targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
            return targetFile
        }

        throw lastError ?: IOException("所有下载源均不可用")
    }

    private fun downloadSourceLabel(candidateUrl: String, apkUrl: String): String {
        if (candidateUrl == apkUrl) return "GitHub 直链"
        val host = candidateUrl.removeSuffix(apkUrl)
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
        return if (host.isBlank()) "镜像" else "镜像 $host"
    }

    private fun downloadApkTo(
        apkUrl: String,
        tempFile: File,
        onBytes: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ) {
        val apkDownloadUrl = if ('?' in apkUrl) {
            "$apkUrl&_=${System.currentTimeMillis()}"
        } else {
            "$apkUrl?_=${System.currentTimeMillis()}"
        }

        val request = Request.Builder()
            .url(apkDownloadUrl)
            .header("Accept", APK_MIME_TYPE)
            .header("User-Agent", "NeoDBLite/${BuildConfig.VERSION_NAME}")
            .header("Cache-Control", "no-cache, no-store")
            .header("Pragma", "no-cache")
            .build()

        downloadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("APK 下载失败：HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("APK 下载内容为空")
            val contentLength = body.contentLength()
            if (contentLength > MAX_APK_SIZE_BYTES) {
                throw IOException("APK 文件大小异常")
            }

            body.byteStream().use { input ->
                tempFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var totalBytes = 0L
                    var lastReported = 0L
                    onBytes(0L, contentLength)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        if (totalBytes > MAX_APK_SIZE_BYTES) {
                            throw IOException("APK 文件大小异常")
                        }
                        output.write(buffer, 0, count)
                        if (totalBytes - lastReported >= 64 * 1024) {
                            lastReported = totalBytes
                            onBytes(totalBytes, contentLength)
                        }
                    }
                    if (totalBytes <= 0L || contentLength > 0L && totalBytes != contentLength) {
                        throw IOException("APK 下载不完整")
                    }
                    onBytes(totalBytes, contentLength)
                }
            }
        }
    }

    private fun validateApk(context: Context, apkFile: File, info: AppUpdateInfo) {
        val packageInfoFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageArchiveInfo(
                apkFile.absolutePath,
                PackageManager.PackageInfoFlags.of(packageInfoFlags.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, packageInfoFlags)
        } ?: throw IOException("下载的文件不是有效 APK")

        if (packageInfo.packageName != context.packageName) {
            throw IOException("APK 包名不匹配")
        }

        val downloadedVersionName = packageInfo.versionName.orEmpty()
        if (compareVersions(downloadedVersionName, info.versionName) != 0) {
            throw IOException(
                "APK versionName 与发布信息不一致：下载到 v$downloadedVersionName，预期 v${info.versionName}"
            )
        }

        val downloadedVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        if (downloadedVersionCode <= BuildConfig.VERSION_CODE.toLong()) {
            throw IOException("APK versionCode 未高于当前版本")
        }
        if (info.versionCode != null && downloadedVersionCode != info.versionCode) {
            throw IOException("APK versionCode 与发布信息不一致")
        }

        val installedPackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(packageInfoFlags.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, packageInfoFlags)
        }
        val installedCertificates = signingCertificateDigests(installedPackageInfo)
        val downloadedCertificates = signingCertificateDigests(packageInfo)
        if (installedCertificates.isNotEmpty() &&
            downloadedCertificates.isNotEmpty() &&
            installedCertificates.intersect(downloadedCertificates).isEmpty()
        ) {
            throw IOException("APK 签名与当前安装版本不一致，请卸载旧版后从下载页重新安装")
        }
    }

    private fun signingCertificateDigests(
        packageInfo: android.content.pm.PackageInfo
    ): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }
        val digest = MessageDigest.getInstance("SHA-256")
        return signatures.orEmpty().mapTo(linkedSetOf()) { signature ->
            digest.digest(signature.toByteArray()).joinToString("") { byte ->
                "%02x".format(byte)
            }
        }
    }

    private fun openInstaller(context: Context, apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (intent.resolveActivity(context.packageManager) == null) {
            throw IOException("系统中没有可用的 APK 安装器")
        }
        context.startActivity(intent)
    }

    internal fun compareVersions(left: String, right: String): Int {
        val leftParts = left.removePrefix("v").split('.', '-', '_')
        val rightParts = right.removePrefix("v").split('.', '-', '_')
        val size = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until size) {
            val leftValue = leftParts.getOrNull(index)?.toIntOrNull() ?: 0
            val rightValue = rightParts.getOrNull(index)?.toIntOrNull() ?: 0
            if (leftValue != rightValue) return leftValue.compareTo(rightValue)
        }
        return 0
    }
}
