package life.vaporized.servermonitor.app.cron

import life.vaporized.servermonitor.app.DiscordBot
import life.vaporized.servermonitor.app.Evaluator
import life.vaporized.servermonitor.app.model.MonitorStatus
import kotlin.time.Duration.Companion.minutes

class Jobs(
    private val evaluator: Evaluator,
    private val discordBot: DiscordBot,
) {

    fun start(
        cronJobManager: CronJobManager,
    ) {
        cronJobManager.addJob(10.minutes, ::evaluateMonitors)
    }

    suspend fun evaluateMonitors() {
        val result = evaluator.evaluate()
        result
            .filterIsInstance<MonitorStatus.AppStatus>()
            .filter { it.isError }
            .forEach { app ->
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
