package ng.name.version5.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import ng.name.version5.R
import ng.name.version5.data.Prefs

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED && Prefs(ctx).protectionOn) ProtectionService.start(ctx)
    }
}

/** Quick Settings tile (API 24+) that turns protection on or off. */
@RequiresApi(Build.VERSION_CODES.N)
class ProtectionTileService : TileService() {
    override fun onStartListening() = refresh()
    override fun onClick() {
        if (Prefs(this).protectionOn) ProtectionService.stop(this) else ProtectionService.start(this)
        refresh()
    }
    private fun refresh() {
        qsTile?.apply {
            label = "GhostGuard"
            state = if (Prefs(this@ProtectionTileService).protectionOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            if (Build.VERSION.SDK_INT >= 28) icon = Icon.createWithResource(this@ProtectionTileService, R.drawable.ic_home)
            updateTile()
        }
    }
}
