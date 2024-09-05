package life.vaporized.servermonitor.app.monitor.model

import kotlinx.serialization.Serializable

@Serializable
data class MonitorEvaluation(
    val list: List<MonitorStatus>,
    val time: Long = System.currentTimeMillis(),
)