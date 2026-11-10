package uz.melova.app.model

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.launch
import uz.melova.app.R
import uz.melova.app.data.MelovaApi

class MelovaViewModel(application:Application):AndroidViewModel(application) {
    val api=MelovaApi(application)
    var page by androidx.compose.runtime.mutableStateOf("home")
    var tracks by androidx.compose.runtime.mutableStateOf<List<MelovaTrack>>(emptyList());private set
    var playlists by androidx.compose.runtime.mutableStateOf<List<MelovaPlaylist>>(emptyList());private set
    var favorites by androidx.compose.runtime.mutableStateOf<Set<String>>(emptySet());private set
    var history by androidx.compose.runtime.mutableStateOf<List<String>>(emptyList());private set
    var reports by androidx.compose.runtime.mutableStateOf<List<MelovaReport>>(emptyList());private set
    var profile by androidx.compose.runtime.mutableStateOf<MelovaProfile?>(null);private set
    var isAdmin by androidx.compose.runtime.mutableStateOf(false);private set
    var busy by androidx.compose.runtime.mutableStateOf(false);private set
    var message by androidx.compose.runtime.mutableStateOf("");private set
    var messageId by androidx.compose.runtime.mutableIntStateOf(0);private set
    var search by androidx.compose.runtime.mutableStateOf("")
    var genre by androidx.compose.runtime.mutableStateOf("Hammasi")
    var controller by androidx.compose.runtime.mutableStateOf<MediaController?>(null);private set
    var activeId by androidx.compose.runtime.mutableStateOf("");private set
    var playing by androidx.compose.runtime.mutableStateOf(false);private set
    var playerPosition by androidx.compose.runtime.mutableLongStateOf(0L);private set
    var playerDuration by androidx.compose.runtime.mutableLongStateOf(0L);private set
    var shuffle by androidx.compose.runtime.mutableStateOf(false);private set
    var repeatMode by androidx.compose.runtime.mutableIntStateOf(Player.REPEAT_MODE_OFF);private set
    var speed by androidx.compose.runtime.mutableFloatStateOf(1f);private set
    var selectedPlaylist by androidx.compose.runtime.mutableStateOf<MelovaPlaylist?>(null)
    var selectedPlaylistTracks by androidx.compose.runtime.mutableStateOf<List<String>>(emptyList());private set
    var showFullPlayer by androidx.compose.runtime.mutableStateOf(false)
    var passwordRecoveryRequired by androidx.compose.runtime.mutableStateOf(false)
    private var playJob:Job?=null
    private var lastLoggedId=""
    val userId get()=api.userId
    val signedIn get()=api.userId!=null
    val isDemo get()=!api.configured
    val publicTracks get()=tracks.filter{it.status=="approved"}
    val ownTracks get()=tracks.filter{it.ownerId==userId&&signedIn}
    val activeTrack get()=tracks.find{it.id==activeId}
    val favoriteTracks get()=tracks.filter{it.id in favorites}
    val filteredTracks get()=publicTracks.filter {
        (genre=="Hammasi"||it.genre==genre) &&
        (search.isBlank()||"${it.title} ${it.artist} ${it.genre}".contains(search.trim(),ignoreCase=true))
    }
    val ownPlaylists get()=playlists.filter{it.ownerId==userId && signedIn}

