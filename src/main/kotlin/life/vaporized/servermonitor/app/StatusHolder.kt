package life.vaporized.servermonitor.app

import life.vaporized.servermonitor.app.model.MonitorEvaluation

class StatusHolder {

    val history: MutableList<MonitorEvaluation> = mutableListOf()

    val last
        get() = history.lastOrNull()

    fun add(evaluation: MonitorEvaluation) {
        history.add(evaluation)
    }
}