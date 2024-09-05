package life.vaporized.servermonitor.app.config

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.decodeFromString
import life.vaporized.servermonitor.app.config.model.MonitorConfig
import life.vaporized.servermonitor.app.monitor.resources.CpuUsageMonitor
import life.vaporized.servermonitor.app.monitor.resources.DiskUsageMonitor
import life.vaporized.servermonitor.app.monitor.resources.RamUsageMonitor
import net.mamoe.yamlkt.Yaml
import java.io.File

class MonitorConfigProvider {

    private companion object {
        const val CONFIG_FILENAME = "monitorconfig.yaml"
        private val RESOURCE_MONITORS = listOf(
            CpuUsageMonitor,
            DiskUsageMonitor,
            RamUsageMonitor,
        )
    }

    private val monitorConfig by lazy {
        try {
            val yamlData = File(CONFIG_FILENAME).readText()
            Yaml.Default.decodeFromString<MonitorConfig>(yamlData)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load monitor config", e)
        }
    }

    val resourceMonitorInterval
        get() = monitorConfig.appMonitorRefreshIntervalS.seconds

    val appMonitorInterval
        get() = monitorConfig.resourceMonitorRefreshIntervalS.seconds

    val historyDuration
        get() = monitorConfig.historyDurationM.minutes

    val appDefinitions
        get() = monitorConfig.apps ?: emptyList()

    val numberResourceDefinitions
        get() = monitorConfig.resources?.numberResourceDefinitions ?: emptyList()

    val enabledResourceMonitors
        get() = RESOURCE_MONITORS.filter {
            it.id in (monitorConfig.resources?.enabled ?: emptyList())
        }
}
