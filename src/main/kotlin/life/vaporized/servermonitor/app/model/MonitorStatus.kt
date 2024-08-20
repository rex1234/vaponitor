package life.vaporized.servermonitor.app.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface MonitorStatus {
    val id: String
    val name: String
    val description: String
    val isAlive: Boolean

    @Serializable
    data class AppStatus(
        val app: AppDefinition,
        val isRunning: Boolean,
        val isHttpReachable: Boolean?,
        val isHttpsReachable: Boolean?,
        val message: String? = null,
    ) : MonitorStatus {

        val isError
            get() = !isRunning || isHttpReachable == false || isHttpsReachable == false

        override val id: String
            get() = "A${app.name}"

        override val name: String
            get() = app.name

        override val description: String
            get() = app.description

        override val isAlive: Boolean
            get() = isRunning
    }

    @Serializable
    data class ResourceStatus(
        override val id: String,
        override val name: String,
        override val description: String,
        override val isAlive: Boolean = true,
        val current: Float,
        val total: Float,
    ) : MonitorStatus {

        val free: Float
            get() = total - current

        val usage: Float
            get() = current / total * 100
    }
}