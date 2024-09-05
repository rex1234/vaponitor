package life.vaporized.servermonitor.app.monitor.resources

import life.vaporized.servermonitor.app.monitor.IResourceMonitor
import life.vaporized.servermonitor.app.monitor.model.MonitorStatus.ResourceStatus
import life.vaporized.servermonitor.app.util.getLogger
import java.io.BufferedReader
import java.io.InputStreamReader

object CpuUsageMonitor : IResourceMonitor {

    private val logger = getLogger()

    override val id = "RCpu"
    override val name: String = "CPU usage"
    override val message: String = "Current CPU usage"

    override suspend fun evaluate(): List<ResourceStatus> =
        ResourceStatus(
            id = id,
            name = name,
            description = message,
            current = getCpuUsage(),
            total = 100f,
        ).let { listOf(it) }

    private fun getCpuUsage(): Float {
        try {
            val command = arrayOf("bash", "-c", "mpstat | tail -n 1 | awk '{print 100 - \$NF}'")
            val process = ProcessBuilder(*command).start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val cpuUsage = reader.readText().trim()
            process.waitFor()
            return cpuUsage.toFloat()
        } catch (e: Exception) {
            logger.error("Failed to get CPU usage", e)
            return 0f
        }
    }
}
