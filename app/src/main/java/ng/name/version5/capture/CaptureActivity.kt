package ng.name.version5.capture

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.CountDownTimer
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.name.version5.data.Prefs
import ng.name.version5.data.ProfileStore
import ng.name.version5.data.Screen
import ng.name.version5.ghost.*
import ng.name.version5.ui.theme.Version5Theme
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

/**
 * "Ghost Watch": after a get-ready countdown the phone is left alone for N seconds and every
 * touch is recorded as a ghost. Handles rotation itself (configChanges) so a watch never restarts.
 */
class CaptureActivity : ComponentActivity() {
    private enum class Phase { SETUP, READY, WATCH, DONE }

    private var phase by mutableStateOf(Phase.SETUP)
    private var secondsLeft by mutableIntStateOf(0)
    private var totalSeconds by mutableIntStateOf(5)
    private var watchSeconds by mutableIntStateOf(30)
    private var hitCount by mutableIntStateOf(0)
    private var hotspotCount by mutableIntStateOf(0)
    private var orientationIdx by mutableIntStateOf(0)

    private val samples = ArrayList<GhostSample>()
    private val lastMove = HashMap<Int, Long>()
    private var timer: CountDownTimer? = null
    private lateinit var prefs: Prefs

    private val orientations = listOf(
        "Auto" to ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
        "Portrait" to ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        "Landscape" to ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        "Rev. landscape" to ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this); watchSeconds = prefs.watchSeconds
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (phase == Phase.READY || phase == Phase.WATCH) abort() else finish()
            }
        })
        setContent { Version5Theme { Content() } }
    }

    override fun onDestroy() { timer?.cancel(); super.onDestroy() }

    // The screen can't be trusted during a watch, so hardware keys abort it.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) &&
            (phase == Phase.READY || phase == Phase.WATCH)) { abort(); return true }
        return super.onKeyDown(keyCode, event)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (phase == Phase.WATCH) { record(ev); return true }
        return super.dispatchTouchEvent(ev)
    }

    private fun record(ev: MotionEvent) {
        val rot = Screen.rotation(this); val sz = Screen.realSize(this)
        val offX = ev.rawX - ev.x; val offY = ev.rawY - ev.y
        fun add(i: Int) {
            val u = ((ev.getX(i) + offX) / sz.x).coerceIn(0f, 1f)
            val v = ((ev.getY(i) + offY) / sz.y).coerceIn(0f, 1f)
            val n = Rotation.toNatural(u, v, rot)
            samples += GhostSample(ev.eventTime, ev.getPointerId(i), n.x, n.y,
                ev.getTouchMajor(i), ev.getPressure(i), ev.getToolType(i), rot)
        }
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> add(0)
            MotionEvent.ACTION_POINTER_DOWN -> add(ev.actionIndex)
            MotionEvent.ACTION_MOVE -> for (i in 0 until ev.pointerCount) {
                val id = ev.getPointerId(i)
                if (ev.eventTime - (lastMove[id] ?: 0L) >= 100) { lastMove[id] = ev.eventTime; add(i) }
            }
        }
        hitCount = samples.size
    }

    private fun startCountdown(seconds: Int, onDone: () -> Unit) {
        timer?.cancel(); totalSeconds = seconds; secondsLeft = seconds
        timer = object : CountDownTimer(seconds * 1000L, 200) {
            override fun onTick(ms: Long) { secondsLeft = ceil(ms / 1000.0).toInt() }
            override fun onFinish() { secondsLeft = 0; onDone() }
        }.start()
    }

    private fun begin() {
        prefs.watchSeconds = watchSeconds
        samples.clear(); lastMove.clear(); hitCount = 0
        applyImmersive(true)
        phase = Phase.READY
        startCountdown(5) {
            phase = Phase.WATCH
            startCountdown(watchSeconds) { finishWatch() }
        }
    }

    private fun abort() { timer?.cancel(); applyImmersive(false); phase = Phase.SETUP }

    private fun finishWatch() {
        applyImmersive(false)
        val nat = Screen.naturalSize(this); val xdpi = Screen.xdpi(this)
        val wMm = nat.x / xdpi * 25.4f; val hMm = nat.y / xdpi * 25.4f
        val hs = HotspotAnalyzer.analyze(samples, wMm, hMm)
        hotspotCount = hs.size
        val name = "Profile " + SimpleDateFormat("MMM d HH:mm", Locale.getDefault()).format(Date())
        ProfileStore(this).saveDraft(GhostProfile(UUID.randomUUID().toString(), name,
            System.currentTimeMillis(), watchSeconds, nat.x, nat.y, xdpi, hs))
        phase = Phase.DONE
    }

    @Suppress("DEPRECATION")
    private fun applyImmersive(on: Boolean) {
        window.decorView.systemUiVisibility = if (!on) View.SYSTEM_UI_FLAG_VISIBLE else
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    @Composable
    private fun Content() {
        val active = phase == Phase.READY || phase == Phase.WATCH
        Box(
            Modifier.fillMaxSize().background(if (active) Color.Black else MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            when (phase) {
                Phase.SETUP -> Setup()
                Phase.READY, Phase.WATCH -> Countdown()
                Phase.DONE -> Done()
            }
        }
    }

    @Composable
    private fun Setup() = Column(
        Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Ghost Watch", fontSize = 28.sp)
        Text("Put the phone down and don't touch it. Every touch during the watch counts as a ghost touch.",
            textAlign = TextAlign.Center)
        Text("Duration: $watchSeconds s")
        Slider(value = watchSeconds.toFloat(), onValueChange = { watchSeconds = (it / 5).toInt() * 5 },
            valueRange = 10f..120f, steps = 21)
        Text("Orientation (test landscape too)")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            orientations.forEachIndexed { i, (label, mode) ->
                FilterChip(selected = orientationIdx == i, onClick = {
                    orientationIdx = i; requestedOrientation = mode
                }, label = { Text(label, fontSize = 12.sp) })
            }
        }
        Button(onClick = { begin() }) { Text("Start (5 s get-ready first)") }
        Text("Abort any time with a volume key or Back.", fontSize = 12.sp)
        TextButton(onClick = { finish() }) { Text("Close") }
    }

    @Composable
    private fun Countdown() {
        val ready = phase == Phase.READY
        val frac = if (totalSeconds == 0) 0f else secondsLeft / totalSeconds.toFloat()
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (ready) "Put the phone down…" else "Don't touch the phone", color = Color.White, fontSize = 22.sp)
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val s = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(Color(0xFF333333), 0f, 360f, false, style = s, size = Size(size.width, size.height))
                    drawArc(if (ready) Color(0xFFFFB300) else Color(0xFF4CAF50), -90f, 360f * frac, false,
                        style = s, topLeft = Offset.Zero, size = Size(size.width, size.height))
                }
                Text("$secondsLeft", color = Color.White, fontSize = 56.sp)
            }
            if (!ready) Text("Touches recorded: $hitCount", color = Color.White)
            Text("Volume key or Back to abort", color = Color.Gray, fontSize = 12.sp)
        }
    }

    @Composable
    private fun Done() = Column(
        Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Watch complete", fontSize = 26.sp)
        Text(if (hitCount == 0) "No ghost touches were recorded. If the problem is intermittent, try a longer watch or a different orientation."
        else "$hitCount touches recorded → $hotspotCount hotspot(s).", textAlign = TextAlign.Center)
        Button(onClick = { setResult(RESULT_OK); finish() }) { Text("Review hotspots") }
        OutlinedButton(onClick = { phase = Phase.SETUP }) { Text("Run again") }
    }
}
