package life.vaporized.servermonitor.app

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.decodeFromString
import life.vaporized.servermonitor.app.model.AppDefinition
import life.vaporized.servermonitor.app.model.MonitorStatus
import life.vaporized.servermonitor.app.monitor.AppRunningMonitor
import life.vaporized.servermonitor.app.monitor.IResourceMonitor
import life.vaporized.servermonitor.app.monitor.resources.DiskUsageMonitor
import life.vaporized.servermonitor.app.monitor.resources.RamUsageMonitor
import net.mamoe.yamlkt.Yaml
import java.io.File

class Evaluator {
    private val staticMonitors: List<IResourceMonitor>
        get() = listOf(
            DiskUsageMonitor,
            RamUsageMonitor,
        )

    private val dynamicMonitors: List<AppRunningMonitor>
        get() = loadMonitors()

    suspend fun evaluate(): List<MonitorStatus> = coroutineScope {
        val status = (dynamicMonitors + staticMonitors).map { monitor ->
            async {
                monitor.evaluate().also { status ->
                    println("${monitor.name}: ${monitor.message}")
                    println("${monitor.name}: ${status?.isAlive}")
                }
            }
        }

        status.awaitAll().filterNotNull()
    }

    private fun loadMonitors(): List<AppRunningMonitor> {
        val yamlData = File("appconfig.yaml").readText()
        val services: List<AppDefinition> = Yaml.Default.decodeFromString(yamlData)

        // Print the loaded services
        services.forEach { println(it) }
        return services.map(::AppRunningMonitor)
    }
}