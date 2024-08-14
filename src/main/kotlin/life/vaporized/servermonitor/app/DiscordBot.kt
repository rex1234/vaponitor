package life.vaporized.servermonitor.app

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import life.vaporized.servermonitor.app.util.getLogger

class DiscordBot(
) {
    private val logger = getLogger()

    private val dotenv = Dotenv.load()

    private val TOKEN = dotenv["TOKEN"] ?: throw IllegalArgumentException("TOKEN not found in .env")
    private val CHANNEL_ID = dotenv["CHANNEL_ID"] ?: throw IllegalArgumentException("CHANNEL_ID not found in .env")

    private lateinit var kord: Kord

    private var isReady = false

    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun init() {
        kord = Kord(TOKEN)

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
            val channel = kord.getChannelOf<TextChannel>(Snowflake(CHANNEL_ID))
            channel?.createMessage(message)
        }
    }
}