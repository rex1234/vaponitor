package life.vaporized.servermonitor.app.cron

interface ICronJob {
    suspend fun run()
}