package ng.name.version5.service

/** Pure shake detector: N spikes above [thresholdG] inside [windowMs]. Unit-testable. */
class ShakeLogic(
    var thresholdG: Float = 2.7f,
    private val windowMs: Long = 600,
    private val spikes: Int = 3,
    private val cooldownMs: Long = 1000,
    private val minGapMs: Long = 80,
) {
    private val times = ArrayDeque<Long>()
    private var lastTrigger = Long.MIN_VALUE / 2

    fun onSample(tMs: Long, g: Float): Boolean {
        if (g < thresholdG) return false
        if (times.isNotEmpty() && tMs - times.last() < minGapMs) return false
        times.addLast(tMs)
        while (times.isNotEmpty() && tMs - times.first() > windowMs) times.removeFirst()
        if (times.size >= spikes && tMs - lastTrigger > cooldownMs) {
            lastTrigger = tMs; times.clear(); return true
        }
        return false
    }
}
