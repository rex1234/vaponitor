package life.vaporized.servermonitor.plugins.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.thymeleaf.*

suspend fun errorRoute(
    call: ApplicationCall,
    cause: Throwable? = null,
    code: HttpStatusCode? = null,
) {
    call.respond(
        ThymeleafContent(
            template = "error",
            model = mapOf(
                "code" to code?.value.toString(),
                "message" to "Oops",
            ),
        )
    )
}