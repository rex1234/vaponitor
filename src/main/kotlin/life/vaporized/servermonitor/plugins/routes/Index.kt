package life.vaporized.servermonitor.plugins.routes

import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.thymeleaf.ThymeleafContent
import kotlin.time.Duration
import life.vaporized.servermonitor.app.StatusRepository
import life.vaporized.servermonitor.app.config.EnvConfig
import life.vaporized.servermonitor.app.config.MonitorConfigProvider
import life.vaporized.servermonitor.app.monitor.model.GraphDefinition
import life.vaporized.servermonitor.app.monitor.model.GraphDefinition.GraphData
import life.vaporized.servermonitor.app.monitor.model.MonitorEvaluation
import life.vaporized.servermonitor.app.monitor.model.MonitorStatus
import life.vaporized.servermonitor.app.monitor.resources.CpuUsageMonitor
import life.vaporized.servermonitor.app.monitor.resources.DiskUsageMonitor
import life.vaporized.servermonitor.app.monitor.resources.RamUsageMonitor
import life.vaporized.servermonitor.app.monitor.resources.RaspberryTempMonitor
import java.text.SimpleDateFormat
import java.util.*

const val TIMELINE_POINTS = 200

fun Routing.indexRoute(
    statusRepository: StatusRepository,
    monitorConfig: MonitorConfigProvider,
) {
    get("/") {
        val enabledResources = monitorConfig.enabledResourceMonitors.map { it.id }

        val lastEval = statusRepository.last()
        val lastStatus = lastEval?.list ?: emptyList()

        val history = statusRepository.history
        val timeline = getTimeLine(monitorConfig.historyDuration)

        val timelineEntries = mapTimelineToEvaluations(timeline, history.elements)

        val ram = timelineEntries.map {
            (it.list.firstOrNull { it.id == RamUsageMonitor.id } as? MonitorStatus.ResourceStatus)?.usage
        }
        val cpu = timelineEntries.map {
            (it.list.firstOrNull { it.id == CpuUsageMonitor.id } as? MonitorStatus.ResourceStatus)?.usage
        }
        val temp = timelineEntries.map {
            (it.list.firstOrNull { it.id == RaspberryTempMonitor.id } as? MonitorStatus.ResourceStatus)?.usage
        }
        val volumes = lastEval?.list
            ?.filterIsInstance<MonitorStatus.ResourceStatus>()
            ?.filter { it.id.startsWith(DiskUsageMonitor.id) }
            ?: emptyList()

        val resourcesGraph = GraphData(
            graphName = "Resources",
            xAxis = timeline,
            yAxis = listOf(
                GraphData.YAxisData(
                    name = "RAM",
                    data = ram,
                    formattedValues = ram.map { "$it %" },
                ),
                GraphData.YAxisData(
                    name = "CPU",
                    data = cpu,
                    formattedValues = ram.map { "$it %" },
                ),
            )
        )

        val tempGraph = if (RaspberryTempMonitor.id in enabledResources) {
            GraphData(
                graphName = "Raspberry",
                xAxis = timeline,
                yAxis = listOf(
                    GraphData.YAxisData(
                        name = "Temperature",
                        data = temp,
                        formattedValues = temp.map { "$it °C" },
                    ),
                )
            )
        } else {
            null
        }

        val graphs = listOfNotNull(
            GraphDefinition(
                title = "Resources",
                description = "RAM and CPU usage",
                data = resourcesGraph,
            ),
            tempGraph?.let {
                GraphDefinition(
                    title = "Raspberry",
                    description = "Temperature",
                    data = it,
                )
            }
        )

        call.respond(
            ThymeleafContent(
                template = "index",
                model = mapOf(
                    "appName" to EnvConfig.appName,
                    "time" to SimpleDateFormat.getTimeInstance().format(Date(lastEval?.time ?: 0)),
                    "appStatusList" to lastStatus.filterIsInstance<MonitorStatus.AppStatus>(),
                    "timeline" to timeline,
                    "graphs" to graphs,
                    "volumes" to volumes,
                ),
            )
        )
    }
}

fun mapTimelineToEvaluations(
    timeline: List<Long>,
    entries: List<MonitorEvaluation>,
): List<MonitorEvaluation> =
    timeline.map { time ->
        val index = entries.binarySearch { it.time.compareTo(time) }
        val closestEvaluation = if (index >= 0) {
            entries[index]
        } else {
            val insertionPoint = -index - 1
            when {
                insertionPoint == 0 -> null // Time is before the first entry
                else -> {
                    listOfNotNull(
                        entries.getOrNull(insertionPoint - 1),
                        entries.getOrNull(insertionPoint)
                    ).minByOrNull { kotlin.math.abs(it.time - time) }
                }
            }
        }
        MonitorEvaluation(
            list = closestEvaluation?.list ?: emptyList(),
            time = time
        )
    }

private fun getTimeLine(timelineDuration: Duration): List<Long> {
    val stepSize = timelineDuration.inWholeMilliseconds / TIMELINE_POINTS
    return (0 until TIMELINE_POINTS).map { System.currentTimeMillis() - it * stepSize }
}

fun <T> List<T>.prefixWithNulls(totalCapacity: Int): List<T?> {
    require(totalCapacity >= this.size) { "Total capacity must be greater than or equal to the original list size." }
    val numberOfNulls = totalCapacity - this.size
    return List(numberOfNulls) { null } + this
}
