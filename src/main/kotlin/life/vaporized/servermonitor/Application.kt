package life.vaporized.servermonitor

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.netty.EngineMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import life.vaporized.servermonitor.app.StatusRepository
import life.vaporized.servermonitor.app.cron.CronJobManager
import life.vaporized.servermonitor.app.discord.DiscordBot
import life.vaporized.servermonitor.plugins.configureHTTP
import life.vaporized.servermonitor.plugins.configureRouting
import life.vaporized.servermonitor.plugins.configureTemplating
import mainModule
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.koin

val scope = CoroutineScope(Dispatchers.IO)

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    koin {
        // printLogger()
        modules(mainModule)
    }

    configureTemplating()
    configureHTTP()
    configureRouting()

    val discordBot: DiscordBot by inject()
    scope.launch {
        discordBot.init()
    }

    val statusRepository: StatusRepository by inject()
    scope.launch {
        statusRepository.restore()
    }

    val cronManager: CronJobManager by inject()
    cronManager.init()

    environment.monitor.subscribe(ApplicationStopping) {
        cronManager.stopAllJobs()
        cronManager.cancelAllJobs()
    }
}
