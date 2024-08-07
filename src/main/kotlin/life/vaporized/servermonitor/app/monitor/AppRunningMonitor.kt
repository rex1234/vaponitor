package life.vaporized.servermonitor.app.monitor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import life.vaporized.servermonitor.app.model.AppDefinition
import life.vaporized.servermonitor.app.model.MonitorStatus
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class AppRunningMonitor(
    private val app: AppDefinition,
) : IMonitor<MonitorStatus.AppStatus> {

    override val name: String
        get() = app.name

    override val message: String
        get() = app.url ?: ""

    override suspend fun evaluate(): MonitorStatus.AppStatus = coroutineScope {
        val isProcessRunning = async {
            isProcessRunning(app.command)
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
        )
    }

    private suspend fun isProcessRunning(processName: String) = withContext(Dispatchers.IO) {
        println("Running pgrep for $processName")

        try {
            val processBuilder = ProcessBuilder("pgrep", "-f", processName)
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            // Check if there is any output from pgrep
            reader.readLine() != null
        } catch (e: Exception) {
            false
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