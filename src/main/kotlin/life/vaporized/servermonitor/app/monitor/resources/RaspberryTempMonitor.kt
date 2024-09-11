package life.vaporized.servermonitor.app.monitor.resources

import life.vaporized.servermonitor.app.monitor.model.NumberResourceDefinition

object RaspberryTempMonitor : BashNumberResourceMonitor(
    command = NumberResourceDefinition(
        name = "CPU temperature",
        description = "Current CPU temperature",
        command = arrayOf("bash", "-c", "vcgencmd measure_temp | cut -d '=' -f 2 | cut -d \"'\" -f 1")
        //command = arrayOf("cmd", "/c", "echo", "50")
    )
) {

    override val id = "RTemp"
    override val name: String = "CPU temperature"
    override val message: String = "Current CPU temperature"
}
