package ng.name.version5.ghost

import org.json.JSONArray
import org.json.JSONObject

/** JSON (de)serialisation using the built-in org.json, so no extra dependency is needed on API 21. */
object ProfileJson {
    fun toJson(p: GhostProfile): String = JSONObject().apply {
        put("version", p.version); put("id", p.id); put("name", p.name)
        put("createdAt", p.createdAt); put("watchSeconds", p.watchSeconds)
        put("w", p.naturalWidthPx); put("h", p.naturalHeightPx); put("xdpi", p.xdpi.toDouble())
        put("hotspots", JSONArray().also { a ->
            p.hotspots.forEach { a.put(JSONObject().apply {
                put("cx", it.cx.toDouble()); put("cy", it.cy.toDouble()); put("r", it.r.toDouble())
                put("hits", it.hits); put("major", it.avgMajor.toDouble()); put("on", it.enabled)
            }) }
        })
    }.toString()

    fun fromJson(s: String): GhostProfile {
        val o = JSONObject(s); val a = o.getJSONArray("hotspots")
        return GhostProfile(
            id = o.getString("id"), name = o.getString("name"), createdAt = o.getLong("createdAt"),
            watchSeconds = o.getInt("watchSeconds"), naturalWidthPx = o.getInt("w"),
            naturalHeightPx = o.getInt("h"), xdpi = o.getDouble("xdpi").toFloat(),
            hotspots = (0 until a.length()).map {
                val h = a.getJSONObject(it)
                Hotspot(h.getDouble("cx").toFloat(), h.getDouble("cy").toFloat(), h.getDouble("r").toFloat(),
                    h.getInt("hits"), h.getDouble("major").toFloat(), h.optBoolean("on", true))
            },
            version = o.optInt("version", 1),
        )
    }
}
