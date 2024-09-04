package life.vaporized.servermonitor.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import life.vaporized.servermonitor.app.StatusRepository
import life.vaporized.servermonitor.plugins.routes.errorRoute
import life.vaporized.servermonitor.plugins.routes.indexRoute
import org.koin.ktor.ext.inject

fun Application.configureRouting() {

    val statusRepository: StatusRepository by inject()

    install(Resources)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            errorRoute(call, cause = cause)
        }
        status(HttpStatusCode.NotFound) { call, status ->
            errorRoute(call, code = status)
        }
    }
    routing {
        indexRoute(statusRepository)
        staticResources("/", "static")
    }
}


