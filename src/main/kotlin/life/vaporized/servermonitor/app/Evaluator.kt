package life.vaporized.servermonitor.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
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

    suspend fun evaluate(): MonitorEvaluation = withContext(Dispatchers.IO) {
        logger.info("Evaluating resource monitors: ${resourceMonitors.joinToString(", ") { it.name }}")
        logger.info("Evaluating app monitors: ${appMonitors.joinToString(", ") { it.name }}")

        val resources = resourceMonitors.map { monitor ->
            async {
                monitor.evaluate().onEach {
                    logger.debug("Evaluation finished: ${it.id} - ${it.current}/${it.total}")
                }
            }
        }
        val apps = appMonitors.map { monitor ->
            async {
                logger.debug("Evaluating: ${monitor.name}")
                monitor.evaluate().onEach {
                    logger.debug("Evaluation finished: ${it.id} - isError: ${it.isError}")
                }
            }
        }
        MonitorEvaluation(apps.awaitAll().flatten(), resources.awaitAll().flatten())
    }
}
