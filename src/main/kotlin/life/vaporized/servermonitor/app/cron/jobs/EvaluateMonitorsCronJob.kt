package life.vaporized.servermonitor.app.cron.jobs

import life.vaporized.servermonitor.app.Evaluator
import life.vaporized.servermonitor.app.StatusRepository
import life.vaporized.servermonitor.app.cron.ICronJob
import life.vaporized.servermonitor.app.discord.DiscordMonitorReporter

class EvaluateMonitorsCronJob(
    private val evaluator: Evaluator,
    private val statusRepository: StatusRepository,
    private val discordMonitorReporter: DiscordMonitorReporter,
) : ICronJob {

    override suspend fun run() {
        statusRepository.add(evaluator.evaluate())
        statusRepository.save()

        discordMonitorReporter.sendReport()
    }
}

