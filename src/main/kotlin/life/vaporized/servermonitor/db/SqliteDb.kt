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
            val measurementId = Tables.Measurement.insertAndGetId {
                it[timestamp] = System.currentTimeMillis()
            }.value

            evaluation.apps.forEach { insertAppStatus(measurementId, it) }
            evaluation.resources.forEach { insertResourceStatus(measurementId, it) }
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

    suspend fun getMeasurementsWithEntries(
        numberOfMeasurements: Int,
    ): List<MonitorEvaluation> = withContext(Dispatchers.IO) {
        logger.debug("Getting $numberOfMeasurements measurements with entries")

        transaction {
            val measurements = Tables.Measurement
                .selectAll()
                .limit(numberOfMeasurements)
                .sortedBy { it[Tables.Measurement.id] }

            val measurementIds = measurements.map { it[Tables.Measurement.id] }
            val apps = Tables.AppEntry
                .selectAll()
                .filter { it[Tables.AppEntry.measurementIdTable] in measurementIds }
                .groupBy { it[Tables.AppEntry.measurementIdTable] }
                .map { (measurementId, entries) ->
                    measurementId to entries.map {
                        MonitorStatus.AppStatus(
                            app = AppDefinition(it[Tables.AppEntry.appId], it[Tables.AppEntry.description] ?: ""),
                            isRunning = it[Tables.AppEntry.isAlive],
                            isHttpReachable = it[Tables.AppEntry.isHttpReachable],
                            isHttpsReachable = it[Tables.AppEntry.isHttpsReachable],
                        )
                    }
                }
                .toMap()

            val resources = Tables.ResourceEntry
                .selectAll()
                .filter { it[Tables.ResourceEntry.measurementIdTable] in measurementIds }
                .groupBy { it[Tables.ResourceEntry.measurementIdTable] }
                .map { (measurementId, entries) ->
                    measurementId to entries.map {
                        MonitorStatus.ResourceStatus(
                            id = it[Tables.ResourceEntry.resourceId],
                            name = it[Tables.ResourceEntry.resourceId],
                            description = it[Tables.ResourceEntry.description] ?: "",
                            current = it[Tables.ResourceEntry.usage],
                            total = it[Tables.ResourceEntry.max],
                        )
                    }
                }
                .toMap()

            logger.info("Retrieved ${measurements.count()} measurements with entries")

            measurements.map { measurement ->
                MonitorEvaluation(
                    apps = apps[measurement[Tables.Measurement.id]] ?: emptyList(),
                    resources = resources[measurement[Tables.Measurement.id]] ?: emptyList(),
                    time = measurement[Tables.Measurement.timestamp],
                )
            }
        }
    }
}
