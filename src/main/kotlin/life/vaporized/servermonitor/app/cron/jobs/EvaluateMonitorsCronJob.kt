package life.vaporized.servermonitor.app.cron.jobs

import life.vaporized.servermonitor.app.DiscordBot
import life.vaporized.servermonitor.app.Evaluator
import life.vaporized.servermonitor.app.StatusHolder
import life.vaporized.servermonitor.app.cron.ICronJob
import life.vaporized.servermonitor.app.model.MonitorEvaluation
import life.vaporized.servermonitor.app.model.MonitorStatus
import life.vaporized.servermonitor.app.util.getLogger

class EvaluateMonitorsCronJob(
    private val evaluator: Evaluator,
    private val discordBot: DiscordBot,
    private val statusHolder: StatusHolder,
) : ICronJob {

    private val logger = getLogger()

    override suspend fun run() {
        val result = evaluator.evaluate()
        reportApps(result)
        statusHolder.add(result)
        statusHolder.save()
    }

    private fun reportApps(result: MonitorEvaluation) {
        val appStatuses = result
            .list
            .filterIsInstance<MonitorStatus.AppStatus>()

        val lastAppStatuses = statusHolder.last?.list?.filterIsInstance<MonitorStatus.AppStatus>() ?: emptyList()

        val shouldReportApp = lastAppStatuses != appStatuses && appStatuses.any {
            it.isError
        }

        if (shouldReportApp) {
            appStatuses
                .filter { it.isError }
                .forEach { app ->
                    logger.info("Reporting error for $app on discord")
                    discordBot.sendMessage(app.errorMessage)
                }
        }
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
