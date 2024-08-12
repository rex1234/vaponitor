package life.vaporized.servermonitor.app.cron

import kotlinx.coroutines.*
import kotlin.time.Duration

class CronJob(
    private val interval: Duration,
) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    fun start(action: suspend () -> Unit) {
        job = scope.launch {
            while (isActive) {
                action()
                delay(interval)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun cancelScope() {
        scope.cancel()
    }
}
