package uz.melova.app

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import uz.melova.app.model.MelovaViewModel
import uz.melova.app.playback.MelovaPlaybackService
import uz.melova.app.ui.MelovaApp

class MainActivity:ComponentActivity() {
    private val vm:MelovaViewModel by viewModels()
    private var controllerFuture:ListenableFuture<MediaController>?=null
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=android.graphics.Color.rgb(17,18,33)
        window.navigationBarColor=android.graphics.Color.rgb(17,18,33)
        val token=SessionToken(this,ComponentName(this,MelovaPlaybackService::class.java))
        val future=MediaController.Builder(this,token).buildAsync()
        controllerFuture=future
        future.addListener({
            try { if(!isFinishing)vm.attachController(future.get()) }
            catch(_:Exception) {vm.toast("Audio xizmati ulanmagan.")}
        },ContextCompat.getMainExecutor(this))
        setContent {
            MelovaApp(vm,openGoogle={uri->
                try {startActivity(Intent(Intent.ACTION_VIEW,uri).addCategory(Intent.CATEGORY_BROWSABLE))}
                catch(e:Exception){vm.toast("Brauzer ochilmadi: ${e.message}")}
            })
        }
        handleOAuth(intent?.data)
    }
    override fun onNewIntent(intent:Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuth(intent.data)
    }
    private fun handleOAuth(uri:Uri?) {
        if(uri?.scheme=="uzmelova"&&uri.host=="auth"&&uri.path=="/callback")vm.oauthCallback(uri)
    }
    override fun onDestroy() {
        vm.detachController()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }
}