    init {refresh()}
    fun toast(msg:String) {message=msg;messageId++}
    fun operation(success:String?=null,job:suspend ()->Unit) {
        viewModelScope.launch {
            busy=true
            try {job();if(success!=null)toast(success)} catch(e:Exception){toast(e.message?:"Kutilmagan xatolik")}
            finally { busy=false }
        }
    }
    private fun setDemo() {
        tracks=listOf(
            MelovaTrack("demo-0","demo","Yangi tong","MELOVA Originals","Energiya","","","approved",isDemo=true),
            MelovaTrack("demo-1","demo","Tungi osmon","MELOVA Originals","Sokin","","","approved",isDemo=true),
            MelovaTrack("demo-2","demo","Iliq lahzalar","MELOVA Originals","Romantik","","","approved",isDemo=true)
        )
        playlists=emptyList();favorites=emptySet();history=emptyList();reports=emptyList();profile=null;isAdmin=false
    }
    fun refresh() = operation {
        if(isDemo){setDemo();return@operation}
        val data=api.load()
        tracks=data.tracks;playlists=data.playlists;favorites=data.favorites;history=data.history
        reports=data.reports;profile=data.profile;isAdmin=data.isAdmin
    }
    fun configure(url:String,key:String) = operation("Supabase sozlandi.") {
        api.configure(url,key)
        controller?.run {stop();clearMediaItems()};activeId="";selectedPlaylist=null
        val d=api.load();tracks=d.tracks;playlists=d.playlists;favorites=d.favorites;history=d.history;reports=d.reports;profile=d.profile;isAdmin=d.isAdmin
    }
    fun signIn(email:String,password:String,onSuccess:()->Unit) = operation("Xush kelibsiz!") {
        api.signIn(email,password);loadFresh();onSuccess()
    }
    fun signUp(email:String,password:String,onSuccess:()->Unit) = operation {
        val loggedIn=api.signUp(email,password)
        if(loggedIn){loadFresh();onSuccess();toast("Akkaunt yaratildi.")}
        else toast("Ro‘yxatdan o‘tish xabari yuborildi. Emailingizni tasdiqlang.")
    }
    fun signOut()=operation("Akkauntdan chiqildi.") {
        api.signOut()
        controller?.run {stop();clearMediaItems()}
        activeId="";lastLoggedId=""
        loadFresh();page="home"
    }
    fun resetPassword(email:String)=operation("Parolni tiklash uchun email yuborildi.") {api.resetPassword(email)}
    fun changePassword(pass:String)=operation("Parol yangilandi."){
        require(pass.length>=8){"Yangi parol kamida 8 belgi bo‘lsin."}
        api.changePassword(pass);passwordRecoveryRequired=false
    }
    fun oauthCallback(callback:Uri)=operation {
        val recovering=api.completeRecovery(callback)
        if(!recovering)api.completeOAuth(callback)
        loadFresh()
        passwordRecoveryRequired=recovering
        toast(if(recovering)"Yangi parol o‘rnating." else "Google orqali kirildi.")
    }
    private suspend fun loadFresh() {
        if(isDemo){setDemo();return}
        val d=api.load();tracks=d.tracks;playlists=d.playlists;favorites=d.favorites;history=d.history
        reports=d.reports;profile=d.profile;isAdmin=d.isAdmin
    }
    fun like(t:MelovaTrack) {
        if(!signedIn){toast("Avval akkauntga kiring.");return}
        operation {val had=t.id in favorites;api.toggleFavorite(t.id,had)
            favorites=if(had)favorites-t.id else favorites+t.id }
    }
    fun clearHistory()=operation("Tinglash tarixi tozalandi."){api.clearHistory();history=emptyList()}
    fun createPlaylist(name:String,description:String,pub:Boolean)=operation("Pleylist yaratildi."){
        require(name.trim().isNotBlank()){"Pleylist nomi kerak."}
        api.createPlaylist(name.trim(),description.trim(),pub);loadFresh()
    }
    fun editPlaylist(id:String,name:String,description:String,pub:Boolean)=operation("Pleylist saqlandi.") {
        api.editPlaylist(id,name.trim(),description.trim(),pub);loadFresh();selectedPlaylist=playlists.find{it.id==id}
    }
    fun deletePlaylist(id:String)=operation("Pleylist o‘chirildi.") {
        api.deletePlaylist(id);selectedPlaylist=null;loadFresh()
    }
    fun openPlaylist(p:MelovaPlaylist)=operation {
        selectedPlaylist=p;selectedPlaylistTracks=api.playlistTracks(p.id)
    }
    fun addToPlaylist(p:MelovaPlaylist,t:MelovaTrack)=operation("Pleylistga qo‘shildi.") {
        api.addToPlaylist(p.id,t.id)
        if(selectedPlaylist?.id==p.id)selectedPlaylistTracks=api.playlistTracks(p.id)
    }
    fun removeFromPlaylist(id:String)=operation("Trek olib tashlandi.") {
        val p=selectedPlaylist ?: return@operation
        api.removeFromPlaylist(p.id,id);selectedPlaylistTracks=api.playlistTracks(p.id)
    }
    fun saveProfile(username:String,name:String,bio:String)=operation("Profil saqlandi.") {
        require(username.matches(Regex("[a-zA-Z0-9_]{3,24}"))){"Username 3–24 ta lotin harfi, son yoki _."}
        require(name.trim().length in 1..64){"Ism 1–64 belgi."}
        require(bio.length<=250){"Bio 250 belgidan oshmasin."}
        api.saveProfile(username,name.trim(),bio.trim());loadFresh()
    }
    fun upload(file:Uri,title:String,artist:String,genre:String,description:String,publish:Boolean,attested:Boolean,onSuccess:()->Unit) {
        operation("Musiqa yuklandi!") {
            require(title.trim().length in 1..120){"Nom 1–120 belgi bo‘lsin."}
            require(artist.trim().length in 1..100){"Ijrochi 1–100 belgi bo‘lsin."}
            require(description.length<=500){"Izoh 500 belgidan oshmasin."}
            require(!publish||attested){"Mualliflik huquqini tasdiqlang."}
            api.uploadTrack(file,title.trim(),artist.trim(),genre,description.trim(),publish)
            loadFresh();onSuccess();page="library"
        }
    }
    fun submitReview(t:MelovaTrack)=operation("Admin tekshiruviga yuborildi."){api.submitForReview(t.id);loadFresh()}
    fun cancelReview(t:MelovaTrack)=operation("So‘rov bekor qilindi."){api.cancelReview(t.id);loadFresh()}
    fun deleteTrack(t:MelovaTrack)=operation("Musiqa o‘chirildi."){api.deleteTrack(t);loadFresh()}
    fun reportTrack(t:MelovaTrack,reason:String)=operation("Shikoyat yuborildi."){
        require(reason.trim().length in 10..600){"Sabab 10–600 belgidan iborat bo‘lsin."}
        api.report(t.id,reason.trim())
    }
    fun moderate(t:MelovaTrack,approve:Boolean,note:String)=operation("Tekshirildi.") {
        if(!approve)require(note.isNotBlank()){"Rad etish sababini kiriting."}
        api.moderate(t.id,approve,note);loadFresh()
    }
    fun unpublish(t:MelovaTrack)=operation("Trek ommadan olindi."){api.unpublish(t.id);loadFresh()}
    fun resolveReport(id:String)=operation("Shikoyat yopildi."){api.resolveReport(id);loadFresh()}

