package life.vaporized.servermonitor.app

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import life.vaporized.servermonitor.Config
import life.vaporized.servermonitor.app.model.MonitorEvaluation
import life.vaporized.servermonitor.app.monitor.AppRunningMonitor
import life.vaporized.servermonitor.app.monitor.IResourceMonitor
import life.vaporized.servermonitor.app.monitor.resources.CpuUsageMonitor
import life.vaporized.servermonitor.app.monitor.resources.DiskUsageMonitor
import life.vaporized.servermonitor.app.monitor.resources.RamUsageMonitor
import life.vaporized.servermonitor.app.util.getLogger

class Evaluator {

    val logger = getLogger()

    private val staticMonitors: List<IResourceMonitor>
        get() = listOf(
            DiskUsageMonitor,
            RamUsageMonitor,
            CpuUsageMonitor,
        )

    private val dynamicMonitors: List<AppRunningMonitor>
        get() = loadMonitors()

    suspend fun evaluate(): MonitorEvaluation = coroutineScope {
        val status = (dynamicMonitors + staticMonitors).map { monitor ->
            async {
                logger.info("${monitor.name}: ${monitor.message}")
                monitor.evaluate().onEach {
                    logger.debug("Evaluation finished: {}", it.toString())
                }
            }
        }
        MonitorEvaluation(status.awaitAll().flatten())
    }

    private fun loadMonitors(): List<AppRunningMonitor> {
        val services = Config.loadAppMonitors()
        services.forEach {
            logger.info("Initializing monitor for: $it")
        }
        return services.map(::AppRunningMonitor)
    }
}