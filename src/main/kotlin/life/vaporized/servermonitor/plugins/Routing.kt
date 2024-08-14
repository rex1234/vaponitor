package life.vaporized.servermonitor.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.resources.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.thymeleaf.*
import life.vaporized.servermonitor.app.StatusHolder
import life.vaporized.servermonitor.app.model.MonitorStatus
import org.koin.ktor.ext.inject
import java.text.SimpleDateFormat
import java.util.*

fun Application.configureRouting() {

    val statusHolder: StatusHolder by inject()

    install(Resources)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/") {
            val lastEval = statusHolder.last
            val lastStatus = lastEval?.list ?: emptyList()

            call.respond(
                ThymeleafContent(
                    template = "index2",
                    model = mapOf(
                        "time" to SimpleDateFormat.getTimeInstance().format(Date(lastEval?.time ?: 0)),
                        "appStatusList" to lastStatus.filterIsInstance<MonitorStatus.AppStatus>()
                    ),
                )
            )
        }

        staticResources("/", "static")
    }
}