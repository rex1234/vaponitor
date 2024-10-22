package life.vaporized.servermonitor.app.monitor.model

import kotlinx.serialization.Serializable

@Serializable
data class AppDefinition(
    val name: String,
    val description: String,
    val command: String? = null,
    val url: String? = null,
    val https: Boolean = true,
) {

    val link: String
        get() = if (https) "https://${url}" else "http://${url}"
}
