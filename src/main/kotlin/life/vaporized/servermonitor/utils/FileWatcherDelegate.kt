import kotlin.concurrent.thread
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.util.concurrent.atomic.AtomicReference

class ReadOnlyFileMappedDelegate<T>(
    private val filePath: Path,
    private val map: (String) -> T
) : ReadOnlyProperty<Any?, T> {

    private val cached: AtomicReference<T>

    init {
        // Initial load (throws if map fails, so caller knows at startup)
        cached = AtomicReference(map(Files.readString(filePath)))

        val dir = filePath.parent ?: Paths.get(".")
        val watchService = FileSystems.getDefault().newWatchService()
        dir.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE
        )

        thread(isDaemon = true, name = "watch-${filePath.fileName}") {
            while (true) {
                val key = try {
                    watchService.take()
                } catch (_: InterruptedException) {
                    break
                }
                var changedThisRound = false

                for (event in key.pollEvents()) {
                    val changed = event.context() as? Path ?: continue
                    if (changed == filePath.fileName) {
                        changedThisRound = true
                    }
                }

                if (changedThisRound && Files.exists(filePath)) {
                    try {
                        cached.set(map(Files.readString(filePath)))
                    } catch (e: Exception) {
                        // map decides how to handle errors
                        // if it throws, the last good value stays
                    }
                }

                if (!key.reset()) break
            }
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = cached.get()
}

fun <T> watchFile(path: String, map: (String) -> T): ReadOnlyProperty<Any?, T> =
    ReadOnlyFileMappedDelegate(Paths.get(path), map)
