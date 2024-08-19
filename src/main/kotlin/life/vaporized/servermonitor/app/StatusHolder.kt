package life.vaporized.servermonitor.app

import life.vaporized.servermonitor.app.cron.CronJobManager
import life.vaporized.servermonitor.app.model.MonitorEvaluation
import life.vaporized.servermonitor.app.model.MonitorStatus
import life.vaporized.servermonitor.app.util.LimitedSizeDeque
import kotlin.time.Duration.Companion.hours

class StatusHolder {

    val history: LimitedSizeDeque<MonitorEvaluation> = LimitedSizeDeque(
        (24.hours.inWholeSeconds / CronJobManager.EVALUATE_MONITORS_INTERVAL.inWholeSeconds).toInt()
    )

    val capacity
        get() = history.capacity

    val last: MonitorEvaluation?
        get() = history.last

    fun add(evaluation: MonitorEvaluation) {
        history.add(evaluation)
    }

    fun getResourceHistory(): Map<String, List<MonitorStatus.ResourceStatus>> =
        history.elements.flatMap { eval ->
            eval.list
                .filterIsInstance<MonitorStatus.ResourceStatus>()
                .filter { it.id.startsWith("R") }
        }.groupBy { it.id }

    fun getStatusHistory(id: String) =
        history.elements.mapNotNull { eval ->
            eval.list.firstOrNull { it.id == id }
        }
}