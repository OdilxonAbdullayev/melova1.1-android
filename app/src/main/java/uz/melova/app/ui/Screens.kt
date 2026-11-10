package uz.melova.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.melova.app.model.*

private val inset=PaddingValues(start=17.dp,end=17.dp,top=10.dp,bottom=23.dp)

@Composable fun HomePage(vm:MelovaViewModel,onMenu:(MelovaTrack)->Unit,go:(String)->Unit) {
    LazyColumn(contentPadding=inset,verticalArrangement=Arrangement.spacedBy(22.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF4A2F85),Color(0xFF1C5663),CardNight)),RoundedCornerShape(28.dp))
                .padding(24.dp)) {
                Text("✦ SENING MUSIQANG · SENING OLAMING",fontSize=10.sp,letterSpacing=1.sp,color=Aqua)
                Spacer(Modifier.height(12.dp))
                Text("Har lahzangga\no‘z ohangi.",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.ExtraBold,lineHeight=38.sp)
                Spacer(Modifier.height(10.dp))
                Text("Sevimli ohanglaringni top, tingla va shaxsiy to‘plamingni yarat.",color=Color.White.copy(alpha=.78f))
                Spacer(Modifier.height(17.dp))
                Button(onClick={go("search")}) {Icon(Icons.Filled.PlayArrow,null);Spacer(Modifier.width(5.dp));Text("Tinglashni boshlash")}
            }
        }
        if(vm.isDemo)item {CardPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Filled.Info,null,tint=Aqua);Spacer(Modifier.width(10.dp))
                Column {Text("Demo rejim",fontWeight=FontWeight.Bold);Text("Uchta asl demo audio mavjud. Onlayn katalog uchun Supabase ulang.",fontSize=12.sp,color=Muted)}
            }
        }}
        item {
            SectionHeader("Kayfiyatingni tanla","Har lahza uchun boshqa ohang")
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                listOf("Sokin","Energiya","Romantik","Milliy").forEach {tag->
                    AssistChip(onClick={vm.genre=tag;vm.page="search"},label={Text(tag)})
                }
            }
        }
        item {
            SectionHeader("Yangi musiqalar","Ommaviy katalogdan yangi qo‘shiqlar","Barchasi") {go("search")}
            TrackList(vm.publicTracks.take(8),vm,onMenu)
        }
        item {
            SectionHeader("Sening kutubxonang","Bitta joyda barcha to‘plamlaring")
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                SmallLaunch("Sevimlilar",Icons.Filled.Favorite,Modifier.weight(1f)){go("favorites")}
                SmallLaunch("Tinglash tarixi",Icons.Filled.History,Modifier.weight(1f)){go("history")}
            }
        }
        item {
            SectionHeader("Ommaviy pleylistlar",action="Hammasi") {go("playlists")}
            val ps=vm.playlists.filter{it.isPublic}.take(4)
            if(ps.isEmpty())TextEmpty("Pleylistlar yo‘q","Birinchi pleylistni yarating.")
            ps.forEach{p->PlaylistCard(p){vm.openPlaylist(p);go("playlists")}}
        }
    }
}
@Composable private fun SmallLaunch(title:String,image:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier,onClick:()->Unit){
    CardPanel(modifier.clickable(onClick=onClick)){
        Icon(image,null,tint=Violet);Spacer(Modifier.height(14.dp));Text(title,fontWeight=FontWeight.SemiBold,fontSize=13.sp)
    }
}
@Composable fun SearchPage(vm:MelovaViewModel,onMenu:(MelovaTrack)->Unit) {
    LazyColumn(contentPadding=inset,verticalArrangement=Arrangement.spacedBy(15.dp)) {
        item {SectionHeader("O‘z ohangingni top.","Trek, ijrochi va janr bo‘yicha qidiruv")}
        item {OutlinedTextField(value=vm.search,onValueChange={vm.search=it},modifier=Modifier.fillMaxWidth(),
            leadingIcon={Icon(Icons.Filled.Search,null)},label={Text("Musiqa yoki ijrochi...")},singleLine=true)}
        item {Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            melovaGenres.forEach {g->FilterChip(selected=vm.genre==g,onClick={vm.genre=g},label={Text(g)})}
        }}
        item {Text("${vm.filteredTracks.size} ta trek",color=Muted,fontSize=12.sp)}
        if(vm.filteredTracks.isEmpty()) item {TextEmpty("Musiqa topilmadi","Qidiruv yoki janrni o‘zgartiring.")}
        else items(vm.filteredTracks,key={it.id}) {t-> TrackRow(t,vm,vm.filteredTracks,onMenu)}
    }
}
@Composable fun LibraryPage(vm:MelovaViewModel,onMenu:(MelovaTrack)->Unit,go:(String)->Unit) {
    LazyColumn(contentPadding=inset,verticalArrangement=Arrangement.spacedBy(17.dp)) {
        item {SectionHeader("Mening kutubxonam","Yuklangan qo‘shiqlaring va to‘plamlaring")}
        item {Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            StatCard("${vm.ownTracks.size}","Musiqalar",Modifier.weight(1f))
            StatCard("${vm.favorites.size}","Sevimlilar",Modifier.weight(1f))
            StatCard("${vm.ownPlaylists.size}","Pleylist",Modifier.weight(1f))
        }}
        item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={go("favorites")},modifier=Modifier.weight(1f)){Icon(Icons.Filled.FavoriteBorder,null);Text(" Sevimlilar",fontSize=12.sp)}
            OutlinedButton(onClick={go("history")},modifier=Modifier.weight(1f)){Icon(Icons.Filled.History,null);Text(" Tarix",fontSize=12.sp)}
        }}
        item {SectionHeader("Mening musiqalarim",action="+ Yuklash") {go("upload")}}
        if(!vm.signedIn) item {TextEmpty("Akkaunt kerak","O‘zing yuklagan musiqalarni ko‘rish uchun kiring.")}
        else if(vm.ownTracks.isEmpty()) item {TextEmpty("Kutubxona bo‘sh","Birinchi musiqani yuklang.")}
        else items(vm.ownTracks,key={it.id}){t->TrackRow(t,vm,vm.ownTracks,onMenu)}
        if(vm.ownTracks.isNotEmpty())item {
            Text("Holatlar: Shaxsiy · Tekshiruvda · Ommaviy · Rad etilgan",color=Muted,fontSize=11.sp)
        }
    }
}
@Composable private fun StatCard(number:String,label:String,modifier:Modifier) {
    CardPanel(modifier) {Text(number,fontSize=24.sp,fontWeight=FontWeight.Bold,color=Aqua);Text(label,fontSize=11.sp,color=Muted)}
}
@Composable fun FavoritesPage(vm:MelovaViewModel,onMenu:(MelovaTrack)->Unit){
    LazyColumn(contentPadding=inset) {
        item {SectionHeader("Sevimli musiqalarim","Yurakcha bosgan treklar")}
        if(vm.favoriteTracks.isEmpty())item{TextEmpty("Sevimlilar bo‘sh","Trek yonidagi yurakchani bosing.")}
        else items(vm.favoriteTracks,key={it.id}){t->TrackRow(t,vm,vm.favoriteTracks,onMenu)}
    }
}
@Composable fun HistoryPage(vm:MelovaViewModel,onMenu:(MelovaTrack)->Unit){
    var confirm by remember{mutableStateOf(false)}
    val recently=vm.history.distinct().mapNotNull{id->vm.tracks.find{it.id==id}}
    LazyColumn(contentPadding=inset) {
        item { SectionHeader("Tinglash tarixi","Yaqinda tinglangan treklar",if(vm.signedIn)"Tozalash" else null){confirm=true} }
        if(recently.isEmpty())item {TextEmpty("Tarix bo‘sh","Musiqa tinglaganingizdan keyin ko‘rinadi.")}
        else items(recently,key={it.id}){t->TrackRow(t,vm,recently,onMenu)}
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Tarix tozalansinmi?")},
        text={Text("Tinglash tarixi akkauntingizdan o‘chiriladi.")},confirmButton={TextButton(onClick={vm.clearHistory();confirm=false}){Text("Tozalash")}},
        dismissButton={TextButton(onClick={confirm=false}){Text("Bekor")}})
}
@Composable fun PlaylistsPage(vm:MelovaViewModel,onMenu:(MelovaTrack)->Unit){
    var editor by remember{mutableStateOf(false)}
    var editExisting by remember{mutableStateOf<MelovaPlaylist?>(null)}
    var confirmDelete by remember{mutableStateOf(false)}
    val selected=vm.selectedPlaylist
    if(selected!=null) {
        val tr=vm.selectedPlaylistTracks.mapNotNull{id->vm.tracks.find{it.id==id}}
        LazyColumn(contentPadding=inset,verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {TextButton(onClick={vm.selectedPlaylist=null}){Icon(Icons.Filled.ArrowBack,null);Text(" Pleylistlarga qaytish")}}
            item {AlbumArt(selected.id,Modifier.fillMaxWidth().height(210.dp),large=true)}
            item {SectionHeader(selected.name,"${if(selected.isPublic)"Ommaviy" else "Shaxsiy"} · ${tr.size} trek")}
            item {Button(onClick={if(tr.isNotEmpty())vm.play(tr[0],tr)else vm.toast("Pleylist bo‘sh.")}){
                Icon(Icons.Filled.PlayArrow,null);Text(" Hammasini tinglash")}}
            if(selected.ownerId==vm.userId)item {Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick={editExisting=selected}){Text("Tahrirlash")}
                OutlinedButton(onClick={confirmDelete=true}){Text("O‘chirish",color=Rose)}
            }}
            if(tr.isEmpty()) item {TextEmpty("Pleylist bo‘sh","Trek menyusidan pleylistga qo‘shing.")}
            else items(tr,key={it.id}){t->
                TrackRow(t,vm,tr,onMenu)
                if(selected.ownerId==vm.userId)TextButton(onClick={vm.removeFromPlaylist(t.id)}){Text("— Pleylistdan olib tashlash",fontSize=12.sp)}
            }
        }
    } else {
        LazyColumn(contentPadding=inset,verticalArrangement=Arrangement.spacedBy(18.dp)) {
            item {SectionHeader("Sening pleylistlaring","Shaxsiy va ommaviy to‘plamlar")}
            if(vm.signedIn)item {PrimaryButton("+ Yangi pleylist",onClick={editor=true},modifier=Modifier.fillMaxWidth())}
            item {SectionHeader("Mening pleylistlarim")
                if(vm.ownPlaylists.isEmpty())TextEmpty("Pleylistlar hali yo‘q","Birinchi to‘plamni yarating.")
                vm.ownPlaylists.forEach{p->PlaylistCard(p){vm.openPlaylist(p)}}
            }
            item {SectionHeader("Ommaviy pleylistlar")
                val ps=vm.playlists.filter{it.isPublic && it.ownerId!=vm.userId}
                if(ps.isEmpty())TextEmpty("Hozircha yo‘q","Ommaviy pleylistlar shu yerda ko‘rinadi.")
                ps.forEach{p->PlaylistCard(p){vm.openPlaylist(p)}}
            }
        }
    }
    if(editor)PlaylistEditorDialog(null,{editor=false}){name,desc,pub->vm.createPlaylist(name,desc,pub);editor=false}
    editExisting?.let{p->PlaylistEditorDialog(p,{editExisting=null}){name,desc,pub->vm.editPlaylist(p.id,name,desc,pub);editExisting=null}}
    if(confirmDelete && selected!=null)AlertDialog(onDismissRequest={confirmDelete=false},title={Text("Pleylist o‘chirilsinmi?")},
        confirmButton={TextButton(onClick={vm.deletePlaylist(selected.id);confirmDelete=false}){Text("O‘chirish",color=Rose)}},
        dismissButton={TextButton(onClick={confirmDelete=false}){Text("Bekor")}})
}
@Composable fun PlaylistCard(p:MelovaPlaylist,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=4.dp).background(CardNight,RoundedCornerShape(16.dp))
        .clickable(onClick=onClick).padding(13.dp),verticalAlignment=Alignment.CenterVertically) {
        AlbumArt(p.id,Modifier.size(60.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name,fontWeight=FontWeight.SemiBold,maxLines=1)
            Text(p.description.ifBlank{if(p.isPublic)"Ommaviy to‘plam" else "Shaxsiy to‘plam"},color=Muted,fontSize=12.sp,maxLines=1)
        }
        Icon(Icons.Filled.ChevronRight,null,tint=Muted)
    }
}
@Composable private fun PlaylistEditorDialog(existing:MelovaPlaylist?,close:()->Unit,save:(String,String,Boolean)->Unit){
    var name by remember(existing){mutableStateOf(existing?.name.orEmpty())}
    var desc by remember(existing){mutableStateOf(existing?.description.orEmpty())}
    var isPublic by remember(existing){mutableStateOf(existing?.isPublic?:false)}
    AlertDialog(onDismissRequest=close,title={Text(if(existing==null)"Yangi pleylist" else "Tahrirlash")},
        text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name,{if(it.length<=90)name=it},label={Text("Nomi")},singleLine=true)
            OutlinedTextField(desc,{if(it.length<=300)desc=it},label={Text("Izoh")})
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(isPublic,{isPublic=it});Text("Ommaviy pleylist")}
        }},confirmButton={TextButton(onClick={save(name.trim(),desc.trim(),isPublic)},enabled=name.isNotBlank()){Text("Saqlash")}},
        dismissButton={TextButton(onClick=close){Text("Bekor")}})
}
