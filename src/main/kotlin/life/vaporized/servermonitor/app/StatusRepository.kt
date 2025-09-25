package life.vaporized.servermonitor.app

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import life.vaporized.servermonitor.app.config.MonitorConfigProvider
import life.vaporized.servermonitor.app.monitor.model.MonitorEvaluation
import life.vaporized.servermonitor.app.util.LimitedSizeDeque
import life.vaporized.servermonitor.app.util.getLogger
import life.vaporized.servermonitor.db.SqliteDb

class StatusRepository(
    private val database: SqliteDb,
    monitorConfig: MonitorConfigProvider,
) {

    private val logger = getLogger()
    private val mutex = Mutex()

    val history: LimitedSizeDeque<MonitorEvaluation> = LimitedSizeDeque(
        (monitorConfig.historyDuration.inWholeSeconds / monitorConfig.appMonitorInterval.inWholeSeconds).toInt()
    )

    val capacity: Int
        get() = history.capacity

    suspend fun last(): MonitorEvaluation? = mutex.withLock {
        history.last
    }

    suspend fun add(evaluation: MonitorEvaluation) = mutex.withLock {
        history.add(evaluation)
        database.insertToDb(evaluation)
    }

    suspend fun restore() = runCatching {
        logger.info("Restoring saved data")

        database.getMeasurementsWithEntries(capacity).forEach { measurement ->
            history.add(measurement)
        }
    }.onFailure {
        logger.error("Failed to restore data", it)
    }

    suspend fun getHistoricalData(startTime: Long, endTime: Long): List<MonitorEvaluation> {
        return database.getMeasurementsWithEntriesInTimeRange(startTime, endTime)
    }
}
