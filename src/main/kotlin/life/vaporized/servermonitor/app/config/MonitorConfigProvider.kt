package life.vaporized.servermonitor.app.config

import kotlinx.serialization.decodeFromString
import life.vaporized.servermonitor.app.config.model.MonitorConfig
import net.mamoe.yamlkt.Yaml
import java.io.File
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class MonitorConfigProvider {

    private companion object {
        const val CONFIG_FILENAME = "monitorconfig.yaml"
    }

    private val monitorConfig by lazy {
        val yamlData = File(CONFIG_FILENAME).readText()
        Yaml.Default.decodeFromString<MonitorConfig>(yamlData)
    }

    val resourceMonitorInterval
        get() = monitorConfig.appMonitorRefreshIntervalS.seconds

    val appMonitorInterval
        get() = monitorConfig.resourceMonitorRefreshIntervalS.seconds

    val historyDuration
        get() = monitorConfig.historyDurationM.minutes

    val appDefinitions
        get() = monitorConfig.appDefinitions

    val numberResourceDefinitions
        get() = monitorConfig.numberResourceDefinitions
}