package ng.name.version5.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import ng.name.version5.MainActivity
import ng.name.version5.R
import ng.name.version5.data.Prefs
import ng.name.version5.data.ProfileStore
import ng.name.version5.data.Screen
import ng.name.version5.ghost.ZoneGeometry
import kotlin.math.sqrt

class ProtectionService : Service(), SensorEventListener {

    companion object {
        private const val CH = "ghostguard"
        private const val NID = 1
        const val ACTION_START = "ng.name.version5.START"
        const val ACTION_TOGGLE_PAUSE = "ng.name.version5.TOGGLE_PAUSE"
        const val ACTION_STOP = "ng.name.version5.STOP"
        const val ACTION_RELOAD = "ng.name.version5.RELOAD"
        @Volatile var running = false

        private fun intent(c: Context, a: String) = Intent(c, ProtectionService::class.java).setAction(a)
        fun start(c: Context) { Prefs(c).protectionOn = true; ContextCompat.startForegroundService(c, intent(c, ACTION_START)) }
        fun stop(c: Context) { Prefs(c).protectionOn = false; c.stopService(Intent(c, ProtectionService::class.java)) }
        fun reload(c: Context) { if (running) c.startService(intent(c, ACTION_RELOAD)) }
    }

    private lateinit var wm: WindowManager
    private lateinit var prefs: Prefs
    private lateinit var store: ProfileStore
    private lateinit var sm: SensorManager
    private lateinit var dm: DisplayManager
    private lateinit var pm: PowerManager
    private val handler = Handler(Looper.getMainLooper())
    private val zoneViews = mutableListOf<View>()
    private val shake = ShakeLogic()
    private val stormTimes = ArrayDeque<Long>()
    private var lastStormLock = 0L
    private var zoneCount = 0
    private var lastOverlayOk = true

    /** Overlay permission can be granted/revoked outside the app (Settings, adb), so re-read it periodically. */
    private val watchdog = object : Runnable {
        override fun run() {
            val ok = canOverlay()
            if (ok != lastOverlayOk) { lastOverlayOk = ok; rebuildZones(); updateNotification() }
            handler.postDelayed(this, 4000)
        }
    }

    private val rebuild = Runnable { rebuildZones(); updateNotification() }
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(id: Int) {}
        override fun onDisplayRemoved(id: Int) {}
        override fun onDisplayChanged(id: Int) { handler.removeCallbacks(rebuild); handler.postDelayed(rebuild, 150) }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        sm = getSystemService(SENSOR_SERVICE) as SensorManager
        dm = getSystemService(DISPLAY_SERVICE) as DisplayManager
        pm = getSystemService(POWER_SERVICE) as PowerManager
        prefs = Prefs(this); store = ProfileStore(this)
        createChannel()
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) startForeground(NID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(NID, n)
        dm.registerDisplayListener(displayListener, handler)
        lastOverlayOk = canOverlay()
        handler.postDelayed(watchdog, 4000)
        running = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { prefs.protectionOn = false; stopSelf(); return START_NOT_STICKY }
            ACTION_TOGGLE_PAUSE -> prefs.zonesPaused = !prefs.zonesPaused
        }
        rebuildZones(); configureShake(); updateNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        dm.unregisterDisplayListener(displayListener)
        sm.unregisterListener(this)
        clearZones()
        super.onDestroy()
    }

    // ---------- Zones ----------
    private fun clearZones() {
        zoneViews.forEach { runCatching { wm.removeView(it) } }
        zoneViews.clear()
    }

    private fun canOverlay() = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this)

    private fun rebuildZones() {
        clearZones(); zoneCount = 0
        if (prefs.zonesPaused || !canOverlay()) return
        val profile = store.get(prefs.activeProfileId) ?: return
        val rot = Screen.rotation(this); val size = Screen.realSize(this)
        @Suppress("DEPRECATION") val legacyType = WindowManager.LayoutParams.TYPE_PHONE
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else legacyType
        for (h in profile.hotspots.filter { it.enabled }) {
            val z = ZoneGeometry.toCurrentPx(h, rot, size.x.toFloat(), size.y.toFloat())
            val d = (z.r * 2).toInt().coerceAtLeast(8)
            val lp = WindowManager.LayoutParams(
                d, d, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP or Gravity.LEFT; x = (z.cx - z.r).toInt(); y = (z.cy - z.r).toInt() }
            val v = ZoneView()
            if (prefs.showZones) v.setBackgroundColor(Color.argb(70, 255, 0, 0))
            try { wm.addView(v, lp); zoneViews += v; zoneCount++ } catch (_: Exception) { }
        }
    }

    /** Swallows every touch that lands in its (square) bounds and counts the blocked taps. */
    private inner class ZoneView : View(this@ProtectionService) {
        override fun onTouchEvent(e: MotionEvent): Boolean {
            if (e.actionMasked == MotionEvent.ACTION_DOWN) onBlocked()
            return true
        }
    }

    private fun onBlocked() {
        prefs.blockedCount = prefs.blockedCount + 1
        if (!prefs.stormLock) return
        val now = System.currentTimeMillis()
        stormTimes.addLast(now)
        while (stormTimes.isNotEmpty() && now - stormTimes.first() > 3000) stormTimes.removeFirst()
        if (stormTimes.size >= prefs.stormLimit && now - lastStormLock > 5000) {
            lastStormLock = now; stormTimes.clear()
            ScreenLocker.lock(this)
        }
    }

    // ---------- Shake ----------
    private fun configureShake() {
        sm.unregisterListener(this)
        shake.thresholdG = prefs.shakeThresholdG
        if (prefs.shakeEnabled) {
            sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sm.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    override fun onSensorChanged(e: SensorEvent) {
        val g = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]) /
            SensorManager.GRAVITY_EARTH
        if (shake.onSample(e.timestamp / 1_000_000L, g) && pm.isInteractive) ScreenLocker.lock(this)
    }

    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    // ---------- Notification ----------
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                NotificationChannel(CH, "GhostGuard protection", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun pi(action: String, code: Int): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getService(this, code, Intent(this, ProtectionService::class.java).setAction(action), flags)
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0
        )
        val paused = prefs.zonesPaused
        return NotificationCompat.Builder(this, CH)
            .setSmallIcon(R.drawable.ic_home)
            .setContentTitle(if (paused) "GhostGuard: zones paused" else "GhostGuard active")
            .setContentText("$zoneCount zones · ${prefs.blockedCount} blocked · shake " + if (prefs.shakeEnabled) "on" else "off")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(0, if (paused) "Resume" else "Pause zones", pi(ACTION_TOGGLE_PAUSE, 1))
            .addAction(0, "Stop", pi(ACTION_STOP, 2))
            .build()
    }

    private fun updateNotification() {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NID, buildNotification())
    }
}
