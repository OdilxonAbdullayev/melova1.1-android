package uz.melova.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import uz.melova.app.BuildConfig
import uz.melova.app.model.*
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

class MelovaApi(private val context:Context) {
    private val prefs=context.getSharedPreferences("melova.config.v1",Context.MODE_PRIVATE)
    private val vault=Vault(context)
    private val refreshLock=Mutex()
    var session:MelovaSession?=readSession();private set
    // One shared Supabase project is included in the APK at build time.
    // A bundled configuration always wins over old per-device manual settings.
    private val bundledUrl = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    private val bundledKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
    val preconfigured:Boolean get() = bundledUrl.matches(Regex("https://[a-zA-Z0-9-]+\\.supabase\\.co")) &&
        bundledKey.length>20 && !bundledKey.contains("service_role") && !bundledKey.startsWith("sb_secret_")
    val url:String get() = if(preconfigured) bundledUrl else prefs.getString("url",null)?.takeIf { it.isNotBlank() } ?: ""
    val key:String get() = if(preconfigured) bundledKey else prefs.getString("key",null)?.takeIf { it.isNotBlank() } ?: ""
    val configured:Boolean get() = url.matches(Regex("https://[a-zA-Z0-9-]+\\.supabase\\.co")) && key.length>20 &&
        !key.contains("service_role") && !key.startsWith("sb_secret_")
    val userId:String? get() = session?.userId

    init {
        // Important for updates: discard a session created against a different project.
        val manualUrl = prefs.getString("url",null)?.trim()?.trimEnd('/')
        val lastUrl = prefs.getString("active_project_url",null)?.trim()?.trimEnd('/') ?: manualUrl
        if (!lastUrl.isNullOrBlank() && lastUrl != url) {
            session = null
            vault.remove("session")
        }
        if (preconfigured) {
            prefs.edit().remove("url").remove("key").putString("active_project_url",url).apply()
        } else if (url.isNotBlank()) {
            prefs.edit().putString("active_project_url",url).apply()
        }
    }

    fun configure(rawUrl:String,rawKey:String) {
        check(!preconfigured) { "MELOVA serveri ilovada avtomatik sozlangan." }
        val value=rawUrl.trim().trimEnd('/')
        require(value.matches(Regex("https://[a-zA-Z0-9-]+\\.supabase\\.co"))) { "Supabase Project URL noto‘g‘ri." }
        require(rawKey.trim().length>20) { "Faqat publishable/anon key kerak." }
        require(!rawKey.contains("service_role") && !rawKey.startsWith("sb_secret_")) { "Secret/service_role kalitni ilovaga kiritmang." }
        session=null;vault.remove("session")
        prefs.edit().putString("url",value).putString("key",rawKey.trim())
            .putString("active_project_url",value).apply()
    }
    private fun readSession():MelovaSession? = try {
        vault.get("session")?.let { j -> JSONObject(j).let {
            MelovaSession(it.str("access"),it.str("refresh"),it.str("uid"),it.optLong("expiry"))
        }}
    } catch (_:Exception) { null }
    private fun saveSession(s:MelovaSession?) {
        session=s
        if(s==null){vault.remove("session");return}
        vault.set("session",JSONObject().put("access",s.accessToken).put("refresh",s.refreshToken)
            .put("uid",s.userId).put("expiry",s.expiresAt).toString())
    }
    fun signOutLocal()=saveSession(null)
    private fun enc(s:String)=URLEncoder.encode(s,"UTF-8")

