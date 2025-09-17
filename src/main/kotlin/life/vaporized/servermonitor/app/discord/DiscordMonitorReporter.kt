package life.vaporized.servermonitor.app.discord

import life.vaporized.servermonitor.app.StatusRepository
import life.vaporized.servermonitor.app.monitor.model.MonitorStatus
import life.vaporized.servermonitor.app.util.getLogger

private const val EMOJI_GREEN_DOT = "✓"
private const val EMOJI_RED_DOT = "✘"

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
                    discordBot.sendMessage(errorMessage(currentStatus, previousStatus))
                } else if (!currentStatus.isError && previousStatus.isError) {
                    discordBot.sendMessage("✅ **Application has fully recovered: ${app.name}**")
                }
            }
    }

    private fun isPartlyRecovered(
        appStatus: MonitorStatus.AppStatus,
        previousAppStatus: MonitorStatus.AppStatus,
    ) = (appStatus.isRunning && !previousAppStatus.isRunning) ||
            (appStatus.isHttpReachable == true && previousAppStatus.isHttpReachable == false) ||
            (appStatus.isHttpsReachable == true && previousAppStatus.isHttpsReachable == false)

    private fun errorMessage(
        appStatus: MonitorStatus.AppStatus,
        previousAppStatus: MonitorStatus.AppStatus,
    ) = buildString {
        if (isPartlyRecovered(appStatus, previousAppStatus)) {
            appendLine("ℹ\uFE0F **Application has partly recovered: ${appStatus.name}**")
        } else {
            appendLine("❌ **Application error: ${appStatus.name}**")
        }
        appendStatusList(appStatus, previousAppStatus)
    }

    private fun StringBuilder.appendStatusList(
        appStatus: MonitorStatus.AppStatus,
        previousAppStatus: MonitorStatus.AppStatus,
    ) {
        if (appStatus.isRunning && !previousAppStatus.isRunning) {
            appendLine("    $EMOJI_GREEN_DOT Process is now running")
        } else if (!appStatus.isRunning) {
            appendLine("    $EMOJI_RED_DOT Process is *not* running")
        }

        if (appStatus.isHttpReachable == true && previousAppStatus.isHttpReachable == false) {
            appendLine("    $EMOJI_GREEN_DOT HTTP ping to ${appStatus.app.httpUrl} *success*")
        } else if (appStatus.isHttpReachable == false) {
            appendLine("    $EMOJI_RED_DOT HTTP ping to ${appStatus.app.httpUrl} *failed*")
        }

        if (appStatus.isHttpsReachable == true && previousAppStatus.isHttpsReachable == false) {
            appendLine("    $EMOJI_GREEN_DOT HTTPS ping ${appStatus.app.httpsUrl} success")
        } else if (appStatus.isHttpsReachable == false) {
            appendLine("    $EMOJI_RED_DOT HTTPS ping ${appStatus.app.httpsUrl} failed")
        }
    }
}
