package life.vaporized.servermonitor.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Index

object Tables {

    object Measurement : IntIdTable() {
        val timestamp = long("timestamp")

        // Index for timestamp-based queries (time range filtering)
        val timestampIndex = Index(listOf(timestamp), unique = false, "idx_measurement_timestamp")
    }

    object ResourceEntry : IntIdTable() {
        val resourceId = varchar("resourceId", 255)
        val usage = float("value")
        val max = float("max")
        val description = varchar("description", 255).nullable()
        val measurementIdTable = reference("measurementId", Measurement)

        // Index for foreign key lookups (most important for performance)
        val measurementIdIndex = Index(listOf(measurementIdTable), unique = false, "idx_resource_measurement_id")

        // Index for resource grouping in SQL GROUP BY operations
        val resourceIdIndex = Index(listOf(resourceId), unique = false, "idx_resource_resource_id")

        // Composite index for the most common query pattern (measurement + resource grouping)
        val compositeIndex = Index(listOf(measurementIdTable, resourceId), unique = false, "idx_resource_composite")
    }

    object AppEntry : IntIdTable() {
        val appId = varchar("appId", 255)
        val description = varchar("description", 255).nullable()
        val isAlive = bool("isAlive")
        val isHttpReachable = bool("isHttpReachable").nullable()
        val isHttpsReachable = bool("isHttpsReachable").nullable()
        val measurementIdTable = reference("measurementId", Measurement)

        // Index for foreign key lookups
        val measurementIdIndex = Index(listOf(measurementIdTable), unique = false, "idx_app_measurement_id")

        // Index for app-specific queries
        val appIdIndex = Index(listOf(appId), unique = false, "idx_app_app_id")
    }
}
