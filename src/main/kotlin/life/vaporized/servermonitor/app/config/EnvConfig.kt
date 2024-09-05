package life.vaporized.servermonitor.app.config

import io.github.cdimascio.dotenv.Dotenv

/**
 * .env config wrapper
 */
object EnvConfig {

    private val dotenv = Dotenv.load()

    val discordToken
        get() = dotenv["TOKEN"] ?: throw IllegalArgumentException("TOKEN not found in .env")

    val discordChannel
        get() = dotenv["CHANNEL_ID"] ?: throw IllegalArgumentException("CHANNEL_ID not found in .env")
}