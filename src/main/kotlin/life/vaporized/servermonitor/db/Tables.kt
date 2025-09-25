package life.vaporized.servermonitor.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Index

object Tables {

    object Measurement : IntIdTable() {
        val timestamp = long("timestamp")

        val timestampIndex = Index(listOf(timestamp), unique = false, "idx_measurement_timestamp")
    }

    object ResourceEntry : IntIdTable() {
        val resourceId = varchar("resourceId", 255)
        val usage = float("value")
        val max = float("max")
        val description = varchar("description", 255).nullable()
        val measurementIdTable = reference("measurementId", Measurement)

        val measurementIdIndex = Index(listOf(measurementIdTable), unique = false, "idx_resource_measurement_id")
        val resourceIdIndex = Index(listOf(resourceId), unique = false, "idx_resource_resource_id")
        val compositeIndex = Index(listOf(measurementIdTable, resourceId), unique = false, "idx_resource_composite")
    }

    object AppEntry : IntIdTable() {
        val appId = varchar("appId", 255)
        val description = varchar("description", 255).nullable()
        val isAlive = bool("isAlive")
        val isHttpReachable = bool("isHttpReachable").nullable()
        val isHttpsReachable = bool("isHttpsReachable").nullable()
        val measurementIdTable = reference("measurementId", Measurement)

        val measurementIdIndex = Index(listOf(measurementIdTable), unique = false, "idx_app_measurement_id")
        val appIdIndex = Index(listOf(appId), unique = false, "idx_app_app_id")
    }
}
