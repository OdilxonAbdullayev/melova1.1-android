package uz.melova.app.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import uz.melova.app.model.MelovaTrack
import uz.melova.app.model.MelovaViewModel

@Composable fun MelovaApp(vm:MelovaViewModel,openGoogle:(Uri)->Unit) {
    MelovaTheme {
        val snackbar=remember{SnackbarHostState()}
        var login by remember {mutableStateOf(false)}
        var config by remember {mutableStateOf(false)}
        var options by remember {mutableStateOf<MelovaTrack?>(null)}
        var playlistTarget by remember {mutableStateOf<MelovaTrack?>(null)}
        var reportTarget by remember {mutableStateOf<MelovaTrack?>(null)}
        var deleteTarget by remember {mutableStateOf<MelovaTrack?>(null)}
        LaunchedEffect(vm.messageId) {if(vm.messageId>0)snackbar.showSnackbar(vm.message)}
        LaunchedEffect(vm.controller) {while(true) { delay(600);vm.tick() }}
        BackHandler(vm.showFullPlayer || vm.selectedPlaylist!=null || vm.page!="home") {
            when {vm.showFullPlayer->vm.showFullPlayer=false;vm.selectedPlaylist!=null->vm.selectedPlaylist=null;else->vm.page="home"}
        }
        Scaffold(
            containerColor=Night,
            topBar={Column {
                Row(Modifier.fillMaxWidth().height(66.dp).padding(horizontal=18.dp),verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Filled.GraphicEq,null,tint=Aqua,modifier=Modifier.size(27.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("MELOVA",fontWeight=FontWeight.Black,fontSize=22.sp,letterSpacing=2.sp)
                    Spacer(Modifier.weight(1f))
                    if(vm.isDemo) TextButton(onClick={config=true}) {Text("DEMO",color=Aqua)}
                    IconButton(onClick={vm::refresh}){Icon(Icons.Filled.Refresh,"Yangilash",tint=Muted)}
                    IconButton(onClick={if(vm.signedIn){vm.page="profile"}else login=true}) {
                        Icon(Icons.Filled.AccountCircle,"Akkaunt",tint=Violet)
                    }
                }
                if(vm.busy) LinearProgressIndicator(modifier=Modifier.fillMaxWidth(),color=Aqua)
            }},
            bottomBar={Column {
                MiniPlayer(vm)
                NavigationBar(containerColor=CardNight,tonalElevation=0.dp) {
                    val destinations=listOf(
                        Triple("home","Kashf",Icons.Filled.Home),
                        Triple("search","Qidiruv",Icons.Filled.Search),
                        Triple("library","Kutubxona",Icons.Filled.LibraryMusic),
                        Triple("playlists","Pleylist",Icons.Filled.QueueMusic),
                        Triple("profile","Profil",Icons.Filled.Person)
                    )
                    destinations.forEach {(id,title,ico)->
                        NavigationBarItem(selected=vm.page==id,onClick={vm.page=id;vm.selectedPlaylist=null},
                            icon={Icon(ico,contentDescription=title)},label={Text(title,fontSize=10.sp)},
                            colors=NavigationBarItemDefaults.colors(selectedIconColor=Violet,selectedTextColor=Violet,
                                indicatorColor=Night,unselectedIconColor=Muted,unselectedTextColor=Muted))
                    }
                }
            }},
            snackbarHost={SnackbarHost(snackbar)}
        ) {padding->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when(vm.page){
                    "home"->HomePage(vm,{options=it},{vm.page=it})
                    "search"->SearchPage(vm){options=it}
                    "library"->LibraryPage(vm,{options=it},{vm.page=it})
                    "favorites"->FavoritesPage(vm){options=it}
                    "history"->HistoryPage(vm){options=it}
                    "playlists"->PlaylistsPage(vm){options=it}
                    "upload"->UploadPage(vm,{login=true})
                    "profile"->ProfilePage(vm,{login=true},{config=true},{vm.page=it})
                    "admin"->AdminPage(vm)
                    else->HomePage(vm,{options=it},{vm.page=it})
                }
            }
        }
        if(vm.showFullPlayer) FullPlayer(vm,onDismiss={vm.showFullPlayer=false})
        if(vm.passwordRecoveryRequired)RecoveryPasswordDialog(vm)
        if(login) AuthDialog(vm,onDismiss={login=false},onGoogle={
            try {openGoogle(vm.api.googleLoginUri());login=false}
            catch(e:Exception){vm.toast(e.message?:"Google kirishda xato")}
        })
        if(config) ConfigDialog(vm,onDismiss={config=false})
        options?.let {t-> TrackActions(vm,t,onDismiss={options=null},addPlaylist={playlistTarget=t;options=null},
            report={reportTarget=t;options=null},delete={deleteTarget=t;options=null}) }
        playlistTarget?.let{t->AddPlaylistDialog(vm,t,onDismiss={playlistTarget=null})}
        reportTarget?.let{t->ReportDialog(vm,t,onDismiss={reportTarget=null})}
        deleteTarget?.let {t ->AlertDialog(onDismissRequest={deleteTarget=null},
            title={Text("Musiqani o‘chirish?")},text={Text("${t.title} butunlay o‘chiriladi. Bu amal qaytarilmaydi.")},
            confirmButton={TextButton(onClick={vm.deleteTrack(t);deleteTarget=null}){Text("O‘chirish",color=Rose)}},
            dismissButton={TextButton(onClick={deleteTarget=null}){Text("Bekor qilish")}}) }
    }
}

