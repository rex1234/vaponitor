package life.vaporized.servermonitor.app.monitor.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppDefinition(
    val name: String,
    val description: String,
    val command: String? = null,
    @SerialName("url")
    private val _httpUrl: String? = null,
    @SerialName("https")
    private val _httpsUrl: String? = null,
    val basicAuthUsername: String? = null,
    val basicAuthPassword: String? = null,
) {

    val httpUrl: String?
        get() = _httpUrl?.let { "http://$it" }

    val httpsUrl: String?
        get() = _httpsUrl?.let { "https://$it" }

    val link: String?
        get() = httpsUrl ?: httpUrl
}
