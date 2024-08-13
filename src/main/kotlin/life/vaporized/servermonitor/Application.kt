package life.vaporized.servermonitor

import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import life.vaporized.servermonitor.app.DiscordBot
import life.vaporized.servermonitor.app.cron.CronJobManager
import life.vaporized.servermonitor.app.cron.Jobs
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
        //printLogger()
        modules(mainModule)
    }

    configureTemplating()
    configureHTTP()
    configureRouting()

    val discordBot: DiscordBot by inject()
    scope.launch {
        discordBot.init()
    }

    val cronManager: CronJobManager by inject()
    val jobs: Jobs by inject()

    jobs.init(cronManager)

    environment.monitor.subscribe(ApplicationStopping) {
        cronManager.stopAllJobs()
        cronManager.cancelAllJobs()
    }
}


