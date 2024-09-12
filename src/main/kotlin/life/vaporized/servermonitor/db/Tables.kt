package life.vaporized.servermonitor.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Tables {

    object Measurement : IntIdTable() {
        val createdAt = datetime("date").defaultExpression(CurrentDateTime)
    }

    object ResourceEntry : IntIdTable() {
        val resourceId = varchar("resourceId", 255)
        val usage = float("value")
        val max = float("max")
        val description = varchar("description", 255)
        val measurementIdTable = reference("measurementId", Measurement)
    }

    object AppEntry : IntIdTable() {
        val appId = varchar("appId", 255)
        val isAlive = bool("isAlive")
        val isHttpEnabled = bool("isHttpEnabled")
        val isHttpsEnabled = bool("isHttpsEnabled")
        val measurementIdTable = reference("measurementId", Measurement)
    }
}
