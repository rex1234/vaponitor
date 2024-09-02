package life.vaporized.servermonitor.plugins.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import life.vaporized.servermonitor.Config
import life.vaporized.servermonitor.app.StatusHolder
import life.vaporized.servermonitor.app.model.MonitorStatus
import life.vaporized.servermonitor.app.monitor.resources.CpuUsageMonitor
import life.vaporized.servermonitor.app.monitor.resources.DiskUsageMonitor
import life.vaporized.servermonitor.app.monitor.resources.RamUsageMonitor
import java.text.SimpleDateFormat
import java.util.*

fun Routing.indexRoute(
    statusHolder: StatusHolder
) {
    get("/") {
        val lastEval = statusHolder.last
        val lastStatus = lastEval?.list ?: emptyList()
        val resources = statusHolder.getResourceHistory()
        val diskUsage = lastEval?.list
            ?.filterIsInstance<MonitorStatus.ResourceStatus>()
            ?.filter { it.id.startsWith(DiskUsageMonitor.ID) }
            ?.maxBy { it.total }
            ?.let {
                listOf(it.current / 1024, it.free / 1024)
            }

        val start = System.currentTimeMillis()
        val timeLine = (0..statusHolder.capacity).map { i ->
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
                        .map { it.usage }.prefixWithNulls(statusHolder.capacity).takeEveryNth(2),
                    "cpu" to (resources[CpuUsageMonitor.ID] ?: emptyList())
                        .map { it.usage }.prefixWithNulls(statusHolder.capacity).takeEveryNth(2),
                    "disk" to (diskUsage ?: emptyList()),
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