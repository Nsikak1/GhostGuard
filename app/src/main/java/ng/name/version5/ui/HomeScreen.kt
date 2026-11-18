package ng.name.version5.ui

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ng.name.version5.capture.TestActivity
import ng.name.version5.data.Prefs
import ng.name.version5.data.ProfileStore
import ng.name.version5.service.ProtectionService
import ng.name.version5.service.ScreenLocker

fun canOverlay(ctx: Context) = Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(ctx)

@Composable
fun HomeScreen(refresh: Int) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    val store = remember { ProfileStore(ctx) }
    var on by remember(refresh) { mutableStateOf(prefs.protectionOn) }
    var paused by remember(refresh) { mutableStateOf(prefs.zonesPaused) }
    val profile = remember(refresh, on) { store.get(prefs.activeProfileId) }
    // Re-read permissions every 2 s: they may be granted outside the app (system Settings / adb).
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(2000); tick++ } }
    val overlayOk = remember(tick, refresh) { canOverlay(ctx) }
    val lockOk = remember(tick, refresh) { ScreenLocker.canLock(ctx) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("GhostGuard", style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (on) "Protection running" else "Protection off", Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium)
                    Switch(on, { want ->
                        if (want) ProtectionService.start(ctx) else ProtectionService.stop(ctx)
                        on = want
                    })
                }
                Text("Active profile: ${profile?.name ?: "none"} (${profile?.hotspots?.count { it.enabled } ?: 0} zones)")
                Text("Blocked ghost taps: ${prefs.blockedCount}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pause zones", Modifier.weight(1f))
                    Switch(paused, { prefs.zonesPaused = it; paused = it; ProtectionService.reload(ctx) })
                }
            }
        }
        if (!overlayOk) Warn("Zones need the 'Display over other apps' permission. Open Settings tab.")
        if (!lockOk) Warn("Shake-to-sleep needs Device admin (or Accessibility on Android 9+). Open Settings tab.")
        if (profile == null) Warn("No active profile. Run a Ghost Watch in the Profiles tab.")
        OutlinedButton(onClick = { profile?.let { ctx.startActivity(TestActivity.intent(ctx, it)) } }, enabled = profile != null) {
            Text("Test active profile")
        }
    }
}

@Composable
fun Warn(msg: String) = Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
    containerColor = MaterialTheme.colorScheme.errorContainer)) { Text(msg, Modifier.padding(12.dp)) }
