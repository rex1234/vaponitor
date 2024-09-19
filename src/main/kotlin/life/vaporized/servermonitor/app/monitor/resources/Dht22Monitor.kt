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
    override val name: String = "Environmental sensors"
    override val message: String = "Current DHT22 sensor values"

    val tempId = "${id}Temp"
    val humidityId = "${id}Humidity"

    override suspend fun evaluate(): List<MonitorStatus.ResourceStatus> {
        return getResourceValue().let { (temp, humidity) ->
            listOfNotNull(
                temp?.let {
                    MonitorStatus.ResourceStatus(
                        id = tempId,
                        name = name,
                        description = "Temperature",
                        current = it,
                        total = 60f,
                    )
                },
                humidity?.let {
                    MonitorStatus.ResourceStatus(
                        id = humidityId,
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

            process.waitFor(5, TimeUnit.SECONDS)

            val processResult = process.inputStream.bufferedReader().use { reader ->
                val eval = reader.readText().trim()
                eval.split(" ").map {
                    it.toFloat()
                }.let { Pair(it[0], it[1]) }
            }

            process.errorStream.bufferedReader().use {
                val errors = it.readText().trim()
                if (errors.isNotEmpty()) {
                    logger.error("Error while executing ${command.name} command: $errors")
                }
            }

            return processResult
        } catch (e: Exception) {
            logger.error("Failed to get DHT22 value", e)
            return null to null
        }
    }
}
