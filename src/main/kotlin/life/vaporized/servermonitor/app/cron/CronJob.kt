package life.vaporized.servermonitor.app.cron

import kotlinx.coroutines.*
import kotlin.time.Duration

class CronJob(private val intervalMillis: Duration) {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    fun start(action: suspend () -> Unit) {
        job = scope.launch {
            while (isActive) {
                action()
                delay(intervalMillis)
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
