package ng.name.version5.ghost

/** One raw capture sample, already converted to natural-space fractions. */
data class GhostSample(
    val t: Long, val pointerId: Int, val x: Float, val y: Float,
    val touchMajor: Float, val pressure: Float, val toolType: Int, val rotation: Int,
)

/** A blocking zone in natural space. [r] is a radius as a fraction of natural WIDTH. */
data class Hotspot(
    val cx: Float, val cy: Float, val r: Float,
    val hits: Int, val avgMajor: Float, val enabled: Boolean = true,
)

data class GhostProfile(
    val id: String,
    val name: String,
    val createdAt: Long,
    val watchSeconds: Int,
    val naturalWidthPx: Int,
    val naturalHeightPx: Int,
    val xdpi: Float,
    val hotspots: List<Hotspot>,
    val version: Int = 1,
)
