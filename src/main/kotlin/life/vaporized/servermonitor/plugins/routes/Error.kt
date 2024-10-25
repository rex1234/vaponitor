package life.vaporized.servermonitor.plugins.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.thymeleaf.ThymeleafContent

suspend fun errorRoute(
    call: ApplicationCall,
    exception: Throwable? = null,
    code: HttpStatusCode? = null,
) {
    call.respond(
        status = code ?: HttpStatusCode.InternalServerError,
        message = ThymeleafContent(
            template = "error",
            model = mapOf(
                "code" to ("HTTP ${code?.value?.toString() ?: "500"}"),
                "message" to (exception?.cause?.message ?: "Unknown error"),
            ),
        )
    )
}
