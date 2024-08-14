package life.vaporized.servermonitor.app.cron

import life.vaporized.servermonitor.app.cron.jobs.EvaluateMonitorsCronJob
import life.vaporized.servermonitor.app.util.getLogger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CronJobManager(
    private val evaluateMonitorsJob: EvaluateMonitorsCronJob,
) {

    private val logger = getLogger()

    private val runners = mutableListOf<CronJobRunner>()

    fun init() {
        addJob(10.seconds, evaluateMonitorsJob)
    }

    fun addJob(interval: Duration, cronJob: ICronJob): CronJobRunner {
        logger.info("Added job ${cronJob::class.simpleName}")

        val cronJobRunner = CronJobRunner(interval)
        cronJobRunner.start {
            logger.info("Running job ${cronJob::class.simpleName}")
            cronJob.run()
        }
        runners.add(cronJobRunner)
        return cronJobRunner
    }

    fun stopAllJobs() {
        runners.forEach { it.stop() }
    }

    fun cancelAllJobs() {
        runners.forEach { it.cancelScope() }
        runners.clear()
    }
}
