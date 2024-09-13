package life.vaporized.servermonitor.app.monitor.resources

import life.vaporized.servermonitor.app.monitor.model.NumberResourceDefinition

object RaspberryTempMonitor : BashNumberResourceMonitor(
    command = NumberResourceDefinition(
        name = "CPU temperature",
        description = "Current CPU temperature",
        command = arrayOf(
            "bash",
            "-c",
            "cat /sys/class/thermal/thermal_zone0/temp | awk '{printf \"%.1f\\n\", \$1 / 1000}'\n"
        )
        //command = arrayOf("cmd", "/c", "echo", "50")
    )
) {

    override val id = "RTemp"
    override val name: String = "CPU temperature"
    override val message: String = "Current CPU temperature"
}
