package life.vaporized.servermonitor.app

import life.vaporized.servermonitor.app.model.MonitorEvaluation
import life.vaporized.servermonitor.app.util.LimitedSizeDeque

class StatusHolder {

    val history: LimitedSizeDeque<MonitorEvaluation> = LimitedSizeDeque(100)

    val last: MonitorEvaluation?
        get() = history.last

    fun add(evaluation: MonitorEvaluation) {
        history.add(evaluation)
    }
}