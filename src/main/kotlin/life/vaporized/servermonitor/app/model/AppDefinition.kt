package life.vaporized.servermonitor.app.model

import kotlinx.serialization.Serializable

@Serializable
data class AppDefinition(
    val name: String,
    val description: String,
    val command: String? = null,
    val url: String? = null,
)
