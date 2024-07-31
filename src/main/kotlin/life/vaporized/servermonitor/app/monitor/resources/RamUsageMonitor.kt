package life.vaporized.servermonitor.app.monitor.resources

import com.sun.management.OperatingSystemMXBean
import life.vaporized.servermonitor.app.model.MonitorStatus
import life.vaporized.servermonitor.app.monitor.IResourceMonitor
import java.lang.management.ManagementFactory

object RamUsageMonitor : IResourceMonitor {

    override val name: String = "RAM usage"
    override val message: String = "Current ram usage"

    override suspend fun evaluate(): MonitorStatus.ResourceStatus? =
        getMemoryUsage().let { (free, total) ->
            return MonitorStatus.ResourceStatus(
                name = name,
                description = message,
                current = (total - free).toFloat(),
                total = total.toFloat(),
            )
        }

    private fun getMemoryUsage(): Pair<Long, Long> {
        val osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
        val totalMemory = osBean.totalMemorySize / (1024 * 1024)
        val freeMemory = osBean.freeMemorySize / (1024 * 1024)
        return Pair(freeMemory, totalMemory)
    }
}