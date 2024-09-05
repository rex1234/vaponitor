package life.vaporized.servermonitor.app.config

import io.github.cdimascio.dotenv.Dotenv

/**
 * .env config wrapper
 */
object EnvConfig {

    private val dotenv = Dotenv.load()

    val appName
        get() = dotenv["APP_NAME"] ?: throw IllegalArgumentException("APP_NAME not found in .env")

    val discordToken
        get() = dotenv["DISCORD_TOKEN"] ?: throw IllegalArgumentException("DISCORD_TOKEN not found in .env")

    val discordChannel
        get() = dotenv["DISCORD_CHANNEL_ID"] ?: throw IllegalArgumentException("DISCORD_CHANNEL_ID not found in .env")
}