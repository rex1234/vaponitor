package life.vaporized.servermonitor.app.discord

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import life.vaporized.servermonitor.app.config.EnvConfig
import life.vaporized.servermonitor.app.util.getLogger

class DiscordBot {

    private val logger = getLogger()

    private lateinit var kord: Kord

    private var isReady = false

    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun init() = withContext(Dispatchers.IO) {
        try {
            logger.info("Initializing Discord bot")

            kord = Kord(EnvConfig.discordToken)

            registerIncomingMessageListener(kord)

            // Listen for the Ready event to know when the bot has connected
            kord.on<ReadyEvent> {
                logger.info("Discord bot initialized")
                isReady = true
            }

            kord.login {
                // we need to specify this to receive the content of messages
                @OptIn(PrivilegedIntent::class)
                intents += Intent.MessageContent
            }
        } catch (e: Exception) {
            logger.error("Failed to initialize Discord bot, discord integration will be turned off", e)
        }
    }

    private fun registerIncomingMessageListener(kord: Kord) {
        kord.on<MessageCreateEvent> { // runs every time a message is created that our bot can read

            // ignore other bots, even ourselves. We only serve humans here!
            if (message.author?.isBot != false) return@on

            // check if our command is being invoked
            if (message.content != "!ping") return@on

            // all clear, give them the pong!
            message.channel.createMessage("pong!")
        }
    }

    fun sendMessage(message: String) = scope.launch {
        if (isReady) {
            val channel = kord.getChannelOf<TextChannel>(Snowflake(EnvConfig.discordChannel))
            channel?.createMessage(message)
        }
    }
}
