package life.vaporized.servermonitor.app.monitor.resources

import life.vaporized.servermonitor.app.monitor.model.MonitorStatus
import life.vaporized.servermonitor.app.monitor.model.NumberResourceDefinition
import life.vaporized.servermonitor.app.util.getLogger
import java.util.concurrent.TimeUnit

object Dht22Monitor : BashNumberResourceMonitor(
    command = NumberResourceDefinition(
        name = "Raspberry sensor data",
        description = "Current DHT22 temperature and humidity",
        command = listOf(
            "bash",
            "-c",
            "/home/rex/Projects/temp/temp/bin/python /home/rex/Projects/temp/read.py"
        )
    )
) {

    val logger = getLogger()

    override val id = "RDht"
    override val name: String = "Environment temperature"
    override val message: String = "Current DHT22 temperature"

    override suspend fun evaluate(): List<MonitorStatus.ResourceStatus> {
        return getResourceValue().let { (temp, humidity) ->
            listOfNotNull(
                temp?.let {
                    MonitorStatus.ResourceStatus(
                        id = "${id}Temp",
                        name = name,
                        description = "Temperature",
                        current = it,
                        total = 60f,
                    )
                },
                humidity?.let {
                    MonitorStatus.ResourceStatus(
                        id = "${id}Humidity",
                        name = name,
                        description = "Humidity",
                        current = it,
                        total = 100f,
                    )
                }
            )
        }
    }

    private fun getResourceValue(): Pair<Float?, Float?> {
        try {
            val process = ProcessBuilder(*command.command.toTypedArray())
                .apply {
                    environment().putAll(System.getenv())
                }.start()

            val processResult = process.inputStream.bufferedReader().use { reader ->
                val temp = reader.readText().trim().toFloatOrNull()
                val humidity = reader.readText().trim().toFloatOrNull()
                process.waitFor(5, TimeUnit.SECONDS)
                Pair(temp, humidity)
            }

            val errors = process.errorStream.bufferedReader().readText()
            if (errors.isNotEmpty()) {
                logger.error("Error while executing ${command.name} command: $errors")
            }

            return processResult
        } catch (e: Exception) {
            logger.error("Failed to get ${command.name} value", e)
            return null to null
        }
    }
}
