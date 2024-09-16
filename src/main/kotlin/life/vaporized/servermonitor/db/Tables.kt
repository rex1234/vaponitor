package life.vaporized.servermonitor.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime

object Tables {

    object Measurement : IntIdTable() {
        val date = datetime("date").defaultExpression(CurrentDateTime)
    }

    object ResourceEntry : IntIdTable() {
        val resourceId = varchar("resourceId", 255)
        val usage = float("value")
        val max = float("max")
        val description = varchar("description", 255).nullable()
        val measurementIdTable = reference("measurementId", Measurement)
    }

    object AppEntry : IntIdTable() {
        val appId = varchar("appId", 255)
        val description = varchar("description", 255).nullable()
        val isAlive = bool("isAlive")
        val isHttpReachable = bool("isHttpReachable").nullable()
        val isHttpsReachable = bool("isHttpsReachable").nullable()
        val measurementIdTable = reference("measurementId", Measurement)
    }
}
