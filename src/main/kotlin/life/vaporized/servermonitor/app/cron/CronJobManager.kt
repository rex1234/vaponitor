package life.vaporized.servermonitor.app.cron

import kotlin.time.Duration

class CronJobManager {

    private val jobs = mutableListOf<CronJob>()

    fun addJob(interval: Duration, action: suspend () -> Unit): CronJob {
        val cronJob = CronJob(interval)
        cronJob.start(action)
        jobs.add(cronJob)
        return cronJob
    }

    fun stopAllJobs() {
        jobs.forEach { it.stop() }
    }

    fun cancelAllJobs() {
        jobs.forEach { it.cancelScope() }
        jobs.clear()
    }
}
