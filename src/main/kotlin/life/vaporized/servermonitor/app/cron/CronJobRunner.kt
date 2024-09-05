package life.vaporized.servermonitor.app.cron

import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CronJobRunner(
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
