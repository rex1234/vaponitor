package life.vaporized.servermonitor.plugins.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.thymeleaf.ThymeleafContent
import life.vaporized.servermonitor.app.StatusRepository
import life.vaporized.servermonitor.app.config.EnvConfig
import life.vaporized.servermonitor.app.monitor.model.MonitorEvaluation
import life.vaporized.servermonitor.db.SqliteDb
import java.text.SimpleDateFormat
import java.util.*

val dateFormat = SimpleDateFormat("MMM dd, HH:mm")

fun Route.appDetailsRoute(
    statusRepository: StatusRepository,
    database: SqliteDb,
) {

    get("/app/{appId}") {
        val appId = call.parameters["appId"] ?: return@get call.respond(
            status = HttpStatusCode.BadRequest,
            message = "App ID is required"
        )

        val recentHistory = statusRepository.history.elements.filter {
            it.appWithId(appId) != null
        }

        if (recentHistory.isEmpty()) {
            return@get call.respond(
                status = HttpStatusCode.NotFound,
                message = "App not found or no history available"
            )
        }

        // Get raw change entries for the Recent Events table (last 100 actual changes)
        val rawChangeEntries = database.getAppStatusHistory(appId, 100)

        val lastEntry = recentHistory.last()
        val appStatus = lastEntry.appWithId(appId) ?: return@get call.respond(
            status = HttpStatusCode.BadRequest,
            message = "App was not evaluated"
        )

        val lastUpdatedTime = Date(lastEntry.time) // Get timestamp from the most recent entry

        // Calculate uptime statistics from recent history
        val totalEntries = recentHistory.size
        val upEntries = recentHistory.count { it.appWithId(appId)?.isError == false }
        val uptimePercentage = if (totalEntries > 0) (upEntries.toDouble() / totalEntries * 100) else 0.0

        val availabilityHistory = generateTimeline(
            appId = appId,
            evaluationHistory = recentHistory,
        )

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
            "lastUpdated" to SimpleDateFormat.getTimeInstance().format(lastUpdatedTime),
            "isCurrentlyUp" to !appStatus.isError,
            "statusBadge" to if (appStatus.isError) "Offline" else "Online",
        )

        call.respond(
            ThymeleafContent(
                template = "app-details",
                model = templateModel
            )
        )
    }
}

private fun generateTimeline(
    appId: String,
    evaluationHistory: List<MonitorEvaluation>
): List<Map<String, Any?>> =
    evaluationHistory.mapNotNull { historyEntry ->
        val status = historyEntry.appWithId(appId) ?: return@mapNotNull null

        mapOf(
            "timestamp" to historyEntry.time,
            "time" to dateFormat.format(Date(historyEntry.time)),
            "isUp" to !status.isError,
            "isRunning" to status.isRunning,
            "isHttpReachable" to status.isHttpReachable,
            "isHttpsReachable" to status.isHttpsReachable
        )
    }
