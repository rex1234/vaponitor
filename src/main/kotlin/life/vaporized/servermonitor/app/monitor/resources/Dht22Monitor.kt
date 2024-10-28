package life.vaporized.servermonitor.app.monitor.resources

import life.vaporized.servermonitor.app.monitor.model.MonitorStatus
import life.vaporized.servermonitor.app.monitor.model.NumberResourceDefinition
import life.vaporized.servermonitor.app.util.getLogger
import life.vaporized.servermonitor.app.util.repeatOnError
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
        return getResourceValue(attempts = 3).let { (temp, humidity) ->
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

    private suspend fun getResourceValue(attempts: Int = 1): Pair<Float?, Float?> = repeatOnError(
        default = null to null,
        logError = { e ->
            logger.error("Failed to get DHT22 value after multiple attempts", e)
        },
        action = ::loadValues,
    )

    private fun loadValues(): Pair<Float, Float> {
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
    }
}
