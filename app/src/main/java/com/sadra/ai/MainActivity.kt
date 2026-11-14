package com.sadra.ai

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.lifecycleScope
import com.sadra.ai.ui.*
import kotlinx.coroutines.launch

private enum class Screen { MAIN, SETTINGS, PRO }

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private var listenTrigger by mutableIntStateOf(0)

    private val buyLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        lifecycleScope.launch { vm.sub.onPurchaseResult(r.resultCode, r.data) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isAssistLaunch(intent)) listenTrigger++
        setContent {
            SadraTheme {
                androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    var screen by remember { mutableStateOf(Screen.MAIN) }
                    when (screen) {
                        Screen.SETTINGS -> SettingsScreen(vm, { screen = Screen.MAIN }, { screen = Screen.PRO })
                        Screen.PRO -> ProScreen(vm, { screen = Screen.MAIN }, ::buy, ::restore)
                        Screen.MAIN -> MainScreen(vm, listenTrigger, { screen = Screen.SETTINGS }, { screen = Screen.PRO })
                    }
                }
            }
        }
    }

    private fun buy() {
        lifecycleScope.launch {
            vm.sub.clearNotice()
            val pi = vm.sub.prepareBuy()
            if (pi != null) buyLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
            else vm.sub.notice.value?.let { Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun restore() { lifecycleScope.launch { vm.sub.clearNotice(); vm.sub.restore() } }

    override fun onResume() {
        super.onResume()
        vm.sub.refreshState() // daily quota reset / PRO expiry while the app was in background
        lifecycleScope.launch { vm.sub.processPending() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isAssistLaunch(intent)) listenTrigger++
    }

    private fun isAssistLaunch(i: Intent?) = i != null && (
        i.getBooleanExtra(EXTRA_AUTO_LISTEN, false) ||
            i.action == Intent.ACTION_ASSIST || i.action == Intent.ACTION_VOICE_COMMAND
        )

    companion object { const val EXTRA_AUTO_LISTEN = "auto_listen" }
}
