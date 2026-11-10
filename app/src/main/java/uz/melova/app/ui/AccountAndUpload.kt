package uz.melova.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.melova.app.model.MelovaViewModel
import uz.melova.app.model.melovaGenres

@Composable fun UploadPage(vm:MelovaViewModel,login:()->Unit){
    var file by remember{mutableStateOf<Uri?>(null)}
    var title by remember{mutableStateOf("")};var artist by remember{mutableStateOf("")}
    var genre by remember{mutableStateOf("Boshqa")};var description by remember{mutableStateOf("")}
    var publicRequest by remember{mutableStateOf(false)};var rights by remember{mutableStateOf(false)}
    var genresOpen by remember{mutableStateOf(false)}
    val filePicker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {file=it}
    LazyColumn(contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(15.dp)) {
        item {SectionHeader("Musiqa yuklash","MP3, M4A, WAV yoki OGG · 20 MB gacha")}
        if(!vm.signedIn)item {CardPanel(Modifier.fillMaxWidth()) {
            Text("Musiqa yuklash uchun akkauntga kirish kerak.",color=Muted)
            Spacer(Modifier.height(10.dp));Button(onClick=login){Text("Kirish")}
        }}
        item {CardPanel(Modifier.fillMaxWidth()) {
            Text("Audio fayl",fontWeight=FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick={filePicker.launch(arrayOf("audio/*","application/ogg"))},modifier=Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.UploadFile,null);Spacer(Modifier.width(8.dp));Text(if(file==null)"Fayl tanlash" else "Boshqa fayl tanlash")
            }
            Text(file?.lastPathSegment.orEmpty().ifBlank{"Fayl tanlanmagan"},fontSize=12.sp,color=Muted,maxLines=2)
        }}
        item {OutlinedTextField(value=title,onValueChange={if(it.length<=120)title=it},label={Text("Musiqa nomi")},singleLine=true,modifier=Modifier.fillMaxWidth())}
        item {OutlinedTextField(value=artist,onValueChange={if(it.length<=100)artist=it},label={Text("Ijrochi")},singleLine=true,modifier=Modifier.fillMaxWidth())}
        item {Box {
            OutlinedButton(onClick={genresOpen=true},modifier=Modifier.fillMaxWidth()){Text("Janr: $genre");Spacer(Modifier.weight(1f));Icon(Icons.Filled.ArrowDropDown,null)}
            DropdownMenu(expanded=genresOpen,onDismissRequest={genresOpen=false}) {
                melovaGenres.drop(1).forEach{g->DropdownMenuItem(text={Text(g)},onClick={genre=g;genresOpen=false})}
            }
        }}
        item {OutlinedTextField(value=description,onValueChange={if(it.length<=500)description=it},label={Text("Izoh (ixtiyoriy)")},minLines=2,modifier=Modifier.fillMaxWidth())}
        item {CardPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically){Checkbox(publicRequest,onCheckedChange={publicRequest=it;if(!it)rights=false});
                Text("Ommaviy katalogga yuborish")}
            Text("Ommaviy musiqa avval administrator tekshiruvidan o‘tadi.",fontSize=12.sp,color=Muted)
            if(publicRequest) Row(verticalAlignment=Alignment.CenterVertically){Checkbox(rights,onCheckedChange={rights=it});
                Text("Bu audioni ommaga tarqatish huquqim bor",fontSize=12.sp)}
        }}
        item {PrimaryButton("Musiqani yuklash",enabled=!vm.busy&&vm.signedIn&&file!=null&&title.isNotBlank()&&artist.isNotBlank()&&(!publicRequest||rights),
            modifier=Modifier.fillMaxWidth(),onClick={
                val selected=file
                if(selected!=null)vm.upload(selected,title,artist,genre,description,publicRequest,rights){
                    title="";artist="";file=null;description="";publicRequest=false;rights=false
                }
            })}
        item {Text("Yuklash davomida ilovani ochiq tuting. Katta fayllar internet tezligiga bog‘liq.",color=Muted,fontSize=12.sp)}
    }
}

