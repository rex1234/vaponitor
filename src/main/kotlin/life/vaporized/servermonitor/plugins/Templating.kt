package life.vaporized.servermonitor.plugins

import io.ktor.server.application.*
import io.ktor.server.thymeleaf.*
import org.thymeleaf.templateresolver.FileTemplateResolver

fun Application.configureTemplating() {
    install(Thymeleaf) {
//        setTemplateResolver(ClassLoaderTemplateResolver().apply {
//            prefix = "templates/thymeleaf/"
//            suffix = ".html"
//            characterEncoding = "utf-8"
//        })
        setTemplateResolver(
            FileTemplateResolver().apply {
                cacheManager = null
                prefix = "src/main/resources/templates/thymeleaf/"
                suffix = ".html"
            }
        )

    }
}