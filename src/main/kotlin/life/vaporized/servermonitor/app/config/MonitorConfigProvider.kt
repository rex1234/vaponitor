package life.vaporized.servermonitor.app.config

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.decodeFromString
import life.vaporized.servermonitor.app.config.model.MonitorConfig
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

    private var _monitorConfig: MonitorConfig? = null

    private val monitorConfig: MonitorConfig
        get() {
            if (_monitorConfig == null) {
                _monitorConfig = loadConfig()
            }
            return _monitorConfig!!
        }

    private fun loadConfig(): MonitorConfig {
        return try {
            val yamlData = File(CONFIG_FILENAME).readText()
            Yaml.Default.decodeFromString<MonitorConfig>(yamlData).also {
                logger.info(
                    "Loaded app monitors [{}] and resource [{}]",
                    it.apps?.joinToString { app -> app.name } ?: "none",
                    it.resources?.enabled?.joinToString() ?: "none"
                )
            }
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load monitor config", e)
        }
    }

    /**
     * Forces reload of the configuration from the YAML file
     */
    fun reload() {
        logger.info("Reloading configuration from $CONFIG_FILENAME")
        _monitorConfig = null // Clear the cache
        // Access monitorConfig to trigger reload
        monitorConfig
        logger.info("Configuration reloaded successfully")
    }

    val resourceMonitorInterval
        get() = monitorConfig.appMonitorRefreshIntervalS.seconds

    val appMonitorInterval
        get() = monitorConfig.resourceMonitorRefreshIntervalS.seconds

    val historyDuration
        get() = monitorConfig.historyDurationM.minutes

    val dbPurgeDuration
        get() = monitorConfig.dbPurgeDays.days

    val appDefinitions
        get() = monitorConfig.apps ?: emptyList()

    val enabledResourceMonitors
        get() = RESOURCE_MONITORS.filter {
            it.id in (monitorConfig.resources?.enabled ?: emptyList())
        }
}
