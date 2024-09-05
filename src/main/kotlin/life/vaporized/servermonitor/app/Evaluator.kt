package life.vaporized.servermonitor.app

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import life.vaporized.servermonitor.app.config.MonitorConfigProvider
import life.vaporized.servermonitor.app.monitor.AppRunningMonitor
import life.vaporized.servermonitor.app.monitor.IResourceMonitor
import life.vaporized.servermonitor.app.monitor.model.MonitorEvaluation
import life.vaporized.servermonitor.app.util.getLogger

class Evaluator(
    private val monitorConfig: MonitorConfigProvider,
) {

    val logger = getLogger()

    private val resourceMonitors: List<IResourceMonitor>
        get() = monitorConfig.enabledResourceMonitors

    private val appMonitors: List<AppRunningMonitor>
        get() = monitorConfig.appDefinitions.map(::AppRunningMonitor)

    suspend fun evaluate(): MonitorEvaluation = coroutineScope {
        val status = (resourceMonitors + appMonitors).map { monitor ->
            async {
                logger.info("${monitor.name}: ${monitor.message}")
                monitor.evaluate().onEach {
                    logger.debug("Evaluation finished: {}", it.toString())
                }
            }
        }
        MonitorEvaluation(status.awaitAll().flatten())
    }
}