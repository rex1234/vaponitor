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
        listOfNotNull(getResourceValue()).map {
            MonitorStatus.ResourceStatus(
                id = id,
                name = name,
                description = message,
                current = it,
                total = 100f,
            )
        }

    private fun getResourceValue(): Float? {
        try {
            val process = ProcessBuilder(*command.command.toTypedArray())
                .apply {
                    environment().putAll(System.getenv())
                }.start()

            process.waitFor(20, TimeUnit.SECONDS)

            val processResult = process.inputStream.bufferedReader().use { reader ->
                val value = reader.readText().trim()
                value.toFloatOrNull()
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
