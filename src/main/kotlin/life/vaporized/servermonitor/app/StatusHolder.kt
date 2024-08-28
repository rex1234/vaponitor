package life.vaporized.servermonitor.app

import life.vaporized.servermonitor.Config
import life.vaporized.servermonitor.app.model.MonitorEvaluation
import life.vaporized.servermonitor.app.model.MonitorStatus
import life.vaporized.servermonitor.app.util.LimitedSizeDeque
import life.vaporized.servermonitor.app.util.StatusSerializer
import life.vaporized.servermonitor.app.util.getLogger
import java.io.File

class StatusHolder(
    private val statusSerializer: StatusSerializer,
) {

    private val logger = getLogger()

    val history: LimitedSizeDeque<MonitorEvaluation> = LimitedSizeDeque(
        (Config.historyDuration.inWholeSeconds / Config.monitorInterval.inWholeSeconds).toInt()
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

    fun save() {
        val jsonData = statusSerializer.serialize(history.elements)
        File("data.json").writeText(jsonData)
        logger.info("Stored current state")
    }

    fun restore() = runCatching {
        val jsonData = File("data.json").readText()
        val data = statusSerializer.deserialize(jsonData)

        data.forEach(::add)

        logger.info("Restore ${data.size} history entries")
    }.onFailure {
        logger.error("Failed to restore data", it)
    }
}