package uz.melova.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.melova.app.model.MelovaTrack
import uz.melova.app.model.MelovaViewModel

@Composable fun AdminPage(vm:MelovaViewModel) {
    if(!vm.isAdmin){TextEmpty("Ruxsat yo‘q","Bu sahifa faqat tasdiqlangan administratorlar uchun.");return}
    val pending=vm.tracks.filter{it.status=="pending"}
    val activeReports=vm.reports.filter{it.state=="open"}
    var selected by remember{mutableStateOf<MelovaTrack?>(null)}
    var rejectReason by remember{mutableStateOf("")}
    var approve by remember{mutableStateOf(false)}
    var selectedReport by remember{mutableStateOf<String?>(null)}
    LazyColumn(contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {SectionHeader("Moderator paneli","Faqat Supabase admins jadvalidagi akkauntlar")}
        item {SectionHeader("Tekshiruvdagi musiqalar","${pending.size} ta so‘rov")
            if(pending.isEmpty())TextEmpty("Navbat bo‘sh","Yangi musiqalar paydo bo‘lganda shu yerda ko‘rinadi.")
            pending.forEach {t-> CardPanel(Modifier.fillMaxWidth().padding(bottom=9.dp)) {
                Text(t.title,fontWeight=FontWeight.Bold)
                Text("${t.artist} · ${t.genre}",color=Muted)
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick={vm.play(t)}){Text("Tinglash")}
                    TextButton(onClick={selected=t;approve=true}){Text("Tasdiqlash",color=Aqua)}
                    TextButton(onClick={selected=t;approve=false}){Text("Rad etish",color=Rose)}
                }
            }}
        }
        item {SectionHeader("Shikoyatlar","${activeReports.size} ta ochiq")
            if(activeReports.isEmpty())TextEmpty("Shikoyatlar yo‘q","Barcha shikoyatlar ko‘rib chiqilgan.")
            activeReports.forEach {r->CardPanel(Modifier.fillMaxWidth().padding(bottom=8.dp)) {
                Text(vm.tracks.find{it.id==r.trackId}?.title?:"O‘chirilgan trek",fontWeight=FontWeight.Bold)
                Text(r.reason,color=Muted)
                Row {
                    TextButton(onClick={selectedReport=r.id}){Text("Hal qilindi")}
                    vm.tracks.find{it.id==r.trackId&&it.status=="approved"}?.let {t->
                        TextButton(onClick={vm.unpublish(t)}){Text("Ommadan olish",color=Rose)}
                    }
                }
            }}
        }
    }
    selected?.let{t->
        AlertDialog(onDismissRequest={selected=null;rejectReason=""},title={Text(if(approve)"Tasdiqlash" else "Rad etish")},
            text={Column {
                Text(t.title)
                if(!approve)OutlinedTextField(value=rejectReason,onValueChange={rejectReason=it},label={Text("Rad etish sababi")})
            }},confirmButton={TextButton(enabled=approve||rejectReason.isNotBlank(),onClick={
                vm.moderate(t,approve,rejectReason);selected=null;rejectReason=""
            }){Text("Tasdiqlayman")}},dismissButton={TextButton(onClick={selected=null}){Text("Bekor")}})
    }
    selectedReport?.let{id->AlertDialog(onDismissRequest={selectedReport=null},title={Text("Shikoyat yopilsinmi?")},
        confirmButton={TextButton(onClick={vm.resolveReport(id);selectedReport=null}){Text("Yopish")}},
        dismissButton={TextButton(onClick={selectedReport=null}){Text("Bekor")}})}
}
