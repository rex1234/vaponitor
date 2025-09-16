package life.vaporized.servermonitor.plugins.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.thymeleaf.ThymeleafContent
import life.vaporized.servermonitor.app.config.EnvConfig
import life.vaporized.servermonitor.db.SqliteDb
import java.text.SimpleDateFormat
import java.util.*

fun Routing.appDetailsRoute(database: SqliteDb) {
    get("/app/{appId}") {
        val appId = call.parameters["appId"] ?: return@get call.respond(
            status = HttpStatusCode.BadRequest,
            message = "App ID is required"
        )

        val history = database.getAppHistory(appId, 500)

        // Get raw change entries for the Recent Events table (last 100 actual changes)
        val rawChangeEntries = database.getAppChangeHistory(appId, 100)

        if (history.isEmpty()) {
            return@get call.respond(
                status = HttpStatusCode.NotFound,
                message = "App not found or no history available"
            )
        }

        val appStatus = history.last().second
        val dateFormat = SimpleDateFormat("MMM dd, HH:mm")
        val lastUpdatedTime = history.last().first // Get timestamp from the most recent entry

        // Calculate uptime statistics
        val totalEntries = history.size
        val upEntries =
            history.count { it.second.isAlive && it.second.isHttpReachable != false && it.second.isHttpsReachable != false }
        val uptimePercentage = if (totalEntries > 0) (upEntries.toDouble() / totalEntries * 100) else 0.0

        // Prepare timeline data for chart
        val timelineData = history.map { (timestamp, status) ->
            mapOf(
                "timestamp" to timestamp,
                "time" to dateFormat.format(Date(timestamp)),
                "isUp" to (status.isAlive && status.isHttpReachable != false && status.isHttpsReachable != false),
                "isRunning" to status.isRunning,
                "isHttpReachable" to status.isHttpReachable,
                "isHttpsReachable" to status.isHttpsReachable
            )
        }

        // Calculate status indicators for template
        val isCurrentlyUp =
            appStatus.isAlive && appStatus.isHttpReachable != false && appStatus.isHttpsReachable != false
        val statusIndicatorClass = if (isCurrentlyUp) "status-indicator status-up" else "status-indicator status-down"
        val statusBadge = if (isCurrentlyUp) "Online" else "Offline"
        val statusBadgeClass = if (isCurrentlyUp) "badge bg-success" else "badge bg-danger"

        val templateModel: Map<String, Any> = mapOf(
            "appName" to EnvConfig.appName,
            "app" to appStatus.app,
            "currentStatus" to appStatus,
            "history" to timelineData,
            "rawHistory" to rawChangeEntries.map { (timestamp, status) ->
                mapOf(
                    "timestamp" to timestamp,
                    "time" to dateFormat.format(Date(timestamp)),
                    "isUp" to (status.isAlive && status.isHttpReachable != false && status.isHttpsReachable != false),
                    "isRunning" to status.isRunning,
                    "isHttpReachable" to status.isHttpReachable,
                    "isHttpsReachable" to status.isHttpsReachable
                )
            },
            "uptimePercentage" to "%.2f".format(uptimePercentage),
            "totalEntries" to totalEntries,
            "upEntries" to upEntries,
            "lastUpdated" to SimpleDateFormat.getTimeInstance().format(Date(lastUpdatedTime)),
            "isCurrentlyUp" to isCurrentlyUp,
            "statusIndicatorClass" to statusIndicatorClass,
            "statusBadge" to statusBadge,
            "statusBadgeClass" to statusBadgeClass
        )

        println(templateModel)

        call.respond(
            ThymeleafContent(
                template = "app-details",
                model = templateModel
            )
        )
    }
}
