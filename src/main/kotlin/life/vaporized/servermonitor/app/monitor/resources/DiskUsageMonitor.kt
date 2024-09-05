package life.vaporized.servermonitor.app.monitor.resources

import life.vaporized.servermonitor.app.monitor.IResourceMonitor
import life.vaporized.servermonitor.app.monitor.model.MonitorStatus.ResourceStatus
import life.vaporized.servermonitor.app.util.getLogger
import java.nio.file.FileStore
import java.nio.file.FileSystems

object DiskUsageMonitor : IResourceMonitor {

    private val logger = getLogger()

    private const val SIZE_THRESHOLD = 1024 * 1024 * 1024

    override val id = "RVolume"
    override val name: String = "Disk usage"
    override val message: String = "Current usage of /"

    override suspend fun evaluate(): List<ResourceStatus> =
        getDiskUsage()
            .filter { (_, _, total) -> total > 0 }
            .map { (unit, free, total) ->
                ResourceStatus(
                    id = "${id}_$unit",
                    name = name,
                    description = unit,
                    current = total - free,
                    total = total,
                )
            }
            .sortedByDescending { it.total }

    private fun getDiskUsage(): List<Triple<String, Float, Float>> {
        val fileStores = buildList {
            for (fileStore: FileStore in FileSystems.getDefault().fileStores) {
                try {
                    if (fileStore.totalSpace < SIZE_THRESHOLD) {
                        continue
                    }
                    if (fileStore.type().equals("tmpfs", ignoreCase = true)) {
                        continue
                    }

                    val totalSpace = fileStore.totalSpace / (1024 * 1024 * 1024).toFloat()
                    val usableSpace = fileStore.usableSpace / (1024 * 1024 * 1024).toFloat()
                    add(Triple(fileStore.toString(), usableSpace, totalSpace))
                } catch (e: Exception) {
                    logger.error("Failed to get disk usage", e)
                }
            }
        }
        return fileStores
    }
}
