package life.vaporized.servermonitor.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Tables {

    object Measurement : IntIdTable() {
        val createdAt = datetime("date").defaultExpression(CurrentDateTime)
    }

    object ResourceEntry : Table() {
        val resourceId = varchar("id", 255).entityId()
        val usage = float("value")
        val max = float("max")
        val description = varchar("description", 255)
        val measurementIdTable = reference("measurementId", Measurement)
    }

    object AppEntry : Table() {
        val appId = varchar("id", 255).entityId()
        val isAlive = bool("isAlive")
        val isHttpEnabled = bool("isHttpEnabled")
        val isHttpsEnabled = bool("isHttpsEnabled")
        val measurementIdTable = reference("measurementId", Measurement)
    }
}
