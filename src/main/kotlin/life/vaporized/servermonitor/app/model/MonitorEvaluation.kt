package life.vaporized.servermonitor.app.model

data class MonitorEvaluation(
    val list: List<MonitorStatus>,
    val time: Long = System.currentTimeMillis(),
)