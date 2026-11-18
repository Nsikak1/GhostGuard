package ng.name.version5.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import ng.name.version5.ghost.GhostProfile
import ng.name.version5.ghost.Hotspot
import ng.name.version5.ghost.ZoneGeometry

/** Shows hotspots over a phone outline (natural orientation). Tap empty space to add a zone. */
@Composable
fun ReviewScreen(
    initial: GhostProfile,
    onSave: (GhostProfile, Boolean) -> Unit,
    onDiscard: () -> Unit,
    onTest: (GhostProfile) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    val zones = remember { mutableStateListOf<Hotspot>().apply { addAll(initial.hotspots) } }
    val aspect = initial.naturalWidthPx.toFloat() / initial.naturalHeightPx
    fun current() = initial.copy(name = name, hotspots = zones.toList())

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Profile name") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())
        Text("Tap the screen outline to add a zone. Keep zones few: they also block real touches.")
        Canvas(Modifier.fillMaxWidth(0.55f).aspectRatio(aspect).align(Alignment.CenterHorizontally)
            .pointerInput(zones.size) {
                detectTapGestures { o ->
                    zones.add(Hotspot(o.x / size.width, o.y / size.height, 0.06f, 0, 0f))
                }
            }) {
            drawRect(Color(0xFF222222), size = size)
            drawRect(Color.Gray, size = size, style = Stroke(3f))
            zones.forEach { h ->
                val z = ZoneGeometry.toCurrentPx(h, 0, size.width, size.height)
                val c = if (h.enabled) Color(0xAAFF3030) else Color(0x55888888)
                drawCircle(c, z.r, Offset(z.cx, z.cy))
            }
        }
        if (zones.isEmpty()) Text("No hotspots found. You can save an empty profile or add zones manually.")
        zones.forEachIndexed { i, h ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Zone ${i + 1} · ${h.hits} hits", Modifier.weight(1f))
                        Switch(h.enabled, { zones[i] = h.copy(enabled = it) })
                        TextButton({ zones.removeAt(i) }) { Text("Delete") }
                    }
                    Text("Size")
                    Slider(h.r, { zones[i] = h.copy(r = it) }, valueRange = 0.015f..0.2f)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ onTest(current()) }) { Text("Test") }
            OutlinedButton(onDiscard) { Text("Discard") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ onSave(current(), true) }) { Text("Save & activate") }
            OutlinedButton({ onSave(current(), false) }) { Text("Save") }
        }
    }
}