@Composable private fun MiniPlayer(vm:MelovaViewModel) {
    val t=vm.activeTrack ?: return
    Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(CardNight,Night)))
        .clickable{vm.showFullPlayer=true}.padding(horizontal=12.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
        AlbumArt(t.id,Modifier.size(45.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text(t.artist,fontSize=12.sp,color=Muted,maxLines=1)
        }
        IconButton(onClick=vm::togglePlay) {
            Icon(if(vm.playing)Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                contentDescription=if(vm.playing)"Pauza" else "Ijro",tint=Aqua,modifier=Modifier.size(35.dp))
        }
        IconButton(onClick=vm::next) {Icon(Icons.Filled.SkipNext,"Keyingi")}
    }
    val progress=if(vm.playerDuration>0)(vm.playerPosition.toFloat()/vm.playerDuration).coerceIn(0f,1f)else 0f
    LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(2.dp),color=Aqua,trackColor=CardNight)
}

@Composable private fun FullPlayer(vm:MelovaViewModel,onDismiss:()->Unit) {
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.fillMaxSize().background(Night).verticalScroll(rememberScrollState()).padding(horizontal=25.dp,vertical=22.dp),
            horizontalAlignment=Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                IconButton(onClick=onDismiss){Icon(Icons.Filled.KeyboardArrowDown,"Yopish")}
                Text("HOZIR TINGLANMOQDA",Modifier.weight(1f),fontSize=12.sp,color=Muted)
                IconButton(onClick=vm::setShuffle){Icon(Icons.Filled.Shuffle,"Aralashtirish",tint=if(vm.shuffle)Aqua else Muted)}
            }
            Spacer(Modifier.height(25.dp))
            val t=vm.activeTrack
            AlbumArt(t?.id.orEmpty(),Modifier.fillMaxWidth().aspectRatio(1f),large=true)
            Spacer(Modifier.height(24.dp))
            Text(t?.title ?: "MELOVA",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
            Text(t?.artist.orEmpty(),color=Muted)
            Spacer(Modifier.height(25.dp))
            val max=vm.playerDuration.coerceAtLeast(1L)
            var drag by remember{mutableStateOf<Float?>(null)}
            Slider(value=drag ?: vm.playerPosition.coerceIn(0,max).toFloat()/max,
                onValueChange={drag=it},onValueChangeFinished={val x=drag;if(x!=null)vm.seek((x*max).toLong());drag=null},
                modifier=Modifier.fillMaxWidth(),colors=SliderDefaults.colors(thumbColor=Aqua,activeTrackColor=Aqua))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(formatMs(vm.playerPosition),color=Muted,fontSize=12.sp)
                Text(formatMs(vm.playerDuration),color=Muted,fontSize=12.sp)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceEvenly) {
                IconButton(onClick=vm::setRepeat){Icon(Icons.Filled.Repeat,"Takrorlash",tint=if(vm.repeatMode>0)Aqua else Muted)}
                IconButton(onClick=vm::previous){Icon(Icons.Filled.SkipPrevious,"Oldingi",modifier=Modifier.size(34.dp))}
                IconButton(onClick=vm::togglePlay,modifier=Modifier.size(72.dp)) {
                    Icon(if(vm.playing)Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,"Ijro/pauza",tint=Violet,modifier=Modifier.size(70.dp))
                }
                IconButton(onClick=vm::next){Icon(Icons.Filled.SkipNext,"Keyingi",modifier=Modifier.size(34.dp))}
                IconButton(onClick=vm::changeSpeed){Text("${vm.speed}×",color=Aqua,fontSize=12.sp)}
            }
            Spacer(Modifier.height(25.dp))
            SectionHeader("Ijro navbati","Treklarni ko‘chiring yoki o‘chiring")
            val player=vm.controller
            val total=player?.mediaItemCount ?: 0
            repeat(total){i->
                val media=player?.getMediaItemAt(i)
                Row(Modifier.fillMaxWidth().padding(vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Filled.MusicNote,null,tint=if(i==player?.currentMediaItemIndex)Aqua else Muted)
                    Spacer(Modifier.width(8.dp))
                    Text(media?.mediaMetadata?.title?.toString().orEmpty(),Modifier.weight(1f),maxLines=1)
                    IconButton(onClick={vm.moveQueued(i,-1)}){Icon(Icons.Filled.KeyboardArrowUp,"Yuqoriga")}
                    IconButton(onClick={vm.moveQueued(i,1)}){Icon(Icons.Filled.KeyboardArrowDown,"Pastga")}
                    IconButton(onClick={vm.removeQueued(i)}){Icon(Icons.Filled.Close,"Navbatdan olish")}
                }
            }
        }
    }
}
fun formatMs(value:Long):String {val s=value.coerceAtLeast(0L)/1000;return "%d:%02d".format(s/60,s%60)}

