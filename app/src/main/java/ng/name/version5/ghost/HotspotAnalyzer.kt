package ng.name.version5.ghost

import kotlin.math.max
import kotlin.math.sqrt

/** Greedy radius clustering of ghost samples into hotspots, done in millimetres so zones stay circular. */
object HotspotAnalyzer {
    fun analyze(
        samples: List<GhostSample>,
        widthMm: Float, heightMm: Float,
        clusterMm: Float = 6f, minHits: Int = 3, padMm: Float = 3f,
    ): List<Hotspot> {
        class C(var sx: Float, var sy: Float, var n: Int, var major: Float, var maxD: Float)
        val clusters = mutableListOf<C>()
        for (s in samples) {
            val px = s.x * widthMm; val py = s.y * heightMm
            val hit = clusters.minByOrNull { d(px, py, it.sx / it.n, it.sy / it.n) }
            if (hit != null && d(px, py, hit.sx / hit.n, hit.sy / hit.n) <= clusterMm) {
                hit.sx += px; hit.sy += py; hit.n++; hit.major += s.touchMajor
                hit.maxD = max(hit.maxD, d(px, py, hit.sx / hit.n, hit.sy / hit.n))
            } else clusters += C(px, py, 1, s.touchMajor, 0f)
        }
        return clusters.filter { it.n >= minHits }.map {
            val cxMm = it.sx / it.n; val cyMm = it.sy / it.n
            Hotspot(cxMm / widthMm, cyMm / heightMm, (max(it.maxD, 2f) + padMm) / widthMm, it.n, it.major / it.n)
        }.sortedByDescending { it.hits }
    }

    private fun d(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2; val dy = y1 - y2; return sqrt(dx * dx + dy * dy)
    }
}
