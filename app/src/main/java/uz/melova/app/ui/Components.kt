package uz.melova.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.melova.app.model.MelovaTrack
import uz.melova.app.model.MelovaViewModel

private val gradients=listOf(
    listOf(Color(0xFF2A8F8F),Color(0xFF374B9D)),
    listOf(Color(0xFFB35B93),Color(0xFF573392)),
    listOf(Color(0xFFBD8557),Color(0xFF8065B5)),
    listOf(Color(0xFF368D70),Color(0xFF224B78)),
    listOf(Color(0xFF8D50AB),Color(0xFF257C91))
)
@Composable fun AlbumArt(id:String,modifier:Modifier=Modifier,large:Boolean=false) {
    val pair=gradients[(id.hashCode().toLong().let{if(it<0)-it else it}%gradients.size).toInt()]
    Box(modifier.clip(RoundedCornerShape(if(large)26.dp else 16.dp))
        .background(Brush.linearGradient(pair)),contentAlignment=Alignment.Center) {
        Icon(Icons.Filled.MusicNote,contentDescription=null,tint=Color.White.copy(alpha=.86f),
            modifier=Modifier.fillMaxSize(if(large) 0.35f else 0.45f))
    }
}
@Composable fun SectionHeader(title:String,subtitle:String?=null,action:String?=null,onAction:(()->Unit)?=null) {
    Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth().padding(bottom=12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            if(subtitle!=null)Text(subtitle,color=Muted,fontSize=12.sp)
        }
        if(action!=null && onAction!=null) TextButton(onClick=onAction){Text(action)}
    }
}
@Composable fun TextEmpty(title:String,subtitle:String) {
    Column(Modifier.fillMaxWidth().padding(vertical=40.dp,horizontal=20.dp),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(9.dp)) {
        Icon(Icons.Filled.LibraryMusic,null,tint=Violet,modifier=Modifier.size(40.dp))
        Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
        Text(subtitle,color=Muted,style=MaterialTheme.typography.bodyMedium)
    }
}
@Composable fun TrackRow(t:MelovaTrack,vm:MelovaViewModel,queue:List<MelovaTrack>,onMenu:(MelovaTrack)->Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=4.dp)
        .clip(RoundedCornerShape(15.dp)).background(CardNight)
        .clickable {vm.play(t,queue)}.padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
        AlbumArt(t.id,Modifier.size(52.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title,maxLines=1,overflow=TextOverflow.Ellipsis,fontWeight=FontWeight.SemiBold)
            Text("${t.artist} · ${t.genre}",color=Muted,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        if(vm.activeId==t.id && vm.playing)Icon(Icons.Filled.Equalizer,null,tint=Aqua,modifier=Modifier.size(21.dp))
        IconButton(onClick={vm.like(t)}) {
            Icon(if(t.id in vm.favorites)Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription="Sevimlilar",tint=if(t.id in vm.favorites)Rose else Muted)
        }
        IconButton(onClick={onMenu(t)}) {Icon(Icons.Filled.MoreVert,"Musiqa menyusi",tint=Muted)}
    }
}
@Composable fun TrackList(tracks:List<MelovaTrack>,vm:MelovaViewModel,onMenu:(MelovaTrack)->Unit) {
    if(tracks.isEmpty())TextEmpty("Hozircha trek yo‘q","Yangi musiqalar shu yerda ko‘rinadi.")
    else Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
        tracks.forEach { TrackRow(it,vm,tracks,onMenu) }
    }
}
@Composable fun CardPanel(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){
    Column(modifier.clip(RoundedCornerShape(21.dp)).background(CardNight).padding(18.dp),content=content)
}
@Composable fun PrimaryButton(title:String,onClick:()->Unit,enabled:Boolean=true,modifier:Modifier=Modifier){
    Button(onClick=onClick,enabled=enabled,modifier=modifier) {
        Text(title,fontWeight=FontWeight.SemiBold)
    }
}
