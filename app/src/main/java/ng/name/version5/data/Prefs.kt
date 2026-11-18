package ng.name.version5.data

import android.content.Context

class Prefs(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("ghostguard", Context.MODE_PRIVATE)

    private fun bool(k: String, d: Boolean) = p.getBoolean(k, d)
    private fun put(k: String, v: Boolean) = p.edit().putBoolean(k, v).apply()

    var protectionOn: Boolean get() = bool("on", false); set(v) = put("on", v)
    var zonesPaused: Boolean get() = bool("paused", false); set(v) = put("paused", v)
    var showZones: Boolean get() = bool("showZones", false); set(v) = put("showZones", v)
    var shakeEnabled: Boolean get() = bool("shake", true); set(v) = put("shake", v)
    var stormLock: Boolean get() = bool("storm", true); set(v) = put("storm", v)

    /** Shake strength in g needed per spike (rest = 1 g). Lower = more sensitive. */
    var shakeThresholdG: Float
        get() = p.getFloat("shakeG", 2.7f); set(v) = p.edit().putFloat("shakeG", v).apply()
    var stormLimit: Int
        get() = p.getInt("stormLimit", 15); set(v) = p.edit().putInt("stormLimit", v).apply()
    var watchSeconds: Int
        get() = p.getInt("watchSec", 30); set(v) = p.edit().putInt("watchSec", v).apply()
    var blockedCount: Long
        get() = p.getLong("blocked", 0L); set(v) = p.edit().putLong("blocked", v).apply()
    var activeProfileId: String?
        get() = p.getString("activeId", null); set(v) = p.edit().putString("activeId", v).apply()
}
