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

fun Routing.appDetailsRoute(
    database: SqliteDb,
) {

    get("/app/{appId}") {
        val appId = call.parameters["appId"] ?: return@get call.respond(
            status = HttpStatusCode.BadRequest,
            message = "App ID is required"
        )

        // Get recent history for statistics (last 500 entries)
        val recentHistory = database.getAppHistory(appId, 500)

        // Get raw change entries for the Recent Events table (last 100 actual changes)
        val rawChangeEntries = database.getAppChangeHistory(appId, 100)

        if (recentHistory.isEmpty()) {
            return@get call.respond(
                status = HttpStatusCode.NotFound,
                message = "App not found or no history available"
            )
        }

        val appStatus = recentHistory.last().second
        val dateFormat = SimpleDateFormat("MMM dd, HH:mm")
        val lastUpdatedTime = recentHistory.last().first // Get timestamp from the most recent entry

        // Calculate uptime statistics from recent history
        val totalEntries = recentHistory.size
        val upEntries =
            recentHistory.count { it.second.isAlive && it.second.isHttpReachable != false && it.second.isHttpsReachable != false }
        val uptimePercentage = if (totalEntries > 0) (upEntries.toDouble() / totalEntries * 100) else 0.0

        // Generate 7-day timeline with 30-minute intervals for availability chart
        val currentTime = System.currentTimeMillis()
        val sevenDaysAgo = currentTime - (7 * 24 * 60 * 60 * 1000L) // 7 days in milliseconds
        val thirtyMinutes = 30 * 60 * 1000L // 30 minutes in milliseconds

        // Get all status changes in the last 7 days
        val sevenDayHistory = database.getAppHistoryInTimeRange(appId, sevenDaysAgo, currentTime)

        // Generate timeline points every 30 minutes for the last 7 days
        val timelinePoints = mutableListOf<Long>()
        var timePoint = sevenDaysAgo
        while (timePoint <= currentTime) {
            timelinePoints.add(timePoint)
            timePoint += thirtyMinutes
        }

        // Fill in the timeline with the correct status at each 30-minute point
        val availabilityHistory = mutableListOf<Map<String, Any>>()
        var statusIndex = 0

        for (point in timelinePoints) {
            // Find the most recent status change before or at this time point
            while (statusIndex < sevenDayHistory.size - 1 &&
                sevenDayHistory[statusIndex + 1].first <= point
            ) {
                statusIndex++
            }

            val status = if (statusIndex < sevenDayHistory.size) {
                sevenDayHistory[statusIndex].second
            } else {
                // If no status available, use the first available status
                if (sevenDayHistory.isNotEmpty()) sevenDayHistory.first().second else appStatus
            }

            availabilityHistory.add(
                mapOf(
                    "timestamp" to point,
                    "time" to dateFormat.format(Date(point)),
                    "isUp" to (status.isAlive && status.isHttpReachable != false && status.isHttpsReachable != false),
                    "isRunning" to status.isRunning,
                    "isHttpReachable" to status.isHttpReachable,
                    "isHttpsReachable" to status.isHttpsReachable
                ) as Map<String, Any>
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
            "history" to availabilityHistory, // 7-day history with 30-minute intervals
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
