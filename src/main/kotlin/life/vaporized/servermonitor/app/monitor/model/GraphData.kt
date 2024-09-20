package life.vaporized.servermonitor.app.monitor.model

data class GraphDefinition<T>(
    val title: String,
    val description: String,
    val data: GraphData<T>,
) {

    val id: String
        get() = hashCode().toString()

    data class GraphData<T>(
        val graphName: String,
        val xAxis: List<T?>,
        val yAxis: List<YAxisData>,
    ) {

        data class YAxisData(
            val name: String,
            val data: List<Float?>,
            val formattedValues: List<String?>,
            val max: Int = 100,
            val min: Int = 0,
        )
    }

}

