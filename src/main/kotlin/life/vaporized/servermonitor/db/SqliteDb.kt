package life.vaporized.servermonitor.db

import kotlin.time.Duration
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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class SqliteDb {

    private val logger = getLogger()

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

            evaluation.apps.forEach { insertAppStatus(measurementId, it, shouldDeleteLast = true) }
            evaluation.resources.forEach { insertResourceStatus(measurementId, it) }
        }
    }

    /**
     * param shouldDeleteLast: if true, the last entry for the resource will be deleted if it is the same
     * -> only changes will be stored
     */
    private fun insertAppStatus(
        measurementId: Int,
        appStatus: MonitorStatus.AppStatus,
        shouldDeleteLast: Boolean,
    ) {
        if (shouldDeleteLast) {
            val lastEntry = Tables.AppEntry
                .selectAll()
                .where { Tables.AppEntry.appId eq appStatus.id }
                .orderBy(Tables.AppEntry.id, SortOrder.DESC)
                .limit(1)
                .lastOrNull()

            val count = Tables.AppEntry
                .select(Tables.AppEntry.appId)
                .where { Tables.AppEntry.appId eq appStatus.id }
                .count()

            val lastAppStatus = lastEntry?.let(::toAppStatus)
            if (lastEntry != null && lastAppStatus?.copy(app = appStatus.app) == appStatus && count > 1) {
                Tables.AppEntry.deleteWhere {
                    Tables.AppEntry.id eq lastEntry[Tables.AppEntry.id]
                }
            }
        }

        Tables.AppEntry.insert {
            it[appId] = appStatus.id
            it[isAlive] = appStatus.isAlive
            it[isHttpReachable] = appStatus.isHttpReachable
            it[isHttpsReachable] = appStatus.isHttpsReachable
            it[measurementIdTable] = measurementId
        }
    }

    private fun insertResourceStatus(
        measurementId: Int,
        resourceStatus: MonitorStatus.ResourceStatus,
    ) {
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
                    measurementId to entries.map(::toAppStatus)
                }
                .toMap()

            val resources = Tables.ResourceEntry
                .selectAll()
                .where { Tables.ResourceEntry.measurementIdTable inList measurementIds }
                .groupBy { it[Tables.ResourceEntry.measurementIdTable] }
                .map { (measurementId, entries) ->
                    measurementId to entries.map(::toResourceStatus)
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

    private fun toResourceStatus(it: ResultRow) = MonitorStatus.ResourceStatus(
        id = it[Tables.ResourceEntry.resourceId],
        name = it[Tables.ResourceEntry.resourceId],
        description = it[Tables.ResourceEntry.description] ?: "",
        current = it[Tables.ResourceEntry.usage],
        total = it[Tables.ResourceEntry.max],
    )

    private fun toAppStatus(it: ResultRow) = MonitorStatus.AppStatus(
        app = AppDefinition(it[Tables.AppEntry.appId], it[Tables.AppEntry.description] ?: ""),
        isRunning = it[Tables.AppEntry.isAlive],
        isHttpReachable = it[Tables.AppEntry.isHttpReachable],
        isHttpsReachable = it[Tables.AppEntry.isHttpsReachable],
    )

    suspend fun deleteOldMeasurements(duration: Duration) = withContext(Dispatchers.IO) {
        transaction {
            val threshold = System.currentTimeMillis() - duration.inWholeMilliseconds

            // Delete from AppEntry and ResourceEntry where measurementId is in the list of old measurementIds
            val oldMeasurementIds = Tables.Measurement
                .selectAll()
                .where { Tables.Measurement.timestamp less threshold }
                .orderBy(Tables.Measurement.id, SortOrder.ASC)
                .map { it[Tables.Measurement.id] }

            if (oldMeasurementIds.isNotEmpty()) {
                Tables.AppEntry.deleteWhere {
                    Tables.AppEntry.measurementIdTable inList oldMeasurementIds
                }
                Tables.ResourceEntry.deleteWhere {
                    Tables.ResourceEntry.measurementIdTable inList oldMeasurementIds
                }
                Tables.Measurement.deleteWhere {
                    Tables.Measurement.timestamp less threshold
                }
            }
        }
    }
}
