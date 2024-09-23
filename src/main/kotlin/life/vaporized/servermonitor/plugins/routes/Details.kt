package life.vaporized.servermonitor.plugins.routes

import io.ktor.server.application.call
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import kotlin.time.Duration.Companion.hours
import life.vaporized.servermonitor.app.StatusRepository
import life.vaporized.servermonitor.app.config.MonitorConfigProvider
import life.vaporized.servermonitor.app.monitor.model.GraphDefinition.GraphData
import life.vaporized.servermonitor.app.monitor.resources.RamUsageMonitor

private const val TIMELINE_POINTS = 1000

fun Routing.historyRoute(
    statusRepository: StatusRepository,
    monitorConfig: MonitorConfigProvider,
) {
    get("/history{resId}/{duration}") {
        val resId = call.parameters["resId"] ?: throw IllegalArgumentException("Resource ID is required")
        val duration =
            call.parameters["duration"]?.toLongOrNull()?.hours ?: throw IllegalArgumentException("Duration is required")

        val lastEval = statusRepository.last()

        val history = statusRepository.history
        val timeline = getTimeLine(duration, TIMELINE_POINTS)

        val timelineEntries = mapTimelineToEvaluations(timeline, history.elements)

        val ram = timelineEntries.map {
            it?.resourceWithId(RamUsageMonitor.id)
        }

        val graph = GraphData(
            graphName = "Resources",
            xAxis = timeline,
            yAxis = listOf(
                GraphData.YAxisData(
                    name = "ree",
                    data = ram.map { it?.usage },
                    formattedValues = ram.map { "${it?.current} MB" },
                ),
            )
        )
    }
}
