package life.vaporized.servermonitor.app.cron

import life.vaporized.servermonitor.app.DiscordBot
import life.vaporized.servermonitor.app.Evaluator
import life.vaporized.servermonitor.app.model.MonitorStatus
import kotlin.time.Duration.Companion.seconds

class Jobs(
    private val evaluator: Evaluator,
    private val discordBot: DiscordBot,
) {

    fun start(cronJobManager: CronJobManager) {
        cronJobManager.addJob(10.seconds, ::evaluateMonitors)
    }

    private suspend fun evaluateMonitors() {
        println("Cron: evaluateMonitors")
        val result = evaluator.evaluate()
        result.forEach {
            println(it)
        }
        result
            .filterIsInstance<MonitorStatus.AppStatus>()
            .filter { it.isError }
            .forEach { app ->
                println("Reporting error for $app")
                discordBot.sendMessage(app.errorMessage)
            }
    }

    private val MonitorStatus.AppStatus.errorMessage: String
        get() = buildString {
            appendLine("**Application error: $name**")
            if (!isRunning) {
                appendLine("Process is not running")
            }
            if (isHttpReachable == false) {
                appendLine("HTTPS ping failed")
            }
            if (isHttpsReachable == false) {
                appendLine("HTTP ping failed")
            }
        }
}
