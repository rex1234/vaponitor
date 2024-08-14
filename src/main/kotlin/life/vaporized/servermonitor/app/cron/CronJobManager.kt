package life.vaporized.servermonitor.app.cron

import life.vaporized.servermonitor.app.cron.jobs.EvaluateMonitorsCronJob
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CronJobManager(
    private val evaluateMonitorsJob: EvaluateMonitorsCronJob,
) {

    private val runners = mutableListOf<CronJobRunner>()

    fun init() {
        addJob(10.seconds, evaluateMonitorsJob)
    }

    fun addJob(interval: Duration, cronJob: ICronJob): CronJobRunner {
        val cronJobRunner = CronJobRunner(interval)
        cronJobRunner.start {
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
