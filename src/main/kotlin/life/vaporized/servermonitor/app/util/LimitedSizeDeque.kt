package life.vaporized.servermonitor.app.util

class LimitedSizeDeque<T>(
    val capacity: Int,
) {

    private val deque = ArrayDeque<T>()

    val elements
        get() = deque.toList()

    val last
        get() = deque.lastOrNull()

    fun add(element: T) {
        if (deque.size >= capacity) {
            deque.removeFirst()
        }
        deque.addLast(element)
    }

    override fun toString(): String = deque.toString()
}
