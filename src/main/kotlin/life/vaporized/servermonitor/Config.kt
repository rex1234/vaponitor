package life.vaporized.servermonitor

import io.github.cdimascio.dotenv.Dotenv
import kotlinx.serialization.decodeFromString
import life.vaporized.servermonitor.app.model.AppDefinition
import net.mamoe.yamlkt.Yaml
import java.io.File
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

object Config {

    private val dotenv = Dotenv.load()

    val discordToken
        get() = dotenv["TOKEN"] ?: throw IllegalArgumentException("TOKEN not found in .env")

    val discordChannel
        get() = dotenv["CHANNEL_ID"] ?: throw IllegalArgumentException("CHANNEL_ID not found in .env")

    val monitorInterval
        get() = 30.seconds

    val appMonitorInterval
        get() = 30.seconds

    val historyDuration
        get() = 4.hours

    fun loadAppMonitors(): List<AppDefinition> {
        val yamlData = File("appconfig.yaml").readText()
        val services: List<AppDefinition> = Yaml.Default.decodeFromString(yamlData)
        return services
    }
}