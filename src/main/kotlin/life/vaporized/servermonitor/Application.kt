package life.vaporized.servermonitor

import io.ktor.server.application.*
import life.vaporized.servermonitor.app.Evaluator
import life.vaporized.servermonitor.plugins.configureHTTP
import life.vaporized.servermonitor.plugins.configureMonitoring
import life.vaporized.servermonitor.plugins.configureRouting
import life.vaporized.servermonitor.plugins.configureTemplating


val evaluator = Evaluator()

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureTemplating()
    configureMonitoring()
    configureHTTP()
    configureRouting(evaluator)
}
