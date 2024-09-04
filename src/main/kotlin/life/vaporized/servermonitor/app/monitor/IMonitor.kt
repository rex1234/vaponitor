package life.vaporized.servermonitor.app.monitor

import life.vaporized.servermonitor.app.model.MonitorStatus

interface IMonitor<T : MonitorStatus> {

    val id: String
    val name: String
    val message: String

    suspend fun evaluate(): List<T>
}