package life.vaporized.servermonitor.app.monitor.resources

import life.vaporized.servermonitor.app.model.MonitorStatus.ResourceStatus
import life.vaporized.servermonitor.app.monitor.IResourceMonitor
import java.nio.file.FileStore
import java.nio.file.FileSystems

object DiskUsageMonitor : IResourceMonitor {

    override val name: String = "Disk usage"
    override val message: String = "Current usage of /"

    override suspend fun evaluate(): ResourceStatus? =
        getDiskUsage().firstOrNull()?.let { (_, free, total) ->
            return ResourceStatus(
                name = name,
                description = message,
                current = (total - free).toFloat(),
                total = total.toFloat(),
            )
        }

    private fun getDiskUsage(): List<Triple<String, Long, Long>> {
        val fileStores = mutableListOf<Triple<String, Long, Long>>()
        for (fileStore: FileStore in FileSystems.getDefault().fileStores) {
            val totalSpace = fileStore.totalSpace / (1024 * 1024)
            val usableSpace = fileStore.usableSpace / (1024 * 1024)
            fileStores.add(Triple(fileStore.toString(), usableSpace, totalSpace))
        }
        return fileStores
    }
}