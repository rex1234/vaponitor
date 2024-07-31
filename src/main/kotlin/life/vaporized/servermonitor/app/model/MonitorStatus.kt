package life.vaporized.servermonitor.app.model

sealed interface MonitorStatus {
    val name: String
    val description: String
    val isAlive: Boolean

    data class AppStatus(
        val app: AppDefinition,
        val isRunning: Boolean,
        val isHttpsReachable: Boolean?,
        val message: String? = null,
    ) : MonitorStatus {

        override val name: String
            get() = app.name

        override val description: String
            get() = app.description

        override val isAlive: Boolean
            get() = isRunning
    }

    data class ResourceStatus(
        override val name: String,
        override val description: String,
        override val isAlive: Boolean = true,
        val current: Float,
        val total: Float,
    ) : MonitorStatus
}