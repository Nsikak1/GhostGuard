package ng.name.version5.ghost

/** Point as a fraction (0..1) of the screen. Pure Kotlin so it is JVM unit-testable. */
data class FPoint(val x: Float, val y: Float)

/**
 * Converts between the CURRENT display orientation and the device's NATURAL
 * (rotation 0) space. [rotation] uses Surface.ROTATION_* values: 0, 1 (90), 2 (180), 3 (270).
 * Profiles are always stored in natural space, so they survive any orientation.
 */
object Rotation {
    fun toNatural(u: Float, v: Float, rotation: Int): FPoint = when (rotation) {
        1 -> FPoint(1f - v, u)
        2 -> FPoint(1f - u, 1f - v)
        3 -> FPoint(v, 1f - u)
        else -> FPoint(u, v)
    }

    fun fromNatural(x: Float, y: Float, rotation: Int): FPoint = when (rotation) {
        1 -> FPoint(y, 1f - x)
        2 -> FPoint(1f - x, 1f - y)
        3 -> FPoint(1f - y, x)
        else -> FPoint(x, y)
    }

    /** Natural-space size (w,h) -> size as seen in the current orientation. */
    fun sizeFromNatural(w: Float, h: Float, rotation: Int): Pair<Float, Float> =
        if (rotation == 1 || rotation == 3) h to w else w to h
}

/** A zone in CURRENT-orientation pixels. */
data class Zone(val cx: Float, val cy: Float, val r: Float)

object ZoneGeometry {
    /** [curW]/[curH] are the full screen size in the current orientation. Radius is a fraction of natural width. */
    fun toCurrentPx(h: Hotspot, rotation: Int, curW: Float, curH: Float): Zone {
        val p = Rotation.fromNatural(h.cx, h.cy, rotation)
        val naturalW = if (rotation == 1 || rotation == 3) curH else curW
        return Zone(p.x * curW, p.y * curH, h.r * naturalW)
    }
}

object ProfileMatcher {
    /** True if the touch at ([px],[py]) in current-orientation pixels lands in an enabled zone. */
    fun isGhost(profile: GhostProfile, px: Float, py: Float, rotation: Int, curW: Float, curH: Float): Boolean =
        profile.hotspots.any { h ->
            h.enabled && ZoneGeometry.toCurrentPx(h, rotation, curW, curH).let { z ->
                val dx = px - z.cx; val dy = py - z.cy
                dx * dx + dy * dy <= z.r * z.r
            }
        }
}
