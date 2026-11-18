package ng.name.version5.capture

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import ng.name.version5.data.Screen
import ng.name.version5.ghost.*
import ng.name.version5.ui.theme.Version5Theme

/** Test mode: draws the zones, turns each touch green (passes) or red (would be blocked). Never blocks anything itself. */
class TestActivity : ComponentActivity() {
    companion object {
        private const val EXTRA = "profile_json"
        fun intent(c: Context, p: GhostProfile) = Intent(c, TestActivity::class.java).putExtra(EXTRA, ProfileJson.toJson(p))
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profile = ProfileJson.fromJson(intent.getStringExtra(EXTRA) ?: run { finish(); return })
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        setContent {
            Version5Theme {
                Box(Modifier.fillMaxSize()) {
                    AndroidView(factory = { TestView(it, profile) }, modifier = Modifier.fillMaxSize())
                    Row(Modifier.align(Alignment.TopCenter).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Auto" to -1, "Port." to ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                            "Land." to ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                            "Rev." to ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE).forEach { (l, m) ->
                            OutlinedButton(onClick = { requestedOrientation = m }, contentPadding = PaddingValues(8.dp)) {
                                Text(l, fontSize = 12.sp)
                            }
                        }
                        Button(onClick = { finish() }, contentPadding = PaddingValues(8.dp)) { Text("Done", fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

private class TestView(ctx: Context, private val profile: GhostProfile) : View(ctx) {
    private val zonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(80, 255, 0, 0) }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f; color = Color.RED }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 40f }
    private val points = ArrayList<Triple<Float, Float, Boolean>>()
    private var blocked = 0; private var passed = 0

    init { setBackgroundColor(Color.rgb(20, 20, 20)) }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val rot = Screen.rotation(context)
        points.clear()
        if (e.actionMasked != MotionEvent.ACTION_UP && e.actionMasked != MotionEvent.ACTION_CANCEL) {
            for (i in 0 until e.pointerCount) {
                if (e.actionMasked == MotionEvent.ACTION_POINTER_UP && i == e.actionIndex) continue
                val g = ProfileMatcher.isGhost(profile, e.getX(i), e.getY(i), rot, width.toFloat(), height.toFloat())
                points += Triple(e.getX(i), e.getY(i), g)
            }
        }
        if (e.actionMasked == MotionEvent.ACTION_DOWN) { if (points.firstOrNull()?.third == true) blocked++ else passed++ }
        invalidate(); return true
    }

    override fun onDraw(c: Canvas) {
        val rot = Screen.rotation(context)
        for (h in profile.hotspots.filter { it.enabled }) {
            val z = ZoneGeometry.toCurrentPx(h, rot, width.toFloat(), height.toFloat())
            c.drawCircle(z.cx, z.cy, z.r, zonePaint); c.drawCircle(z.cx, z.cy, z.r, ring)
        }
        for ((x, y, g) in points) { dot.color = if (g) Color.RED else Color.GREEN; c.drawCircle(x, y, 40f, dot) }
        c.drawText("Rotation ${rot * 90}°   passed $passed   blocked $blocked", 24f, height - 40f, text)
        c.drawText("Red zones = blocked. Touch inside and outside.", 24f, height / 2f, text)
    }
}
