package life.vaporized.servermonitor.app.monitor.resources

import life.vaporized.servermonitor.app.monitor.IResourceMonitor
import life.vaporized.servermonitor.app.monitor.model.MonitorStatus
import life.vaporized.servermonitor.app.monitor.model.NumberResourceDefinition
import life.vaporized.servermonitor.app.util.getLogger
import java.util.concurrent.TimeUnit

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
            val process = ProcessBuilder(*command.command.toTypedArray())
                .apply {
                    environment().putAll(System.getenv())
                }.start()

            val processResult = process.inputStream.bufferedReader().use { reader ->
                val value = reader.readText().trim()
                process.waitFor(5, TimeUnit.SECONDS)
                value.toFloatOrNull() ?: -1f
            }

            val errors = process.errorStream.bufferedReader().readText()
            if (errors.isNotEmpty()) {
                logger.error("Error while executing ${command.name} command: $errors")
            }
            return processResult
        } catch (e: Exception) {
            logger.error("Failed to get ${command.name} value", e)
            return 0f
        }
    }
}
