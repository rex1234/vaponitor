package life.vaporized.servermonitor.app.cron.jobs

import life.vaporized.servermonitor.app.config.MonitorConfigProvider
import life.vaporized.servermonitor.app.cron.ICronJob
import life.vaporized.servermonitor.db.SqliteDb

class CleanDatabaseJob(
    private val monitorConfig: MonitorConfigProvider,
    private val database: SqliteDb,
) : ICronJob {

    override suspend fun run() {
        val resourcePurgeSettings = monitorConfig.resourcePurgeSettings

        if (resourcePurgeSettings.isNotEmpty()) {
            // Use per-resource cleanup
            database.deleteOldMeasurementsPerResource(resourcePurgeSettings)
        }

        if (monitorConfig.dbPurgeDuration != null)
            database.deleteOldMeasurements(duration = monitorConfig.dbPurgeDuration!!)
    }
}