package life.vaporized.servermonitor.plugins.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.thymeleaf.ThymeleafContent

suspend fun errorRoute(
    call: ApplicationCall,
    cause: Throwable? = null,
    code: HttpStatusCode? = null,
) {
    call.respond(
        ThymeleafContent(
            template = "error",
            model = mapOf(
                    "code" to (code?.value?.toString() ?: cause?.cause?.message ?: "Unknown error"),
                "message" to "Oops",
            ),
        )
    )
}
