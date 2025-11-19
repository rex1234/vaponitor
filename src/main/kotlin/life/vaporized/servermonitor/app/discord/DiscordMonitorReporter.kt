package life.vaporized.servermonitor.app.discord

import dev.kord.common.Color
import dev.kord.rest.builder.message.EmbedBuilder
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
        val color = if (isPartialRecovery) Color(0x1921BF) else Color(0xCB2A3E)

        discordBot.sendEmbed {
            title = "**${appStatus.name}**"
            description = if (isPartialRecovery) "Application Partly Recovered" else "Application Error"
            this.color = color
            timestamp = Clock.System.now()

            addProcessField(appStatus, previousAppStatus, inline = true)
            addHttpField(appStatus, previousAppStatus, inline = true)
            addHttpsField(appStatus, previousAppStatus, inline = true)
            addFooterIfPresent(appStatus)
        }
    }

    private fun sendRecoveryEmbed(appStatus: MonitorStatus.AppStatus) {
        discordBot.sendEmbed {
            title = appStatus.name
            description = "**Application Fully Recovered**"
            color = Color(0x669949)
            timestamp = Clock.System.now()

            addProcessField(appStatus, previousAppStatus = null, inline = true)
            addHttpField(appStatus, previousAppStatus = null, inline = true)
            addHttpsField(appStatus, previousAppStatus = null, inline = true)
            addFooterIfPresent(appStatus)
        }
    }

    private fun EmbedBuilder.addProcessField(
        appStatus: MonitorStatus.AppStatus,
        previousAppStatus: MonitorStatus.AppStatus?,
        inline: Boolean
    ) {
        field {
            name = "🔧 Process\t\t\t|"
            value = getProcessStatusText(appStatus, previousAppStatus)
            this.inline = inline
        }
    }

    private fun EmbedBuilder.addHttpField(
        appStatus: MonitorStatus.AppStatus,
        previousAppStatus: MonitorStatus.AppStatus?,
        inline: Boolean
    ) {
        if (appStatus.app.httpUrl == null || appStatus.isHttpReachable == null) return

        field {
            name = "🌐 HTTP\t\t\t|"
            value = buildString {
                appendLine(getHttpStatusText(appStatus, previousAppStatus))
                append(appStatus.app.httpUrl)
            }
            this.inline = inline
        }
    }

    private fun EmbedBuilder.addHttpsField(
        appStatus: MonitorStatus.AppStatus,
        previousAppStatus: MonitorStatus.AppStatus?,
        inline: Boolean
    ) {
        if (appStatus.app.httpsUrl == null || appStatus.isHttpsReachable == null) return

        field {
            name = "🔒 HTTPS\t\t\t"
            value = buildString {
                appendLine(getHttpsStatusText(appStatus, previousAppStatus))
                append(appStatus.app.httpsUrl)
            }
            this.inline = inline
        }
    }

    private fun EmbedBuilder.addFooterIfPresent(appStatus: MonitorStatus.AppStatus) {
        if (appStatus.description.isNotBlank()) {
            footer { text = appStatus.description }
        }
    }

    private fun getProcessStatusText(
        appStatus: MonitorStatus.AppStatus,
        previousAppStatus: MonitorStatus.AppStatus?
    ): String = when {
        previousAppStatus != null && appStatus.isRunning && !previousAppStatus.isRunning ->
            "$EMOJI_GREEN_DOT Now running"

        !appStatus.isRunning ->
            "$EMOJI_RED_DOT Not running"

        else ->
            "Running"
    }

    private fun getHttpStatusText(
        appStatus: MonitorStatus.AppStatus,
        previousAppStatus: MonitorStatus.AppStatus?
    ): String = when {
        previousAppStatus != null && appStatus.isHttpReachable == true && previousAppStatus.isHttpReachable == false ->
            "$EMOJI_GREEN_DOT Recovered"

        appStatus.isHttpReachable == false ->
            "$EMOJI_RED_DOT Failed"

        appStatus.isHttpReachable == true ->
            "Reachable"

        else ->
            "N/A"
    }

    private fun getHttpsStatusText(
        appStatus: MonitorStatus.AppStatus,
        previousAppStatus: MonitorStatus.AppStatus?
    ): String = when {
        previousAppStatus != null && appStatus.isHttpsReachable == true && previousAppStatus.isHttpsReachable == false ->
            "$EMOJI_GREEN_DOT Recovered"

        appStatus.isHttpsReachable == false ->
            "$EMOJI_RED_DOT Failed"

        appStatus.isHttpsReachable == true ->
            "Reachable"

        else ->
            "N/A"
    }
}
