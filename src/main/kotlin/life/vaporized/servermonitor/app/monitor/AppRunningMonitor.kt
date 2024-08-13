package life.vaporized.servermonitor.app.monitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import life.vaporized.servermonitor.app.model.AppDefinition
import life.vaporized.servermonitor.app.model.MonitorStatus
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class AppRunningMonitor(
    private val app: AppDefinition,
) : IMonitor<MonitorStatus.AppStatus> {

    override val name: String
        get() = app.name

    override val message: String
        get() = app.url ?: ""

    override suspend fun evaluate(): List<MonitorStatus.AppStatus> = coroutineScope {
        val isProcessRunning = async {
            isProcessRunning(app)
        }
        val isReachableHttp = async {
            app.url?.let { isUrlReachable("http://$it") }
        }
        val isHttpsReachableHttps = async {
            app.url?.let { isUrlReachable("https://$it") }
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
            println("Running ${app.name} - ${app.command}")

            val process = ProcessBuilder("bash", "-c", app.command)
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

            return@withContext (exitCode == 0 && hasOutput).also {
                println("Process ${app.name} running: $it")
            }
        } catch (e: Exception) {
            println(e)
            return@withContext false
        }
    }

    private suspend fun isUrlReachable(url: String): Boolean = withContext(Dispatchers.IO) {
        println("Checking reachability for $url")

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: IOException) {
            println(e)
            false
        }
    }
}