package life.vaporized.servermonitor.app.monitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import life.vaporized.servermonitor.app.monitor.model.AppDefinition
import life.vaporized.servermonitor.app.monitor.model.MonitorStatus
import life.vaporized.servermonitor.app.util.getLogger
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class AppRunningMonitor(
    private val app: AppDefinition,
) : IMonitor<MonitorStatus.AppStatus> {

    private val logger = getLogger()
    private val httpClient = OkHttpClient()

    override val name: String
        get() = app.name

    override val message: String
        get() = app.url ?: ""

    override val id = name

    override suspend fun evaluate(): List<MonitorStatus.AppStatus> = coroutineScope {
        val isProcessRunning = async {
            app.command?.let { isProcessRunning(app) } ?: true
        }
        val isReachableHttp = async {
            app.url?.let { isUrlReachable("http://$it") }
        }
        val isHttpsReachableHttps = async {
            if (app.https) {
                app.url?.let { isUrlReachable("https://$it") }
            } else {
                null
            }
        }
        MonitorStatus.AppStatus(
            app = app,
            isRunning = isProcessRunning.await(),
            isHttpReachable = isReachableHttp.await(),
            isHttpsReachable = isHttpsReachableHttps.await(),
        ).let { listOf(it) }
    }

    private suspend fun isProcessRunning(app: AppDefinition) = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("bash", "-c", app.command)
                .apply {
                    environment().putAll(System.getenv())
                }
                .start()

            var hasOutput = false
            process.inputStream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    if (line.isNotEmpty()) {
                        hasOutput = true
                    }
                }
            }
            val exitCode = process.waitFor()

            return@withContext exitCode == 0 && hasOutput
        } catch (e: Exception) {
            logger.debug("Process evaluation failed", e)
            return@withContext false
        }
    }

    private suspend fun isUrlReachable(url: String): Boolean = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: IOException) {
            logger.debug("URL check failed ($url) {}", e::class.simpleName)
            false
        }
    }
}
