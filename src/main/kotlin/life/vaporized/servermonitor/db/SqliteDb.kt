package life.vaporized.servermonitor.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import life.vaporized.servermonitor.app.monitor.model.AppDefinition
import life.vaporized.servermonitor.app.monitor.model.MonitorEvaluation
import life.vaporized.servermonitor.app.monitor.model.MonitorStatus
import life.vaporized.servermonitor.app.util.getLogger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.ZoneOffset

class SqliteDb {

    val logger = getLogger()

    suspend fun init() = withContext(Dispatchers.IO) {
        logger.info("Initializing SQLite database")

        Class.forName("org.sqlite.JDBC")
        Database.connect("jdbc:sqlite:measurements.db", driver = "org.sqlite.JDBC")

        transaction {
            SchemaUtils.create(
                Tables.Measurement,
                Tables.ResourceEntry,
                Tables.AppEntry,
            )
        }
    }

    suspend fun insertToDb(evaluation: MonitorEvaluation) = withContext(Dispatchers.IO) {
        transaction {
            val measurementId = Tables.Measurement.insertAndGetId {}.value

            evaluation.list.forEach {
                when (it) {
                    is MonitorStatus.AppStatus -> insertAppStatus(measurementId, it)
                    is MonitorStatus.ResourceStatus -> insertResourceStatus(measurementId, it)
                }
            }
        }
    }

    private fun insertAppStatus(measurementId: Int, appStatus: MonitorStatus.AppStatus) {
        Tables.AppEntry.insert {
            it[appId] = appStatus.id
            it[isAlive] = appStatus.isAlive
            it[isHttpReachable] = appStatus.isHttpReachable
            it[isHttpsReachable] = appStatus.isHttpsReachable
            it[measurementIdTable] = measurementId
        }
    }

    private fun insertResourceStatus(measurementId: Int, resourceStatus: MonitorStatus.ResourceStatus) {
        Tables.ResourceEntry.insert {
            it[resourceId] = resourceStatus.id
            it[usage] = resourceStatus.current
            it[max] = resourceStatus.total
            it[description] = resourceStatus.description
            it[measurementIdTable] = measurementId
        }
    }

    fun getMeasurementsWithEntries(): List<MonitorEvaluation> {
        return transaction {
            // Perform a LEFT JOIN on Measurement, AppEntry, and ResourceEntry
            (Tables.Measurement leftJoin Tables.AppEntry leftJoin Tables.ResourceEntry)
                .selectAll()
                .groupBy { it[Tables.Measurement.id] }
                .map { (measurementId, rows) ->
                    val appEntries = rows.filter { it[Tables.AppEntry.appId] != null }.map { row ->
                        MonitorStatus.AppStatus(
                            app = AppDefinition(
                                name = row[Tables.AppEntry.appId],
                                description = "",
                            ),
                            isRunning = row[Tables.AppEntry.isAlive],
                            isHttpReachable = row[Tables.AppEntry.isHttpReachable],
                            isHttpsReachable = row[Tables.AppEntry.isHttpsReachable],
                        )
                    }

                    val resourceEntries = rows.filter { it[Tables.ResourceEntry.resourceId] != null }.map { row ->
                        MonitorStatus.ResourceStatus(
                            id = row[Tables.ResourceEntry.resourceId],
                            name = row[Tables.ResourceEntry.resourceId],
                            description = row[Tables.ResourceEntry.description] ?: "",
                            current = row[Tables.ResourceEntry.usage],
                            total = row[Tables.ResourceEntry.max],
                        )
                    }

                    // Group data by Measurement ID and collect appEntries and resourceEntries
                    MonitorEvaluation(
                        time = rows.first()[Tables.Measurement.createdAt].toInstant(ZoneOffset.UTC).toEpochMilli(),
                        list = resourceEntries + appEntries,
                    )
                }
        }
    }
}
