package life.vaporized.servermonitor.db

import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import life.vaporized.servermonitor.app.config.MonitorConfigProvider
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
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class SqliteDb(
    private val monitorConfig: MonitorConfigProvider,
) {

    private val logger = getLogger()

    suspend fun init() = withContext(Dispatchers.IO) {
        logger.info("Initializing SQLite database")

        Class.forName("org.sqlite.JDBC")
        Database.connect("jdbc:sqlite:measurements.db", driver = "org.sqlite.JDBC")

        transaction {
            // Create tables if they don't exist
            SchemaUtils.create(
                Tables.Measurement,
                Tables.ResourceEntry,
                Tables.AppEntry,
            )

            // Create indices (will be created if they don't exist)
            logger.info("Creating database indices for performance optimization...")

            SchemaUtils.createIndex(Tables.Measurement.timestampIndex)

            SchemaUtils.createIndex(Tables.ResourceEntry.measurementIdIndex)
            SchemaUtils.createIndex(Tables.ResourceEntry.resourceIdIndex)
            SchemaUtils.createIndex(Tables.ResourceEntry.compositeIndex)

            SchemaUtils.createIndex(Tables.AppEntry.measurementIdIndex)
            SchemaUtils.createIndex(Tables.AppEntry.appIdIndex)

            logger.info("Database initialization completed with performance indices")
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
            val lastEntries = Tables.AppEntry
                .selectAll()
                .where { Tables.AppEntry.appId eq appStatus.id }
                .orderBy(Tables.AppEntry.id, SortOrder.DESC)
                .limit(2)
                .toList()

            val lastEntriesMapped = lastEntries.map { result ->
                toAppStatus(result).copy(app = appStatus.app)
            }

            if (lastEntries.size == 2 && lastEntriesMapped.all { it == appStatus }) {
                Tables.AppEntry.deleteWhere {
                    Tables.AppEntry.id eq lastEntries[0][Tables.AppEntry.id]
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

    suspend fun getMeasurementsWithEntriesInTimeRange(
        startTime: Long,
        endTime: Long,
        numberOfBuckets: Int = 200
    ): List<MonitorEvaluation> = withContext(Dispatchers.IO) {
        logger.debug("Getting measurements with entries between $startTime and $endTime grouped into $numberOfBuckets buckets")

        transaction {
            val timeRange = endTime - startTime
            val bucketSize = timeRange / numberOfBuckets

            if (bucketSize <= 0) {
                logger.warn("Invalid time range or bucket configuration")
                return@transaction emptyList<MonitorEvaluation>()
            }

            // Calculate sampling rate based on time range duration
            val durationDays = timeRange / (24 * 60 * 60 * 1000) // Convert to days
            val sampleRate = when {
                durationDays <= 1 -> 1      // 1 day: use all data (4320 entries max)
                durationDays <= 7 -> 3      // 1 week: every 3rd entry (~10k entries)
                durationDays <= 30 -> 15    // 1 month: every 15th entry (~8.6k entries)
                durationDays <= 365 -> 100  // 1 year: every 100th entry (~16k entries)
                else -> 200                 // >1 year: every 200th entry
            }

            println("Time range: $durationDays days, using sample rate: $sampleRate")

            // PERFORMANCE FIX: Get sampled resource data in ONE query
            val allResourceData = if (sampleRate == 1) {
                // For short periods, get all data
                Tables.ResourceEntry
                    .innerJoin(Tables.Measurement)
                    .select(
                        Tables.ResourceEntry.resourceId,
                        Tables.ResourceEntry.usage,
                        Tables.ResourceEntry.max,
                        Tables.ResourceEntry.description,
                        Tables.Measurement.timestamp
                    )
                    .where { (Tables.Measurement.timestamp greaterEq startTime) and (Tables.Measurement.timestamp lessEq endTime) }
                    .orderBy(Tables.Measurement.timestamp, SortOrder.ASC)
                    .toList()
            } else {
                // For longer periods, use sampling by getting every Nth measurement ID
                val sampledMeasurementIds = Tables.Measurement
                    .select(Tables.Measurement.id)
                    .where { (Tables.Measurement.timestamp greaterEq startTime) and (Tables.Measurement.timestamp lessEq endTime) }
                    .orderBy(Tables.Measurement.timestamp, SortOrder.ASC)
                    .toList()
                    .filterIndexed { index, _ -> index % sampleRate == 0 } // Take every Nth entry
                    .map { it[Tables.Measurement.id] }

                println("Sampled ${sampledMeasurementIds.size} measurements out of total range")

                // Get resource data only for sampled measurements
                Tables.ResourceEntry
                    .innerJoin(Tables.Measurement)
                    .select(
                        Tables.ResourceEntry.resourceId,
                        Tables.ResourceEntry.usage,
                        Tables.ResourceEntry.max,
                        Tables.ResourceEntry.description,
                        Tables.Measurement.timestamp
                    )
                    .where { Tables.ResourceEntry.measurementIdTable inList sampledMeasurementIds }
                    .orderBy(Tables.Measurement.timestamp, SortOrder.ASC)
                    .toList()
            }

            println("Got ${allResourceData.size} resource entries in one query (sample rate: $sampleRate)")

            // Group all resource data by bucket and resourceId
            val resourceDataByBucket = allResourceData.groupBy { row ->
                val timestamp = row[Tables.Measurement.timestamp]
                val relativeTime = timestamp - startTime
                val bucketIndex = (relativeTime / bucketSize).toInt().coerceIn(0, numberOfBuckets - 1)
                bucketIndex
            }

            // Create results for each bucket
            val results = (0 until numberOfBuckets).map { bucketIndex ->
                val bucketTime = startTime + (bucketIndex * bucketSize) + (bucketSize / 2)

                val resourceEntriesInBucket = resourceDataByBucket[bucketIndex] ?: emptyList()

                if (resourceEntriesInBucket.isEmpty()) {
                    MonitorEvaluation(
                        apps = emptyList(),
                        resources = emptyList(),
                        time = bucketTime
                    )
                } else {
                    // Group by resourceId and calculate averages in Kotlin (fast since data is already fetched)
                    val avgResources = resourceEntriesInBucket
                        .groupBy { it[Tables.ResourceEntry.resourceId] }
                        .map { (resourceId, entries) ->
                            val avgUsage = entries.map { it[Tables.ResourceEntry.usage] }.average().toFloat()
                            val avgMax = entries.map { it[Tables.ResourceEntry.max] }.average().toFloat()
                            val description = entries.firstOrNull()?.get(Tables.ResourceEntry.description) ?: ""

                            MonitorStatus.ResourceStatus(
                                id = resourceId,
                                name = resourceId,
                                description = description,
                                current = avgUsage,
                                total = avgMax,
                            )
                        }

                    MonitorEvaluation(
                        apps = emptyList(),
                        resources = avgResources,
                        time = bucketTime
                    ).also {
                        println("Bucket $bucketIndex at $bucketTime with ${resourceEntriesInBucket.size} resource entries and ${avgResources.size} unique resources")
                    }
                }
            }

            logger.info("Retrieved and averaged ${allResourceData.size} resource entries into $numberOfBuckets buckets (sample rate: $sampleRate)")
            results
        }
    }

    private fun toResourceStatus(result: ResultRow) = MonitorStatus.ResourceStatus(
        id = result[Tables.ResourceEntry.resourceId],
        name = result[Tables.ResourceEntry.resourceId],
        description = result[Tables.ResourceEntry.description] ?: "",
        current = result[Tables.ResourceEntry.usage],
        total = result[Tables.ResourceEntry.max],
    )

    private fun toAppStatus(result: ResultRow) = MonitorStatus.AppStatus(
        app = monitorConfig.appDefinitions.firstOrNull { app -> result[Tables.AppEntry.appId] == "A${app.name}" }
            ?: AppDefinition(result[Tables.AppEntry.appId], result[Tables.AppEntry.description] ?: ""),
        isRunning = result[Tables.AppEntry.isAlive],
        isHttpReachable = result[Tables.AppEntry.isHttpReachable],
        isHttpsReachable = result[Tables.AppEntry.isHttpsReachable],
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

    suspend fun getAppStatusHistory(
        appId: String,
        numberOfEntries: Int = 100,
    ): List<Pair<Long, MonitorStatus.AppStatus>> = withContext(Dispatchers.IO) {
        logger.debug("Getting app change history for $appId with $numberOfEntries entries")

        transaction {
            // Get the actual status changes as stored in the database, ordered by timestamp (newest first)
            val changeEntries = Tables.AppEntry
                .innerJoin(Tables.Measurement)
                .selectAll()
                .where { Tables.AppEntry.appId eq appId }
                .orderBy(Tables.Measurement.timestamp, SortOrder.DESC)
                .limit(numberOfEntries)
                .toList()

            changeEntries.map { row ->
                val timestamp = row[Tables.Measurement.timestamp]
                val appStatus = toAppStatus(row)
                timestamp to appStatus
            }
        }
    }
}
