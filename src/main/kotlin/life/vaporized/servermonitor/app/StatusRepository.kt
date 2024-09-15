package life.vaporized.servermonitor.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import life.vaporized.servermonitor.app.config.MonitorConfigProvider
import life.vaporized.servermonitor.app.monitor.model.MonitorEvaluation
import life.vaporized.servermonitor.app.util.LimitedSizeDeque
import life.vaporized.servermonitor.app.util.StatusSerializer
import life.vaporized.servermonitor.app.util.getLogger
import life.vaporized.servermonitor.db.SqliteDb
import java.io.File

class StatusRepository(
    private val monitorConfig: MonitorConfigProvider,
    private val statusSerializer: StatusSerializer,
    private val database: SqliteDb,
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

    suspend fun save() = mutex.withLock {
        withContext(Dispatchers.IO) {
            val jsonData = statusSerializer.serialize(history.elements)
            File("data.json").writeText(jsonData)
            logger.info("Stored current state")
        }
    }

    suspend fun restore() = runCatching {
        logger.info("Restoring saved data")

        withContext(Dispatchers.IO) {
            mutex.withLock {
                val jsonData = File("data.json").readText()
                val data = statusSerializer.deserialize(jsonData)

                data.forEach {
                    history.add(it)
                }

                logger.info("Restore ${data.size} history entries")
            }
        }
    }.onFailure {
        logger.error("Failed to restore data", it)
    }
}
