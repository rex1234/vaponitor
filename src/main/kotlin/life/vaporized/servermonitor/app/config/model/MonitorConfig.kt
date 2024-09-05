package life.vaporized.servermonitor.app.config.model

import life.vaporized.servermonitor.app.monitor.model.AppDefinition
import life.vaporized.servermonitor.app.monitor.model.NumberResourceDefinition

class MonitorConfig(
    val appMonitorRefreshIntervalS: Int,
    val resourceMonitorRefreshIntervalS: Int,
    val historyDurationM: Int,
    val appDefinitions: List<AppDefinition>,
    val numberResourceDefinitions: List<NumberResourceDefinition>,
)