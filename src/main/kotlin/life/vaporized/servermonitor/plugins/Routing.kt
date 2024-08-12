package life.vaporized.servermonitor.plugins

import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import kotlinx.serialization.Serializable
import life.vaporized.servermonitor.app.DiscordBot
import life.vaporized.servermonitor.app.Evaluator
import life.vaporized.servermonitor.app.model.MonitorStatus
import org.koin.ktor.ext.inject

fun Application.configureRouting() {

    val evaluator: Evaluator by inject()
    val discordBot: DiscordBot by inject()

    install(Resources)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/") {
            val status = evaluator.evaluate()
            println(status)
            val response = buildString {
                status.forEach { monitorStatus ->
                    when (monitorStatus) {
                        is MonitorStatus.AppStatus -> append(monitorStatus.toString())
                        is MonitorStatus.ResourceStatus -> append(monitorStatus.toString())
                    }
                    append("<br/>")
                }
            }

            discordBot.sendMessage(response)

            call.respond(
                ThymeleafContent(
                    template = "index2",
                    model = mapOf("appStatusList" to status.filterIsInstance<MonitorStatus.AppStatus>()),
                )
            )
        }
        get<Articles> { article ->
            // Get all articles ...
            call.respond("List of articles sorted starting from ${article.sort}")
        }
        // Static plugin. Try to access `/static/index.html`
        staticResources("/", "static")
    }
}

@Serializable
@Resource("/articles")
class Articles(val sort: String? = "new")
