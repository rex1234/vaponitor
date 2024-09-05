package life.vaporized.servermonitor.app.monitor.resources

import life.vaporized.servermonitor.app.monitor.IResourceMonitor
import life.vaporized.servermonitor.app.monitor.model.MonitorStatus
import oshi.SystemInfo

object RamUsageMonitor : IResourceMonitor {

    override val id = "RRam"
    override val name: String = "RAM usage"
    override val message: String = "Current ram usage"

    override suspend fun evaluate(): List<MonitorStatus.ResourceStatus> {
        val memory = SystemInfo().hardware.memory
        val total = memory.total / (1024 * 1024)
        val free = memory.available / (1024 * 1024)

        return MonitorStatus.ResourceStatus(
            id = id,
            name = name,
            description = message,
            current = (total - free).toFloat(),
            total = total.toFloat(),
        ).let { listOf(it) }
    }
}
