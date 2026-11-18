package ng.name.version5.data

import android.content.Context
import ng.name.version5.ghost.GhostProfile
import ng.name.version5.ghost.ProfileJson
import java.io.File

/** Profiles live as JSON files in internal storage, always in natural-orientation space. */
class ProfileStore(ctx: Context) {
    private val dir = File(ctx.applicationContext.filesDir, "profiles").apply { mkdirs() }
    private val draft = File(ctx.applicationContext.filesDir, "draft.json")

    private fun file(id: String) = File(dir, id.filter { it.isLetterOrDigit() || it == '-' } + ".json")

    fun list(): List<GhostProfile> =
        (dir.listFiles { f -> f.extension == "json" } ?: emptyArray())
            .mapNotNull { runCatching { ProfileJson.fromJson(it.readText()) }.getOrNull() }
            .sortedByDescending { it.createdAt }

    fun get(id: String?): GhostProfile? =
        id?.let { f -> file(f).takeIf { it.exists() }?.let { runCatching { ProfileJson.fromJson(it.readText()) }.getOrNull() } }

    fun save(p: GhostProfile) = file(p.id).writeText(ProfileJson.toJson(p))
    fun delete(id: String) { file(id).delete() }

    fun saveDraft(p: GhostProfile) = draft.writeText(ProfileJson.toJson(p))
    fun loadDraft(): GhostProfile? =
        draft.takeIf { it.exists() }?.let { runCatching { ProfileJson.fromJson(it.readText()) }.getOrNull() }
    fun clearDraft() { draft.delete() }
}
