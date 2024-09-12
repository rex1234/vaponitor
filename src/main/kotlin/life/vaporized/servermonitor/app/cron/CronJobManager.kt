package life.vaporized.servermonitor.app.cron

import kotlin.time.Duration
import life.vaporized.servermonitor.app.config.MonitorConfigProvider
import life.vaporized.servermonitor.app.cron.jobs.EvaluateMonitorsCronJob
import life.vaporized.servermonitor.app.util.getLogger

class CronJobManager(
    private val evaluateMonitorsJob: EvaluateMonitorsCronJob,
    private val monitorConfig: MonitorConfigProvider,
) {

    private val logger = getLogger()

    private val runners = mutableListOf<CronJobRunner>()

    fun init() {
        logger.info("Initializing cron jobs")
        addJob(monitorConfig.appMonitorInterval, evaluateMonitorsJob)
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
