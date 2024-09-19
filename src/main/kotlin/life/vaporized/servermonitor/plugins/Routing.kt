package life.vaporized.servermonitor.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.resources.Resources
import io.ktor.server.routing.routing
import life.vaporized.servermonitor.app.util.getLogger
import life.vaporized.servermonitor.plugins.routes.errorRoute
import life.vaporized.servermonitor.plugins.routes.indexRoute
import org.koin.ktor.ext.get

fun Application.configureRouting() {
    val logger = getLogger()
    install(Resources)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            errorRoute(call, exception = cause)
        }
        status(HttpStatusCode.NotFound) { call, status ->
            errorRoute(call, code = status)
        }
    }
    routing {
        indexRoute(get(), get())
        staticResources("/", "static")
    }
}
