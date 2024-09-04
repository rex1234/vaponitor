package life.vaporized.servermonitor.plugins.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import life.vaporized.servermonitor.Config
import life.vaporized.servermonitor.app.StatusRepository
import life.vaporized.servermonitor.app.model.MonitorStatus
import life.vaporized.servermonitor.app.monitor.resources.CpuUsageMonitor
import life.vaporized.servermonitor.app.monitor.resources.DiskUsageMonitor
import life.vaporized.servermonitor.app.monitor.resources.RamUsageMonitor
import java.text.SimpleDateFormat
import java.util.*

fun Routing.indexRoute(
    statusRepository: StatusRepository
) {
    get("/") {
        val lastEval = statusRepository.last
        val lastStatus = lastEval?.list ?: emptyList()
        val resources = statusRepository.getResourceHistory()
        val diskUsage = lastEval?.list
            ?.filterIsInstance<MonitorStatus.ResourceStatus>()
            ?.filter { it.id.startsWith(DiskUsageMonitor.ID) }

        val start = System.currentTimeMillis()
        val timeLine = (0..statusRepository.capacity).map { i ->
            start - i * Config.monitorInterval.inWholeMilliseconds
        }.reversed().takeEveryNth(2)

        call.respond(
            ThymeleafContent(
                template = "index",
                model = mapOf(
                    "time" to SimpleDateFormat.getTimeInstance().format(Date(lastEval?.time ?: 0)),
                    "appStatusList" to lastStatus.filterIsInstance<MonitorStatus.AppStatus>(),
                    "timeline" to timeLine,
                    "ram" to (resources[RamUsageMonitor.ID] ?: emptyList())
                        .map { it.usage }.prefixWithNulls(statusRepository.capacity).takeEveryNth(2),
                    "cpu" to (resources[CpuUsageMonitor.ID] ?: emptyList())
                        .map { it.usage }.prefixWithNulls(statusRepository.capacity).takeEveryNth(2),
                    "volumes" to (diskUsage ?: emptyList()),
                ),
            )
        )
    }
}

fun <T> List<T>.prefixWithNulls(totalCapacity: Int): List<T?> {
    require(totalCapacity >= this.size) { "Total capacity must be greater than or equal to the original list size." }
    val numberOfNulls = totalCapacity - this.size
    return List(numberOfNulls) { null } + this
}

fun <T> List<T>.takeEveryNth(n: Int): List<T> {
    require(n > 0) { "n must be greater than 0." }
    return this.filterIndexed { index, _ -> index % n == n - 1 }
}
