package ng.name.version5.ui

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
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
import ng.name.version5.data.Prefs
import ng.name.version5.service.ProtectionService
import ng.name.version5.service.ScreenLocker

@Composable
fun SettingsScreen(refresh: Int) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var shake by remember { mutableStateOf(prefs.shakeEnabled) }
    var g by remember { mutableFloatStateOf(prefs.shakeThresholdG) }
    var storm by remember { mutableStateOf(prefs.stormLock) }
    var limit by remember { mutableFloatStateOf(prefs.stormLimit.toFloat()) }
    var show by remember { mutableStateOf(prefs.showZones) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(2000); tick++ } }
    val key = tick + refresh
    val overlayOk = remember(key) { canOverlay(ctx) }
    val adminOk = remember(key) { ScreenLocker.isAdminActive(ctx) }
    val a11yOk = remember(key) { ScreenLocker.isAccessibilityReady() }
    val battOk = remember(key) {
        Build.VERSION.SDK_INT < 23 ||
            (ctx.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)
    }
    fun apply() = ProtectionService.reload(ctx)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Permissions", style = MaterialTheme.typography.titleLarge)
        PermRow("Display over other apps (zones)", overlayOk) {
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        PermRow("Device admin (lock screen)", adminOk) {
            ctx.startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ScreenLocker.adminComponent(ctx))
                .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Only used to lock the screen when you shake the phone.")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        if (Build.VERSION.SDK_INT >= 28) PermRow("Accessibility (alternative lock, optional)", a11yOk) {
            ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        if (Build.VERSION.SDK_INT >= 23) PermRow("Ignore battery optimisation", battOk) {
            ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Text("These rows re-check themselves live, so permissions granted outside the app (system Settings or adb) show up within seconds.",
            style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text("Shake to sleep", style = MaterialTheme.typography.titleLarge)
        SwitchRow("Enabled", shake) { shake = it; prefs.shakeEnabled = it; apply() }
        Text("Sensitivity: needs ${"%.1f".format(g)} g per shake (lower = easier)")
        Slider(g, { g = it }, valueRange = 1.8f..4.5f, onValueChangeFinished = { prefs.shakeThresholdG = g; apply() })
        HorizontalDivider()
        Text("Ghost storm", style = MaterialTheme.typography.titleLarge)
        SwitchRow("Lock screen on ghost storm", storm) { storm = it; prefs.stormLock = it }
        Text("Trigger: ${limit.toInt()} blocked taps within 3 s")
        Slider(limit, { limit = it }, valueRange = 5f..40f, onValueChangeFinished = { prefs.stormLimit = limit.toInt() })
        HorizontalDivider()
        SwitchRow("Show zones as red tint", show) { show = it; prefs.showZones = it; apply() }
        Text("Protection restarts automatically after reboot if it was on.")
    }
}

@Composable
private fun PermRow(label: String, ok: Boolean, onClick: () -> Unit) =
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text((if (ok) "✓ " else "✗ ") + label, Modifier.weight(1f))
        if (!ok) Button(onClick) { Text("Grant") }
    }

@Composable
private fun SwitchRow(label: String, v: Boolean, onChange: (Boolean) -> Unit) =
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f)); Switch(v, onChange)
    }
