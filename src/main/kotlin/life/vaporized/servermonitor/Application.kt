package life.vaporized.servermonitor

import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import life.vaporized.servermonitor.app.DiscordBot
import life.vaporized.servermonitor.app.Evaluator
import life.vaporized.servermonitor.app.cron.CronJobManager
import life.vaporized.servermonitor.app.cron.evaluateMonitors
import life.vaporized.servermonitor.plugins.configureHTTP
import life.vaporized.servermonitor.plugins.configureMonitoring
import life.vaporized.servermonitor.plugins.configureRouting
import life.vaporized.servermonitor.plugins.configureTemplating
import kotlin.time.Duration.Companion.minutes

val evaluator = Evaluator()
val discordBot = DiscordBot()
val cronManager = CronJobManager()

val scope = CoroutineScope(Dispatchers.IO)

fun main(args: Array<String>) {
    EngineMain.main(args)

    cronManager.stopAllJobs()
    cronManager.cancelAllJobs()
}

fun Application.module() {
    configureTemplating()
    configureMonitoring()
    configureHTTP()
    configureRouting(evaluator, discordBot)

    scope.launch {
        discordBot.init()
    }

    cronManager.addJob(10.minutes, ::evaluateMonitors)
}


