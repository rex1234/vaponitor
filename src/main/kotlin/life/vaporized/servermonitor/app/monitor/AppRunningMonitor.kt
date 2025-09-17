package life.vaporized.servermonitor.app.monitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import life.vaporized.servermonitor.app.monitor.model.AppDefinition
import life.vaporized.servermonitor.app.monitor.model.MonitorStatus
import life.vaporized.servermonitor.app.util.getLogger
import life.vaporized.servermonitor.app.util.repeatOnError
import okhttp3.OkHttpClient
import okhttp3.Request

class AppRunningMonitor(
    private val app: AppDefinition,
) : IMonitor<MonitorStatus.AppStatus> {

    private val logger = getLogger()
    private val httpClient = OkHttpClient()

    override val name: String
        get() = app.name

    override val message: String
        get() = app.httpUrl ?: ""

    override val id = name

    override suspend fun evaluate(): List<MonitorStatus.AppStatus> = coroutineScope {
        val isProcessRunning = async {
            app.command?.let { isProcessRunning(app) } ?: true
        }
        val isReachableHttp = async {
            app.httpUrl?.let { isUrlReachable(it) }
        }
        val isHttpsReachableHttps = async {
            app.httpsUrl?.let { isUrlReachable(it) }
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

        repeatOnError(
            default = false,
            logError = { e ->
                logger.error("Failed to check URL $url", e)
            },
            action = {
                httpClient.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            },
        )
    }
}
