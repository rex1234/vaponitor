package life.vaporized.servermonitor.app.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import life.vaporized.servermonitor.app.model.MonitorEvaluation
import life.vaporized.servermonitor.app.model.MonitorStatus

class StatusSerializer {

    private val module = SerializersModule {
        polymorphic(MonitorStatus::class) {
            subclass(MonitorStatus.ResourceStatus::class, MonitorStatus.ResourceStatus.serializer())
            subclass(MonitorStatus.AppStatus::class, MonitorStatus.AppStatus.serializer())
        }
    }

    private val json = Json { serializersModule = module }

    fun serialize(data: List<MonitorEvaluation>): String {
        return json.encodeToString(data)
    }

    fun deserialize(jsonStr: String): List<MonitorEvaluation> {
        return json.decodeFromString(jsonStr)
    }
}