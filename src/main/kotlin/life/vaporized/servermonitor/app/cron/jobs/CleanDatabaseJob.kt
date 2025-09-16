package life.vaporized.servermonitor.app.cron.jobs

import life.vaporized.servermonitor.app.config.MonitorConfigProvider
import life.vaporized.servermonitor.app.cron.ICronJob
import life.vaporized.servermonitor.db.SqliteDb

class CleanDatabaseJob(
    private val monitorConfig: MonitorConfigProvider,
    private val database: SqliteDb,
) : ICronJob {

    override suspend fun run() {
        database.deleteOldMeasurements(duration = monitorConfig.dbPurgeDuration)
    }
}