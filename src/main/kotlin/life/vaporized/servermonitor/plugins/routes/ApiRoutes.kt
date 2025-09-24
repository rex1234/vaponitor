package life.vaporized.servermonitor.plugins.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlin.system.exitProcess
import kotlinx.coroutines.delay
import life.vaporized.servermonitor.app.config.MonitorConfigProvider
import life.vaporized.servermonitor.app.cron.CronJobManager
import life.vaporized.servermonitor.app.cron.jobs.EvaluateMonitorsCronJob
import life.vaporized.servermonitor.app.util.getLogger

fun Routing.apiRoutes(
    cronManager: CronJobManager,
    evaluateMonitorsCronJob: EvaluateMonitorsCronJob,
    monitorConfigProvider: MonitorConfigProvider,
) {
    val logger = getLogger()

    route("/api") {

        post("/monitors/refresh") {
            try {
                logger.info("Manual refresh of all monitors requested")

                // Execute the same job that runs on schedule
                evaluateMonitorsCronJob.run()

                logger.info("Manual refresh completed successfully")
                call.respond(HttpStatusCode.OK, mapOf("message" to "All monitors refreshed successfully"))

            } catch (e: Exception) {
                logger.error("Error during manual monitor refresh", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to refresh monitors"))
            }
        }

        post("/config/reload") {
            try {
                logger.info("Configuration reload requested")

                // Stop current cron jobs
                cronManager.cancelAllJobs()

                // Force reload the YAML configuration
                monitorConfigProvider.reload()

                // Restart cron jobs with the newly loaded configuration
                cronManager.init()

                // Execute the evaluation job to apply new configuration
                evaluateMonitorsCronJob.run()

                logger.info("Configuration reloaded successfully")
                call.respond(HttpStatusCode.OK, mapOf("message" to "Configuration reloaded successfully"))

            } catch (e: Exception) {
                logger.error("Error during configuration reload", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to reload configuration"))
            }
        }

        post("/system/restart") {
            try {
                logger.info("Application restart requested")

                call.respond(HttpStatusCode.OK, mapOf("message" to "Application is restarting..."))

                // Delay to allow response to be sent
                delay(1000)

                // Stop all jobs gracefully
                cronManager.cancelAllJobs()

                // Exit the application (assuming it's managed by a process manager that will restart it)
                logger.info("Shutting down application for restart")
                exitProcess(0)

            } catch (e: Exception) {
                logger.error("Error during application restart", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to restart application"))
            }
        }
    }
}