@Composable fun ProfilePage(vm:MelovaViewModel,login:()->Unit,config:()->Unit,go:(String)->Unit) {
    var username by remember(vm.profile){mutableStateOf(vm.profile?.username.orEmpty())}
    var name by remember(vm.profile){mutableStateOf(vm.profile?.displayName.orEmpty())}
    var bio by remember(vm.profile){mutableStateOf(vm.profile?.bio.orEmpty())}
    var reset by remember{mutableStateOf(false)}
    var signOut by remember{mutableStateOf(false)}
    LazyColumn(contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
        item {SectionHeader("Profilim","MELOVA · Sening musiqa olaming")}
        item {CardPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Filled.AccountCircle,null,tint=Violet,modifier=Modifier.size(56.dp))
                Spacer(Modifier.width(10.dp));Column {
                    Text(vm.profile?.displayName?:"Mehmon",fontSize=20.sp,fontWeight=FontWeight.Bold)
                    Text(if(vm.isAdmin)"Administrator" else if(vm.signedIn)"Tinglovchi" else "Akkauntga kirmagan",color=Muted)
                }
            }
        }}
        if(!vm.signedIn)item {CardPanel(Modifier.fillMaxWidth()) {
            Text("Barcha imkoniyatlar uchun akkauntga kiring.",color=Muted)
            Spacer(Modifier.height(8.dp));PrimaryButton("Kirish yoki ro‘yxatdan o‘tish",login)
        }}
        if(vm.signedIn)item {
            CardPanel(Modifier.fillMaxWidth()) {
                Text("Profilni tahrirlash",fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(name,{if(it.length<=64)name=it},label={Text("Ism")},singleLine=true)
                OutlinedTextField(username,{if(it.length<=24)username=it},label={Text("Username")},singleLine=true)
                OutlinedTextField(bio,{if(it.length<=250)bio=it},label={Text("Bio")},minLines=2)
                Spacer(Modifier.height(10.dp));PrimaryButton("Saqlash",{vm.saveProfile(username,name,bio)},enabled=!vm.busy)
            }
        }
        item {SectionHeader("Kutubxona va boshqaruv")
            ProfileAction("Sevimli musiqalar",Icons.Filled.FavoriteBorder){go("favorites")}
            ProfileAction("Tinglash tarixi",Icons.Filled.History){go("history")}
            ProfileAction("Musiqa yuklash",Icons.Filled.CloudUpload){go("upload")}
            if(vm.isAdmin)ProfileAction("Administrator paneli",Icons.Filled.AdminPanelSettings){go("admin")}
            // Packaged production builds are already connected to our shared project.
            // Manual project settings are only available in old/demo testing builds.
            if (!vm.api.preconfigured) ProfileAction("Supabase ulanishi",Icons.Filled.Settings,config)
            if(vm.signedIn) {
                ProfileAction("Parolni tiklash",Icons.Filled.LockReset){reset=true}
                ProfileAction("Akkauntdan chiqish",Icons.Filled.Logout){signOut=true}
            }
        }
        item {Text("MELOVA 1.0 · Native Android · Kotlin + Media3",fontSize=11.sp,color=Muted)}
    }
    if(reset)ResetDialog(vm){reset=false}
    if(signOut)AlertDialog(onDismissRequest={signOut=false},title={Text("Chiqishni tasdiqlang")},
        confirmButton={TextButton(onClick={vm.signOut();signOut=false}){Text("Chiqish")}},
        dismissButton={TextButton(onClick={signOut=false}){Text("Bekor")}})
}
@Composable private fun ProfileAction(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,onClick:()->Unit){
    Row(Modifier.fillMaxWidth().padding(bottom=4.dp).background(CardNight,RoundedCornerShape(15.dp))
        .clickable(onClick=onClick).padding(15.dp),verticalAlignment=Alignment.CenterVertically) {
        Icon(icon,null,tint=Violet);Spacer(Modifier.width(13.dp));Text(label,Modifier.weight(1f));Icon(Icons.Filled.ChevronRight,null,tint=Muted)
    }
}

@Composable fun AuthDialog(vm:MelovaViewModel,onDismiss:()->Unit,onGoogle:()->Unit) {
    var register by remember{mutableStateOf(false)}
    var email by remember{mutableStateOf("")};var password by remember{mutableStateOf("")}
    var reset by remember{mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(register)"Ro‘yxatdan o‘tish" else "MELOVA'ga kirish")},
        text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(email,{email=it},modifier=Modifier.fillMaxWidth(),label={Text("Email")},singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email))
            OutlinedTextField(password,{password=it},modifier=Modifier.fillMaxWidth(),label={Text("Parol")},singleLine=true,
                visualTransformation=PasswordVisualTransformation())
            PrimaryButton(if(register)"Akkaunt yaratish" else "Kirish",modifier=Modifier.fillMaxWidth(),enabled=!vm.busy&&email.isNotBlank()&&password.length>=6,onClick={
                if(register)vm.signUp(email,password,onDismiss) else vm.signIn(email,password,onDismiss)
            })
            OutlinedButton(onClick=onGoogle,modifier=Modifier.fillMaxWidth(),enabled=vm.api.configured) {
                Icon(Icons.Filled.Language,null);Spacer(Modifier.width(8.dp));Text("Google bilan kirish")
            }
            TextButton(onClick={register=!register}){Text(if(register)"Akkaunting bormi? Kirish" else "Akkaunt yo‘qmi? Ro‘yxatdan o‘tish")}
            TextButton(onClick={reset=true}){Text("Parolni unutdingizmi?",fontSize=12.sp)}
            if(vm.isDemo)Text("Google/email kirish uchun avval Supabase sozlanishi kerak.",color=Muted,fontSize=11.sp)
        }},confirmButton={TextButton(onClick=onDismiss){Text("Yopish")}})
    if(reset)ResetDialog(vm){reset=false}
}
@Composable private fun ResetDialog(vm:MelovaViewModel,onDismiss:()->Unit){
    var email by remember{mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Parolni tiklash")},
        text={OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true)},
        confirmButton={TextButton(enabled=email.contains('@'),onClick={vm.resetPassword(email);onDismiss()}){Text("Email yuborish")}},
        dismissButton={TextButton(onClick=onDismiss){Text("Bekor")}})
}
@Composable fun ConfigDialog(vm:MelovaViewModel,onDismiss:()->Unit) {
    var url by remember{mutableStateOf(vm.api.url)};var key by remember{mutableStateOf(vm.api.key)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Supabase ulanishi")},
        text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text("Melova web ilovangiz bilan BIR XIL Supabase Project URL va publishable/anon keyni kiriting.",fontSize=12.sp,color=Muted)
            OutlinedTextField(value=url,onValueChange={url=it},label={Text("https://project.supabase.co")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(value=key,onValueChange={key=it},label={Text("Publishable / anon key")},maxLines=3,modifier=Modifier.fillMaxWidth())
            Text("service_role va sb_secret_ kalitlarni kiritmang. Ulanishni almashtirish joriy sessiyadan chiqaradi.",color=Muted,fontSize=11.sp)
        }},confirmButton={TextButton(enabled=url.isNotBlank()&&key.length>20,onClick={vm.configure(url,key);onDismiss()}){Text("Ulash")}},
        dismissButton={TextButton(onClick=onDismiss){Text("Bekor")}})
}
