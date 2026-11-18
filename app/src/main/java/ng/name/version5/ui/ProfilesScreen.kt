package ng.name.version5.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Intent
import ng.name.version5.capture.CaptureActivity
import ng.name.version5.capture.TestActivity
import ng.name.version5.data.Prefs
import ng.name.version5.data.ProfileStore
import ng.name.version5.ghost.GhostProfile
import ng.name.version5.service.ProtectionService

@Composable
fun ProfilesScreen() {
    val ctx = LocalContext.current
    val store = remember { ProfileStore(ctx) }
    val prefs = remember { Prefs(ctx) }
    var profiles by remember { mutableStateOf(store.list()) }
    var activeId by remember { mutableStateOf(prefs.activeProfileId) }
    var editing by remember { mutableStateOf<GhostProfile?>(null) }

    val capture = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) editing = store.loadDraft()
    }

    editing?.let { p ->
        ReviewScreen(p,
            onSave = { saved, activate ->
                store.save(saved); store.clearDraft()
                if (activate) { prefs.activeProfileId = saved.id; activeId = saved.id }
                profiles = store.list(); editing = null; ProtectionService.reload(ctx)
            },
            onDiscard = { store.clearDraft(); editing = null },
            onTest = { ctx.startActivity(TestActivity.intent(ctx, it)) })
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Ghost profiles", style = MaterialTheme.typography.headlineSmall)
        Button({ capture.launch(Intent(ctx, CaptureActivity::class.java)) }) { Text("New Ghost Watch (calibrate)") }
        if (profiles.isEmpty()) Text("No profiles yet. Run a Ghost Watch to record where the ghost touches happen.")
        profiles.forEach { p ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(p.name + if (p.id == activeId) "  ✓ active" else "", style = MaterialTheme.typography.titleMedium)
                    Text("${p.hotspots.count { it.enabled }}/${p.hotspots.size} zones · ${p.watchSeconds}s watch")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton({ prefs.activeProfileId = p.id; activeId = p.id; ProtectionService.reload(ctx) }) { Text("Activate") }
                        TextButton({ editing = p }) { Text("Edit") }
                        TextButton({ ctx.startActivity(TestActivity.intent(ctx, p)) }) { Text("Test") }
                        TextButton({
                            store.delete(p.id)
                            if (activeId == p.id) { prefs.activeProfileId = null; activeId = null }
                            profiles = store.list(); ProtectionService.reload(ctx)
                        }) { Text("Delete") }
                    }
                }
            }
        }
    }
}