@Composable private fun TrackActions(vm:MelovaViewModel,t:MelovaTrack,onDismiss:()->Unit,
    addPlaylist:()->Unit,report:()->Unit,delete:()->Unit) {
    AlertDialog(onDismissRequest=onDismiss,title={Text(t.title)},
        text={Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
            ActionItem("Ijro etish",Icons.Filled.PlayArrow){vm.play(t);onDismiss()}
            ActionItem("Navbatga qo‘shish",Icons.Filled.QueueMusic){vm.addToQueue(t);onDismiss()}
            ActionItem("Sevimlilar",Icons.Filled.FavoriteBorder){vm.like(t);onDismiss()}
            if(vm.signedIn)ActionItem("Pleylistga qo‘shish",Icons.Filled.PlaylistAdd,addPlaylist)
            if(vm.signedIn&&t.ownerId==vm.userId && t.status in listOf("private","rejected")) {
                ActionItem("Admin tekshiruviga yuborish",Icons.Filled.CloudUpload){vm.submitReview(t);onDismiss()}
                ActionItem("Musiqani o‘chirish",Icons.Filled.Delete,delete)
            }
            if(vm.signedIn&&t.ownerId==vm.userId && t.status=="pending")
                ActionItem("Tekshiruvni bekor qilish",Icons.Filled.Close){vm.cancelReview(t);onDismiss()}
            if(vm.signedIn&&t.status=="approved"&&t.ownerId!=vm.userId)
                ActionItem("Shikoyat yuborish",Icons.Filled.Flag,report)
        }},confirmButton={TextButton(onClick=onDismiss){Text("Yopish")}})
}
@Composable private fun ActionItem(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=11.dp),verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,tint=Violet);Spacer(Modifier.width(13.dp));Text(label)
    }
}
@Composable private fun AddPlaylistDialog(vm:MelovaViewModel,t:MelovaTrack,onDismiss:()->Unit){
    AlertDialog(onDismissRequest=onDismiss,title={Text("Pleylistga qo‘shish")},
        text={Column {if(vm.ownPlaylists.isEmpty())Text("Avval pleylist yarating.",color=Muted)
            vm.ownPlaylists.forEach {p->ActionItem(p.name,Icons.Filled.QueueMusic){vm.addToPlaylist(p,t);onDismiss()}}
        }},confirmButton={TextButton(onClick=onDismiss){Text("Yopish")}})
}
@Composable private fun ReportDialog(vm:MelovaViewModel,t:MelovaTrack,onDismiss:()->Unit){
    var reason by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Shikoyat yuborish")},
        text={OutlinedTextField(value=reason,onValueChange={if(it.length<=600)reason=it},label={Text("Sabab, 10–600 belgi")},minLines=3)},
        confirmButton={TextButton(onClick={vm.reportTrack(t,reason);onDismiss()},enabled=reason.trim().length>=10){Text("Yuborish")}},
        dismissButton={TextButton(onClick=onDismiss){Text("Bekor qilish")}})
}

@Composable private fun RecoveryPasswordDialog(vm:MelovaViewModel) {
    var password by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest={},title={Text("Yangi parol o‘rnating")},
        text={OutlinedTextField(value=password,onValueChange={password=it},
            label={Text("Yangi parol (8+ belgi)")},singleLine=true,
            visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation())},
        confirmButton={TextButton(onClick={vm.changePassword(password)},enabled=password.length>=8&&!vm.busy){Text("Parolni saqlash")}})
}
