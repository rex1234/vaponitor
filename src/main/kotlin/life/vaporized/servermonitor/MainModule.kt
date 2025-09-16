import life.vaporized.servermonitor.app.Evaluator
import life.vaporized.servermonitor.app.StatusRepository
import life.vaporized.servermonitor.app.config.MonitorConfigProvider
import life.vaporized.servermonitor.app.cron.CronJobManager
import life.vaporized.servermonitor.app.cron.jobs.CleanDatabaseJob
import life.vaporized.servermonitor.app.cron.jobs.EvaluateMonitorsCronJob
import life.vaporized.servermonitor.app.discord.DiscordBot
import life.vaporized.servermonitor.app.discord.DiscordMonitorReporter
import life.vaporized.servermonitor.app.util.StatusSerializer
import life.vaporized.servermonitor.db.SqliteDb
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val mainModule = module {
    singleOf(::DiscordBot)
    singleOf(::CronJobManager)
    singleOf(::StatusRepository)
    singleOf(::MonitorConfigProvider)
    singleOf(::SqliteDb)

    factoryOf(::DiscordMonitorReporter)
    factoryOf(::StatusSerializer)
    factoryOf(::Evaluator)
    factoryOf(::EvaluateMonitorsCronJob)
    factoryOf(::CleanDatabaseJob)
}
