package life.vaporized.servermonitor.app.monitor.resources

import life.vaporized.servermonitor.app.monitor.model.NumberResourceDefinition

object CpuUsageMonitor : BashNumberResourceMonitor(
    command = NumberResourceDefinition(
        name = "CPU usage",
        description = "Current CPU usage",
        command = arrayOf("bash", "-c", "mpstat -P ALL 5 1 | tail -n 1 | awk '{print 100 - \$NF}'"),
    )
) {

    override val id = "RCpu"
    override val name: String = "CPU usage"
    override val message: String = "Current CPU usage"
}
