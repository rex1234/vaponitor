package life.vaporized.servermonitor.app.util

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

suspend fun <T> repeatOnError(
    times: Int = 3,
    delay: Duration = 1.seconds,
    logError: (Exception) -> Unit = {},
    default: T,
    action: () -> T,
): T {
    repeat(times) { current ->
        try {
            return action()
        } catch (e: Exception) {
            if (current < times) {
                delay(delay)
            } else {
                logError(e)
            }
        }
    }
    return default
}
