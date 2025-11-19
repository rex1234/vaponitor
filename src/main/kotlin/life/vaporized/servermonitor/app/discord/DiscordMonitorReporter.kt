package life.vaporized.servermonitor.app.discord

import dev.kord.common.Color
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import life.vaporized.servermonitor.app.StatusRepository
import life.vaporized.servermonitor.app.monitor.model.MonitorStatus
import life.vaporized.servermonitor.app.util.getLogger

private const val EMOJI_GREEN_DOT = "✅"
private const val EMOJI_RED_DOT = "❌"

@OptIn(ExperimentalTime::class)
class DiscordMonitorReporter(
    private val discordBot: DiscordBot,
    private val statusRepository: StatusRepository,
) {

    private val logger = getLogger()

    fun sendReport() {
        if (statusRepository.history.size < 2) {
            return
        }

        val eval = statusRepository.history.last(2)

        val currentAppEval = eval.last()
        val previousAppEval = eval.first()

        currentAppEval.apps
            .forEach { currentStatus ->
                val app = currentStatus.app
                val previousStatus = previousAppEval.apps
                    .find { it.id == currentStatus.id } ?: return@forEach

                if (currentStatus == previousStatus) {
                    return@forEach
                }

                if (currentStatus.isError) {
                    logger.info("Reporting error for $app on discord")
                    sendErrorEmbed(currentStatus, previousStatus)
                } else if (!currentStatus.isError && previousStatus.isError) {
                    sendRecoveryEmbed(currentStatus)
                }
            }
    }

    private fun isPartlyRecovered(
        appStatus: MonitorStatus.AppStatus,
        previousAppStatus: MonitorStatus.AppStatus,
    ) = (appStatus.isRunning && !previousAppStatus.isRunning) ||
            (appStatus.isHttpReachable == true && previousAppStatus.isHttpReachable == false) ||
            (appStatus.isHttpsReachable == true && previousAppStatus.isHttpsReachable == false)

    private fun sendErrorEmbed(
        appStatus: MonitorStatus.AppStatus,
        previousAppStatus: MonitorStatus.AppStatus,
    ) {
        val isPartialRecovery = isPartlyRecovered(appStatus, previousAppStatus)
        val color = if (isPartialRecovery) Color(0x1921BF) else Color(0xFF0000) // Blue or Red

        discordBot.sendEmbed {
            title = if (isPartialRecovery) {
                "Application Partly Recovered"
            } else {
                "Application Error"
            }
            description = "**${appStatus.name}**"
            this.color = color
            timestamp = Clock.System.now()

            field {
                name = "Status Details"
                value = buildStatusDetails(appStatus, previousAppStatus)
                inline = false
            }

            if (!appStatus.description.isBlank()) {
                footer {
                    text = appStatus.description
                }
            }
        }
    }

    private fun sendRecoveryEmbed(appStatus: MonitorStatus.AppStatus) {
        discordBot.sendEmbed {
            title = "Application Fully Recovered"
            description = "**${appStatus.name}**"
            color = Color(0x00FF00) // Green
            timestamp = Clock.System.now()

            field {
                name = "Current Status"
                value = buildString {
                    appendLine("$EMOJI_GREEN_DOT Process is running")
                    if (appStatus.isHttpReachable == true) {
                        appendLine("$EMOJI_GREEN_DOT HTTP is reachable")
                    }
                    if (appStatus.isHttpsReachable == true) {
                        appendLine("$EMOJI_GREEN_DOT HTTPS is reachable")
                    }
                }
                inline = false
            }

            if (!appStatus.description.isBlank()) {
                footer {
                    text = appStatus.description
                }
            }
        }
    }

    private fun buildStatusDetails(
        appStatus: MonitorStatus.AppStatus,
        previousAppStatus: MonitorStatus.AppStatus,
    ) = buildString {
        if (appStatus.isRunning && !previousAppStatus.isRunning) {
            appendLine("$EMOJI_GREEN_DOT Process is now running")
        } else if (!appStatus.isRunning) {
            appendLine("$EMOJI_RED_DOT Process is **not** running")
        }

        if (appStatus.isHttpReachable == true && previousAppStatus.isHttpReachable == false) {
            appendLine("$EMOJI_GREEN_DOT HTTP ping to ${appStatus.app.httpUrl} **success**")
        } else if (appStatus.isHttpReachable == false) {
            appendLine("$EMOJI_RED_DOT HTTP ping to ${appStatus.app.httpUrl} **failed**")
        }

        if (appStatus.isHttpsReachable == true && previousAppStatus.isHttpsReachable == false) {
            appendLine("$EMOJI_GREEN_DOT HTTPS ping to ${appStatus.app.httpsUrl} **success**")
        } else if (appStatus.isHttpsReachable == false) {
            appendLine("$EMOJI_RED_DOT HTTPS ping to ${appStatus.app.httpsUrl} **failed**")
        }
    }
}
