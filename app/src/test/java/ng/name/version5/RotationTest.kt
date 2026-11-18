package ng.name.version5

import ng.name.version5.ghost.*
import org.junit.Assert.assertEquals
import org.junit.Test

class RotationTest {
    @Test fun roundTripAllRotations() {
        for (rot in 0..3) for (u in listOf(0f, .2f, .5f, .9f)) for (v in listOf(0f, .3f, .75f, 1f)) {
            val n = Rotation.toNatural(u, v, rot)
            val b = Rotation.fromNatural(n.x, n.y, rot)
            assertEquals(u, b.x, 1e-5f); assertEquals(v, b.y, 1e-5f)
        }
    }

    @Test fun naturalTopLeftMovesWithRotation() {
        // Natural top-left (0,0) appears bottom-left in ROTATION_90 and top-right in ROTATION_270.
        val r90 = Rotation.fromNatural(0f, 0f, 1); assertEquals(0f, r90.x, 1e-5f); assertEquals(1f, r90.y, 1e-5f)
        val r270 = Rotation.fromNatural(0f, 0f, 3); assertEquals(1f, r270.x, 1e-5f); assertEquals(0f, r270.y, 1e-5f)
    }

    @Test fun sameGhostInPortraitAndLandscapeClustersOnce() {
        val p = Rotation.toNatural(0.3f, 0.6f, 0)
        val l = Rotation.fromNatural(p.x, p.y, 1).let { Rotation.toNatural(it.x, it.y, 1) }
        val s = listOf(p, l, p).mapIndexed { i, q -> GhostSample(i.toLong(), 0, q.x, q.y, .1f, .1f, 1, 0) }
        val hs = HotspotAnalyzer.analyze(s, 70f, 150f)
        assertEquals(1, hs.size); assertEquals(3, hs[0].hits)
    }

    @Test fun profileJsonRoundTrip() {
        val p = GhostProfile("1", "Case", 5L, 30, 1080, 2400, 420f, listOf(Hotspot(.1f, .2f, .05f, 9, .3f)))
        assertEquals(p, ProfileJson.fromJson(ProfileJson.toJson(p)))
    }
}

class ShakeTest {
    @Test fun threeSpikesTriggerOnceThenCooldown() {
        val s = ng.name.version5.service.ShakeLogic(2.7f)
        assertEquals(false, s.onSample(0, 3f)); assertEquals(false, s.onSample(150, 3f))
        assertEquals(true, s.onSample(300, 3f))
        assertEquals(false, s.onSample(450, 3f)); assertEquals(false, s.onSample(600, 3f))
    }
    @Test fun restingAndWalkingDoNotTrigger() {
        val s = ng.name.version5.service.ShakeLogic(2.7f)
        for (t in 0..5000 step 20) assertEquals(false, s.onSample(t.toLong(), 1.0f + (t % 7) * 0.1f))
        for (t in 0..5000 step 500) assertEquals(false, s.onSample(t.toLong(), 3.0f))
    }
}

class ZoneTest {
    private val prof = GhostProfile("1", "t", 0L, 30, 1080, 2400, 420f, listOf(Hotspot(.8f, .2f, .05f, 9, .3f)))

    @Test fun zoneIsHitInPortraitAndLandscape() {
        assertEquals(true, ProfileMatcher.isGhost(prof, 864f, 480f, 0, 1080f, 2400f))
        assertEquals(false, ProfileMatcher.isGhost(prof, 1000f, 480f, 0, 1080f, 2400f))
        // ROTATION_90: screen is 2400x1080, same physical spot is at (480, 216)
        assertEquals(true, ProfileMatcher.isGhost(prof, 480f, 216f, 1, 2400f, 1080f))
        assertEquals(false, ProfileMatcher.isGhost(prof, 580f, 216f, 1, 2400f, 1080f))
    }

    @Test fun radiusIsSameInBothOrientations() {
        val a = ZoneGeometry.toCurrentPx(prof.hotspots[0], 0, 1080f, 2400f)
        val b = ZoneGeometry.toCurrentPx(prof.hotspots[0], 1, 2400f, 1080f)
        assertEquals(a.r, b.r, 1e-3f)
    }
}