    private fun call(method:String,path:String,body:ByteArray?=null,mime:String="application/json",authenticated:Boolean=true,
        additional:Map<String,String> = emptyMap()):String {
        check(configured) { "Supabase ulanmagan." }
        val conn=(URL(url+path).openConnection() as HttpURLConnection)
        try {
            conn.requestMethod=method
            conn.connectTimeout=20000;conn.readTimeout=50000
            conn.setRequestProperty("apikey",key)
            val token=if(authenticated) session?.accessToken else null
            if(token!=null) conn.setRequestProperty("Authorization","Bearer $token")
            else if(!key.startsWith("sb_publishable_")) conn.setRequestProperty("Authorization","Bearer $key")
            conn.setRequestProperty("Accept","application/json")
            conn.setRequestProperty("Content-Type",mime)
            for((k,v) in additional) conn.setRequestProperty(k,v)
            if(body!=null){conn.doOutput=true;conn.setFixedLengthStreamingMode(body.size);conn.outputStream.use { it.write(body) }}
            val code=conn.responseCode
            val text=(if(code in 200..299)conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if(code !in 200..299) {
                val details=try{JSONObject(text)}catch(_:Exception){JSONObject()}
                val message=listOf("msg","error_description","message","error","code").map { details.str(it) }.firstOrNull{it.isNotBlank()} ?: "HTTP $code"
                throw IllegalStateException(message)
            }
            return text
        } finally {conn.disconnect()}
    }
    private fun json(method:String,path:String,payload:JSONObject?=null,auth:Boolean=true,additional:Map<String,String> = emptyMap()):String =
        call(method,path,payload?.toString()?.toByteArray(Charsets.UTF_8),authenticated=auth,additional=additional)

    private suspend fun ensureToken() = refreshLock.withLock {
        val current=session ?: return@withLock
        if(System.currentTimeMillis()/1000 < current.expiresAt-90) return@withLock
        val response=withContext(Dispatchers.IO) {
            json("POST","/auth/v1/token?grant_type=refresh_token",JSONObject().put("refresh_token",current.refreshToken),auth=false)
        }
        saveSession(parseSession(JSONObject(response)))
    }
    private fun parseSession(j:JSONObject):MelovaSession {
        val access=j.str("access_token")
        val refresh=j.str("refresh_token")
        val userId=j.optJSONObject("user")?.str("id")?.takeIf{it.isNotBlank()}
            ?: session?.userId ?: ""
        check(access.isNotBlank()&&refresh.isNotBlank()&&userId.isNotBlank()) { "Sessiya ma’lumotlari to‘liq emas." }
        return MelovaSession(access,refresh,userId,System.currentTimeMillis()/1000+j.optLong("expires_in",3600))
    }
    private suspend fun request(method:String,path:String,payload:JSONObject?=null,authenticated:Boolean=true,extra:Map<String,String> = emptyMap()):String {
        if(authenticated)ensureToken()
        return withContext(Dispatchers.IO) { json(method,path,payload,authenticated,extra) }
    }
    suspend fun signIn(email:String,password:String) {
        val result=request("POST","/auth/v1/token?grant_type=password",JSONObject().put("email",email.trim()).put("password",password),false)
        saveSession(parseSession(JSONObject(result)))
    }
    suspend fun signUp(email:String,password:String):Boolean {
        val result=request("POST","/auth/v1/signup",JSONObject().put("email",email.trim()).put("password",password),false)
        val j=JSONObject(result)
        if(j.str("access_token").isNotBlank()) { saveSession(parseSession(j));return true }
        return false // Email verification required.
    }
    suspend fun resetPassword(email:String) {
        request("POST","/auth/v1/recover?redirect_to=${enc("uzmelova://auth/callback")}",JSONObject().put("email",email.trim()),false)
    }
    suspend fun changePassword(password:String) { request("PUT","/auth/v1/user",JSONObject().put("password",password)) }
    suspend fun signOut() {
        try { request("POST","/auth/v1/logout",JSONObject()) } finally {saveSession(null)}
    }
    /** External browser + PKCE. Never request Google login within embedded WebView. */
    fun googleLoginUri():Uri {
        check(configured) { "Supabase ulanmagan." }
        val bytes=ByteArray(32).also { SecureRandom().nextBytes(it) }
        val verifier=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val digest=MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
        val challenge=Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        vault.set("pkce",verifier)
        return Uri.parse("$url/auth/v1/authorize?provider=google&redirect_to=${enc("uzmelova://auth/callback")}&code_challenge=${enc(challenge)}&code_challenge_method=s256")
    }
    suspend fun completeRecovery(callback:Uri):Boolean {
        val frag=callback.encodedFragment ?: return false
        val params=Uri.parse("https://callback.local/?$frag")
        val kind=params.getQueryParameter("type")
        if(kind!="recovery")return false
        val access=params.getQueryParameter("access_token") ?: error("Tiklash tokeni yo‘q.")
        val refresh=params.getQueryParameter("refresh_token") ?: error("Tiklash sessiyasi yo‘q.")
        val ttl=params.getQueryParameter("expires_in")?.toLongOrNull() ?: 3600
        // Validate this token against OUR configured Supabase project before saving it.
        saveSession(MelovaSession(access,refresh,"pending",System.currentTimeMillis()/1000+ttl))
        try {
            val j=JSONObject(request("GET","/auth/v1/user"))
            val uid=j.str("id")
            require(uid.isNotBlank()) {"Token tasdiqlanmadi."}
            saveSession(MelovaSession(access,refresh,uid,System.currentTimeMillis()/1000+ttl))
        } catch(e:Exception) {saveSession(null);throw e}
        return true
    }
    suspend fun completeOAuth(callback:Uri) {
        require(callback.scheme=="uzmelova"&&callback.host=="auth"&&callback.path=="/callback") {"Noto‘g‘ri callback."}
        val err=callback.getQueryParameter("error_description") ?: callback.getQueryParameter("error")
        if(!err.isNullOrBlank()) throw IllegalStateException(err)
        val code=callback.getQueryParameter("code") ?: throw IllegalStateException("OAuth kodi kelmadi.")
        val verifier=vault.get("pkce") ?: throw IllegalStateException("OAuth boshlanmagan.")
        val result=request("POST","/auth/v1/token?grant_type=pkce",JSONObject().put("auth_code",code).put("code_verifier",verifier),false)
        saveSession(parseSession(JSONObject(result)))
        vault.remove("pkce")
    }
    private suspend fun rows(table:String,query:String="",fields:String="*"):JSONArray {
        val q="?select=${enc(fields)}"+if(query.isBlank())"" else "&$query"
        return JSONArray(request("GET","/rest/v1/$table$q"))
    }
    private inline fun <T> JSONArray.items(transform:(JSONObject)->T):List<T> = (0 until length()).map { transform(getJSONObject(it)) }
    private fun JSONObject.body()=this
    private suspend fun update(table:String,idColumn:String,id:String,data:JSONObject) = request("PATCH","/rest/v1/$table?${idColumn}=eq.${enc(id)}",data)
    suspend fun load():MelovaData {
        if(!configured) return MelovaData(emptyList(),emptyList(),emptySet(),emptyList(),emptyList(),null,false)
        val tracks=rows("tracks","order=created_at.desc&limit=350").items(MelovaTrack::parse)
        val playlists=rows("playlists","order=created_at.desc&limit=150").items(MelovaPlaylist::parse)
        val uid=userId ?: return MelovaData(tracks,playlists,emptySet(),emptyList(),emptyList(),null,false)
        val profile=rows("profiles","id=eq.${enc(uid)}&limit=1").let { if(it.length()>0) MelovaProfile.parse(it.getJSONObject(0)) else null }
        val admin=rows("admins","id=eq.${enc(uid)}&limit=1").length()>0
        val favorites=rows("favorites","user_id=eq.${enc(uid)}&limit=500","track_id").items{it.str("track_id")}.toSet()
        val history=rows("listening_history","user_id=eq.${enc(uid)}&order=played_at.desc&limit=100","track_id").items{it.str("track_id")}
        val reports=if(admin) rows("reports","order=created_at.desc&limit=100").items(MelovaReport::parse) else emptyList()
        return MelovaData(tracks,playlists,favorites,history,reports,profile,admin)
    }
    suspend fun signedAudio(track:MelovaTrack):String {
        if(track.isDemo) throw IllegalArgumentException("Demo audio local asset bilan ochiladi.")
        val value=request("POST","/storage/v1/object/sign/melova-audio/${track.filePath}",JSONObject().put("expiresIn",3600))
        val path=JSONObject(value).str("signedURL").ifBlank {JSONObject(value).str("signedUrl")}
        check(path.isNotBlank()) { "Audio URL qaytmadi." }
        return if(path.startsWith("http"))path else if(path.startsWith("/storage/v1/")) "$url$path" else "$url/storage/v1${if(path.startsWith('/')) "" else "/"}$path"
    }
    suspend fun toggleFavorite(trackId:String,has:Boolean) {
        val uid=userId ?: error("Kirish kerak.")
        if(has) request("DELETE","/rest/v1/favorites?user_id=eq.${enc(uid)}&track_id=eq.${enc(trackId)}")
        else request("POST","/rest/v1/favorites",JSONObject().put("user_id",uid).put("track_id",trackId))
    }
    suspend fun logListen(trackId:String) {
        val uid=userId ?: return
        request("POST","/rest/v1/listening_history",JSONObject().put("user_id",uid).put("track_id",trackId))
    }
    suspend fun clearHistory() {userId?.let{request("DELETE","/rest/v1/listening_history?user_id=eq.${enc(it)}")}}
    suspend fun createPlaylist(name:String,description:String,pub:Boolean) {
        val uid=userId ?: error("Kirish kerak.")
        request("POST","/rest/v1/playlists",JSONObject().put("owner_id",uid).put("name",name).put("description",description).put("is_public",pub))
    }
    suspend fun editPlaylist(id:String,name:String,description:String,pub:Boolean) {
        update("playlists","id",id,JSONObject().put("name",name).put("description",description).put("is_public",pub))
    }
    suspend fun deletePlaylist(id:String) { request("DELETE","/rest/v1/playlists?id=eq.${enc(id)}") }
    suspend fun playlistTracks(id:String):List<String> = rows("playlist_tracks","playlist_id=eq.${enc(id)}&order=added_at.asc","track_id").items { it.str("track_id") }
    suspend fun addToPlaylist(playlistId:String,trackId:String) {
        request("POST","/rest/v1/playlist_tracks",JSONObject().put("playlist_id",playlistId).put("track_id",trackId))
    }
    suspend fun removeFromPlaylist(playlistId:String,trackId:String) {
        request("DELETE","/rest/v1/playlist_tracks?playlist_id=eq.${enc(playlistId)}&track_id=eq.${enc(trackId)}")
    }
    suspend fun saveProfile(username:String,displayName:String,bio:String) {
        val uid=userId ?: error("Kirish kerak.")
        update("profiles","id",uid,JSONObject().put("username",username).put("display_name",displayName).put("bio",bio))
    }
    suspend fun submitForReview(id:String) {
        update("tracks","id",id,JSONObject().put("status","pending")
            .put("copyright_attested_at",java.time.Instant.now().toString()))
    }
    suspend fun cancelReview(id:String) {update("tracks","id",id,JSONObject().put("status","private"))}
    suspend fun moderate(id:String,approve:Boolean,note:String) {
        update("tracks","id",id,JSONObject().put("status",if(approve)"approved" else "rejected")
            .put("review_note",if(approve)JSONObject.NULL else note)
            .put("reviewed_by",userId)
            .put("reviewed_at",java.time.Instant.now().toString()))
    }
    suspend fun unpublish(id:String) { moderate(id,false,"Moderator tomonidan ommaviy katalogdan olib tashlandi") }
    suspend fun report(id:String,reason:String) {
        val uid=userId ?: error("Kirish kerak.")
        request("POST","/rest/v1/reports",JSONObject().put("reporter_id",uid).put("track_id",id).put("reason",reason))
    }
    suspend fun resolveReport(id:String){update("reports","id",id,JSONObject().put("state","resolved"))}
    suspend fun deleteTrack(track:MelovaTrack) {
        request("DELETE","/storage/v1/object/melova-audio",JSONObject().put("prefixes",JSONArray().put(track.filePath)))
        request("DELETE","/rest/v1/tracks?id=eq.${enc(track.id)}")
    }
    suspend fun uploadTrack(file:Uri,title:String,artist:String,genre:String,description:String,publicRequest:Boolean) {
        val uid=userId ?: error("Kirish kerak.")
        val content=context.contentResolver
        val filename=content.query(file,null,null,null,null)?.use{cursor->
            val idx=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if(idx>=0 && cursor.moveToFirst())cursor.getString(idx) else null
        } ?: (file.lastPathSegment?:"")
        val ext=filename.substringAfterLast('.',"").lowercase()
        val mime=mapOf("mp3" to "audio/mpeg","m4a" to "audio/mp4","wav" to "audio/wav","ogg" to "audio/ogg")[ext]
            ?: error("Faqat MP3, M4A, WAV yoki OGG fayl mumkin.")
        val bytes=withContext(Dispatchers.IO) {
            val stream=content.openInputStream(file) ?: error("Audio faylni o‘qib bo‘lmadi.")
            stream.use { input ->
                val out=ByteArrayOutputStream();val buffer=ByteArray(64*1024);var size=0
                while(true){val n=input.read(buffer);if(n<0)break;size+=n;require(size<=20*1024*1024){"Audio 20 MB dan katta."};out.write(buffer,0,n)}
                require(size>0){"Audio fayl bo‘sh."};out.toByteArray()
            }
        }
        val path="$uid/${UUID.randomUUID()}.$ext"
        ensureToken()
        withContext(Dispatchers.IO) {
            call("POST","/storage/v1/object/melova-audio/$path",bytes,mime,additional=mapOf("x-upsert" to "false","cache-control" to "3600"))
        }
        try {
            val body=JSONObject().put("owner_id",uid).put("title",title).put("artist",artist)
                .put("genre",genre).put("description",description).put("file_path",path)
                .put("file_size",bytes.size).put("mime_type",mime).put("status","private")
            val inserted=JSONArray(request("POST","/rest/v1/tracks",body,extra=mapOf("Prefer" to "return=representation")))
            val id=inserted.getJSONObject(0).str("id")
            if(publicRequest){
                try { submitForReview(id) } catch(e:Exception) { throw IllegalStateException("Trek saqlandi, lekin tekshiruvga yuborilmadi: ${e.message}") }
            }
        } catch(e:Exception) {
            // Do not delete a successfully inserted song on a later moderation failure.
            if(!e.message.orEmpty().startsWith("Trek saqlandi")) {
                try {request("DELETE","/storage/v1/object/melova-audio",JSONObject().put("prefixes",JSONArray().put(path)))}catch(_:Exception){}
            }
            throw e
        }
    }
}
