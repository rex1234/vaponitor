package life.vaporized.servermonitor.app.monitor.model

import kotlinx.serialization.Serializable

@Serializable
data class MonitorEvaluation(
    val apps: List<MonitorStatus.AppStatus>,
    val resources: List<MonitorStatus.ResourceStatus>,
    val time: Long = System.currentTimeMillis(),
) {

    fun resourceWithId(id: String): MonitorStatus.ResourceStatus? = resources.find { it.id == id }

    fun appWithId(id: String): MonitorStatus.AppStatus? = apps.find { it.id == id }
}
