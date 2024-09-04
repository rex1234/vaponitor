package life.vaporized.servermonitor.app.model

import kotlinx.serialization.Serializable

@Serializable
data class NumberResourceCommandDefinition(
    val name: String,
    val description: String,
    val command: String,
)