    private val listener=object:Player.Listener {
        override fun onEvents(player:Player,events:Player.Events) { syncPlayer() }
    }
    fun attachController(newController:MediaController) {
        controller?.removeListener(listener)
        controller=newController
        controller?.addListener(listener)
        syncPlayer()
    }
    fun detachController() {controller?.removeListener(listener);controller=null}
    fun tick() {
        val c=controller ?:return
        playerPosition=c.currentPosition.coerceAtLeast(0)
        playerDuration=c.duration.coerceAtLeast(0)
    }
    private fun syncPlayer() {
        val c=controller ?:return
        activeId=c.currentMediaItem?.mediaId.orEmpty()
        playing=c.isPlaying
        shuffle=c.shuffleModeEnabled;repeatMode=c.repeatMode;speed=c.playbackParameters.speed
        tick()
        if(playing && activeId.isNotEmpty() && activeId!=lastLoggedId) {
            lastLoggedId=activeId
            if(signedIn&& !activeId.startsWith("demo-")) viewModelScope.launch {try {api.logListen(activeId);history=listOf(activeId)+history.take(99)}catch(_:Exception){} }
        }
    }
    private suspend fun itemFor(t:MelovaTrack):MediaItem {
        val source=if(t.isDemo) "android.resource://${getApplication<Application>().packageName}/${when(t.id){"demo-0"->R.raw.demo_1;"demo-1"->R.raw.demo_2;else->R.raw.demo_3}}" else api.signedAudio(t)
        return MediaItem.Builder().setMediaId(t.id).setUri(source).setMediaMetadata(
            MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist).setAlbumTitle("MELOVA").build()
        ).build()
    }
    fun play(t:MelovaTrack,queue:List<MelovaTrack> = listOf(t)) {
        if(controller==null){toast("Pleyer ulanishi davom etmoqda.");return}
        playJob?.cancel()
        playJob=viewModelScope.launch {
            busy=true
            try {
                val selected=(listOf(t)+queue).distinctBy{it.id}.filter {
                    it.status=="approved" || (signedIn && it.ownerId==userId) || isAdmin || it.isDemo
                }.take(30)
                val items=coroutineScope {
                    val limiter=Semaphore(4)
                    selected.map{song->async {limiter.withPermit{itemFor(song)}}}.awaitAll()
                }
                val player=controller ?:return@launch
                player.setMediaItems(items)
                player.prepare();player.play()
                lastLoggedId=""
                syncPlayer()
            } catch(e:Exception){toast("Ijro xatosi: ${e.message}")}
            finally {busy=false}
        }
    }
    fun addToQueue(t:MelovaTrack) {
        viewModelScope.launch {
            try { controller?.addMediaItem(itemFor(t)) ?: toast("Pleyer tayyor emas.") }
            catch(e:Exception){toast(e.message?:"Navbatga qo‘shib bo‘lmadi.")}
        }
    }
    fun togglePlay() { controller?.let { if(it.isPlaying)it.pause() else it.play() } }
    fun next() {controller?.seekToNextMediaItem()}
    fun previous() {controller?.seekToPreviousMediaItem()}
    fun seek(ms:Long){controller?.seekTo(ms.coerceAtLeast(0))}
    fun setShuffle(){controller?.let{it.shuffleModeEnabled=!it.shuffleModeEnabled};syncPlayer()}
    fun setRepeat(){controller?.let{it.repeatMode=(it.repeatMode+1)%3};syncPlayer()}
    fun changeSpeed(){val v=when(speed){0.75f->1f;1f->1.25f;1.25f->1.5f;1.5f->2f;else->0.75f};controller?.setPlaybackSpeed(v);syncPlayer()}
    fun removeQueued(index:Int){controller?.let{if(index in 0 until it.mediaItemCount)it.removeMediaItem(index)}}
    fun moveQueued(index:Int,delta:Int){controller?.let{val to=index+delta;if(index in 0 until it.mediaItemCount&&to in 0 until it.mediaItemCount)it.moveMediaItem(index,to)}}
    override fun onCleared() {controller?.removeListener(listener);super.onCleared()}
}
