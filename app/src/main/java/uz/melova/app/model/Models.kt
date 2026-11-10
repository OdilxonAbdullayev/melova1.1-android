package uz.melova.app.model

import org.json.JSONObject

fun JSONObject.str(key: String): String = optString(key, "").takeUnless { it == "null" } ?: ""

data class MelovaTrack(
    val id: String, val ownerId: String, val title: String, val artist: String,
    val genre: String, val description: String, val filePath: String,
    val status: String, val fileSize: Long = 0, val coverPath: String = "",
    val isDemo: Boolean = false
) {
    companion object {
        fun parse(j: JSONObject) = MelovaTrack(
            j.str("id"), j.str("owner_id"), j.str("title"), j.str("artist"),
            j.str("genre"), j.str("description"), j.str("file_path"),
            j.str("status"), j.optLong("file_size"), j.str("cover_path")
        )
    }
}

data class MelovaPlaylist(
    val id: String, val ownerId: String, val name: String,
    val description: String, val isPublic: Boolean
) {
    companion object {
        fun parse(j: JSONObject) = MelovaPlaylist(j.str("id"),j.str("owner_id"),j.str("name"),j.str("description"),j.optBoolean("is_public"))
    }
}

data class MelovaReport(val id: String, val trackId: String, val reason: String, val state: String) {
    companion object {
        fun parse(j: JSONObject) = MelovaReport(j.str("id"),j.str("track_id"),j.str("reason"),j.str("state"))
    }
}

data class MelovaProfile(val id: String, val username: String, val displayName: String, val bio: String) {
    companion object {
        fun parse(j: JSONObject) = MelovaProfile(j.str("id"),j.str("username"),j.str("display_name"),j.str("bio"))
    }
}

data class MelovaSession(val accessToken:String,val refreshToken:String,val userId:String,val expiresAt:Long)

data class MelovaData(
    val tracks:List<MelovaTrack>, val playlists:List<MelovaPlaylist>,
    val favorites:Set<String>, val history:List<String>,
    val reports:List<MelovaReport>, val profile:MelovaProfile?,val isAdmin:Boolean
)

val melovaGenres = listOf("Hammasi", "Sokin", "Energiya", "Romantik", "Instrumental", "Pop", "Milliy", "Boshqa")
