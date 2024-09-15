package life.vaporized.servermonitor.app.monitor.resources

import life.vaporized.servermonitor.app.monitor.IResourceMonitor
import life.vaporized.servermonitor.app.monitor.model.MonitorStatus
import life.vaporized.servermonitor.app.monitor.model.NumberResourceDefinition
import life.vaporized.servermonitor.app.util.getLogger

/**
 * Executes a command yielding int value that can be shown in a graph
 */
open class BashNumberResourceMonitor(
    val command: NumberResourceDefinition,
) : IResourceMonitor {

    private val logger = getLogger()

    override val id: String = "int_command_${command.hashCode()}"
    override val name: String = command.name
    override val message: String = command.description

    override suspend fun evaluate(): List<MonitorStatus.ResourceStatus> =
        MonitorStatus.ResourceStatus(
            id = id,
            name = name,
            description = message,
            current = getResourceValue(),
            total = 100f,
        ).let { listOf(it) }

    private fun getResourceValue(): Float {
        try {
            val process = ProcessBuilder(*command.command.toTypedArray()).start()

            process.inputStream.bufferedReader().use { reader ->
                val value = reader.readText().trim()
                process.waitFor()
                return value.toFloat()
            }
        } catch (e: Exception) {
            logger.error("Failed to get ${command.name} value", e)
            return 0f
        }
    }
}
