import life.vaporized.servermonitor.app.DiscordBot
import life.vaporized.servermonitor.app.Evaluator
import life.vaporized.servermonitor.app.StatusHolder
import life.vaporized.servermonitor.app.cron.CronJobManager
import life.vaporized.servermonitor.app.cron.jobs.EvaluateMonitorsCronJob
import life.vaporized.servermonitor.app.util.StatusSerializer
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val mainModule = module {
    singleOf(::DiscordBot)
    singleOf(::CronJobManager)
    singleOf(::StatusHolder)

    factoryOf(::StatusSerializer)
    factoryOf(::Evaluator)
    factoryOf(::EvaluateMonitorsCronJob)
}