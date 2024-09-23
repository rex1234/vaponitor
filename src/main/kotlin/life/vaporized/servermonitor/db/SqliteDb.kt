package life.vaporized.servermonitor.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import life.vaporized.servermonitor.app.monitor.model.AppDefinition
import life.vaporized.servermonitor.app.monitor.model.MonitorEvaluation
import life.vaporized.servermonitor.app.monitor.model.MonitorStatus
import life.vaporized.servermonitor.app.util.getLogger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
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
                .orderBy(Tables.Measurement.id, SortOrder.DESC)
                .limit(numberOfMeasurements)
                .toList()
                .reversed()

            val measurementIds = measurements.map { it[Tables.Measurement.id] }
            val apps = Tables.AppEntry
                .selectAll()
                .where { Tables.AppEntry.measurementIdTable inList measurementIds }
                .groupBy { it[Tables.AppEntry.measurementIdTable] }
                .map { (measurementId, entries) ->
                    measurementId to entries.map(::appStatus)
                }
                .toMap()

            val resources = Tables.ResourceEntry
                .selectAll()
                .where { Tables.ResourceEntry.measurementIdTable inList measurementIds }
                .groupBy { it[Tables.ResourceEntry.measurementIdTable] }
                .map { (measurementId, entries) ->
                    measurementId to entries.map(::resourceStatus)
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

    suspend fun getResource(
        resourceId: String,
        startDate: Long,
    ): List<MonitorStatus.ResourceStatus> = withContext(Dispatchers.IO) {
        val measurementIds = Tables.Measurement
            .select(Tables.Measurement.id)
            .where { Tables.Measurement.timestamp greaterEq startDate }
            .orderBy(Tables.Measurement.id, SortOrder.ASC)
            .toList()

        Tables.ResourceEntry
            .selectAll()
            .where {
                (Tables.ResourceEntry.resourceId eq resourceId)
                    .and(Tables.ResourceEntry.measurementIdTable inList measurementIds.map { it[Tables.Measurement.id] })
            }
            .toList()
            .map(::resourceStatus)
    }

    private fun resourceStatus(it: ResultRow) = MonitorStatus.ResourceStatus(
        id = it[Tables.ResourceEntry.resourceId],
        name = it[Tables.ResourceEntry.resourceId],
        description = it[Tables.ResourceEntry.description] ?: "",
        current = it[Tables.ResourceEntry.usage],
        total = it[Tables.ResourceEntry.max],
    )

    private fun appStatus(it: ResultRow) = MonitorStatus.AppStatus(
        app = AppDefinition(it[Tables.AppEntry.appId], it[Tables.AppEntry.description] ?: ""),
        isRunning = it[Tables.AppEntry.isAlive],
        isHttpReachable = it[Tables.AppEntry.isHttpReachable],
        isHttpsReachable = it[Tables.AppEntry.isHttpsReachable],
    )
}
