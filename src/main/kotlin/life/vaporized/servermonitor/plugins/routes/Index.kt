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
import life.vaporized.servermonitor.app.monitor.resources.CpuUsageMonitor
import life.vaporized.servermonitor.app.monitor.resources.Dht22Monitor
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

        val history = statusRepository.history
        val timeline = getTimeLine(monitorConfig.historyDuration)

        val timelineEntries = mapTimelineToEvaluations(timeline, history.elements)

        val ram = timelineEntries.map {
            it?.resourceWithId(RamUsageMonitor.id)
        }
        val cpu = timelineEntries.map {
            it?.resourceWithId(CpuUsageMonitor.id)?.usage
        }
        val temp = timelineEntries.map {
            it?.resourceWithId(RaspberryTempMonitor.id)?.usage
        }
        val dhtTemp = timelineEntries.map {
            it?.resourceWithId(Dht22Monitor.tempId)?.current
        }
        val dhtHum = timelineEntries.map {
            it?.resourceWithId(Dht22Monitor.humidityId)?.usage
        }
        val volumes = lastEval?.resources
            ?.filter { it.id.startsWith(DiskUsageMonitor.id) }
            ?: emptyList()

        val resourcesGraph = GraphData(
            graphName = "Resources",
            xAxis = timeline,
            yAxis = listOf(
                GraphData.YAxisData(
                    name = "RAM",
                    data = ram.map { it?.usage },
                    formattedValues = ram.map { "${it?.current} MB" },
                ),
                GraphData.YAxisData(
                    name = "CPU",
                    data = cpu,
                    formattedValues = cpu.map { "%.2f %%".format(it ?: 0f) },
                ),
            )
        )

        val tempGraph = if (RaspberryTempMonitor.id in enabledResources) {
            GraphData(
                graphName = "Raspberry",
                xAxis = timeline,
                yAxis = listOf(
                    GraphData.YAxisData(
                        name = "CPU Temperature",
                        data = temp,
                        formattedValues = temp.map { "%.2f °C".format(it ?: 0f) },
                    ),
                )
            )
        } else {
            null
        }

        val sensorGraph = if (Dht22Monitor.id in enabledResources) {
            GraphData(
                graphName = "Sensors",
                xAxis = timeline,
                yAxis = listOf(
                    GraphData.YAxisData(
                        name = "Temperature",
                        max = 35,
                        min = 10,
                        data = dhtTemp,
                        formattedValues = dhtTemp.map { "%.2f °C".format(it ?: 0f) },
                    ),
                    GraphData.YAxisData(
                        name = "Humidity",
                        data = dhtHum,
                        formattedValues = dhtHum.map { "%.2f %%".format(it ?: 0f) },
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
                    description = "CPU Temperature",
                    data = it,
                )
            },
            sensorGraph?.let {
                GraphDefinition(
                    title = "Sensors",
                    description = "Temperature and humidity",
                    data = it,
                )
            },
        )

        call.respond(
            ThymeleafContent(
                template = "index",
                model = mapOf(
                    "appName" to EnvConfig.appName,
                    "time" to SimpleDateFormat.getTimeInstance().format(Date(lastEval?.time ?: 0)),
                    "appStatusList" to (lastEval?.apps ?: emptyList()),
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
): List<MonitorEvaluation?> =
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

        if (closestEvaluation == null || time < entries.first().time) {
            null // Explicitly handle case where time is before the first entry
        } else {
            MonitorEvaluation(
                apps = closestEvaluation.apps,
                resources = closestEvaluation.resources,
                time = time
            )
        }
    }

private fun getTimeLine(timelineDuration: Duration): List<Long> {
    val stepSize = timelineDuration.inWholeMilliseconds / TIMELINE_POINTS
    return (0 until TIMELINE_POINTS).map { System.currentTimeMillis() - it * stepSize }
}
