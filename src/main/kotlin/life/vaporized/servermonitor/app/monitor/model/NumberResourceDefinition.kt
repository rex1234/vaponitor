package life.vaporized.servermonitor.app.monitor.model

import kotlinx.serialization.Serializable

@Serializable
data class NumberResourceDefinition(
    val name: String,
    val description: String,
    val command: String,
)
