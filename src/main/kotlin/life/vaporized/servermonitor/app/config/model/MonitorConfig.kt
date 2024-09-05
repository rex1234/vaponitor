package life.vaporized.servermonitor.app.config.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import life.vaporized.servermonitor.app.monitor.model.AppDefinition
import life.vaporized.servermonitor.app.monitor.model.NumberResourceDefinition

@Serializable
class MonitorConfig(
    @SerialName("app_monitor_interval_s")
    val appMonitorRefreshIntervalS: Int,
    @SerialName("resource_monitor_interval_s")
    val resourceMonitorRefreshIntervalS: Int,
    @SerialName("history_duration_m")
    val historyDurationM: Int,
    @SerialName("apps")
    val apps: List<AppDefinition>?,
    @SerialName("resources")
    val resources: Resources?,
) {

    @Serializable
    data class Resources(
        val enabled: List<String>,
        @SerialName("number")
        val numberResourceDefinitions: List<NumberResourceDefinition>,
    )
}