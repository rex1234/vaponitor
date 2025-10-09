package life.vaporized.servermonitor.app.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.decodeFromString
import life.vaporized.servermonitor.app.config.model.MonitorConfig
import life.vaporized.servermonitor.app.monitor.IResourceMonitor
import life.vaporized.servermonitor.app.monitor.model.AppDefinition
import life.vaporized.servermonitor.app.monitor.resources.CpuUsageMonitor
import life.vaporized.servermonitor.app.monitor.resources.Dht22Monitor
import life.vaporized.servermonitor.app.monitor.resources.DiskUsageMonitor
import life.vaporized.servermonitor.app.monitor.resources.RamUsageMonitor
import life.vaporized.servermonitor.app.monitor.resources.RaspberryTempMonitor
import life.vaporized.servermonitor.app.util.getLogger
import net.mamoe.yamlkt.Yaml
import java.io.File

class MonitorConfigProvider {

    companion object {
        const val CONFIG_FILENAME = "monitorconfig.yaml"

        val CLEANUP_INTERVAL = 1.days

        val RESOURCE_MONITORS = listOf(
            CpuUsageMonitor,
            DiskUsageMonitor,
            RamUsageMonitor,
            RaspberryTempMonitor,
            Dht22Monitor,
        )
    }

    private val logger = getLogger()

    private val monitorConfig: MonitorConfig by lazy {
        try {
            val yamlData = File(CONFIG_FILENAME).readText()
            Yaml.Default.decodeFromString<MonitorConfig>(yamlData).also {
                logger.info(
                    "Loaded app monitors [{}] and resource [{}]",
                    it.apps?.joinToString { app -> app.name } ?: "none",
                    it.resources?.items?.joinToString { item -> item.id } ?: "none"
                )
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load monitor config", e)
        }
    }

    val resourceMonitorInterval: Duration
        get() = monitorConfig.appMonitorRefreshIntervalS.seconds

    val appMonitorInterval: Duration
        get() = monitorConfig.resourceMonitorRefreshIntervalS.seconds

    val historyDuration: Duration
        get() = monitorConfig.historyDurationM.minutes

    val dbPurgeDuration: Duration?
        get() = monitorConfig.dbPurgeDays?.days

    val resourcePurgeSettings: Map<String, Duration>
        get() = monitorConfig.resources?.items
            ?.filter { it.dbPurgeDays != null }
            ?.associate { it.id to it.dbPurgeDays!!.days }
            ?: emptyMap()

    val appDefinitions: List<AppDefinition>
        get() = monitorConfig.apps ?: emptyList()

    val enabledResourceMonitors: List<IResourceMonitor>
        get() = RESOURCE_MONITORS.filter {
            it.id in (monitorConfig.resources?.items?.map { item -> item.id } ?: emptyList())
        }
}
