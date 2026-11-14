package com.sadra.ai.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MainScreen(vm: MainViewModel, listenTrigger: Int, onOpenSettings: () -> Unit, onOpenPro: () -> Unit) {
    val ctx = LocalContext.current
    val msgs by vm.messages.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val access by vm.access.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    fun micGranted() = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.toggleListening()
        else vm.setNotice("برای دستور صوتی باید دسترسی میکروفون را بدهید. می‌توانید دستور را تایپ هم بکنید.")
    }
    fun onMic() { if (micGranted()) vm.toggleListening() else micLauncher.launch(Manifest.permission.RECORD_AUDIO) }

    LaunchedEffect(listenTrigger) { if (listenTrigger > 0 && micGranted()) vm.startListening() }
    LaunchedEffect(msgs.size) { if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.lastIndex) }

    Column(Modifier.fillMaxSize().background(Bg).systemBarsPadding().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Avatar(40)
            Spacer(Modifier.width(10.dp))
            Text("صدرا", style = MaterialTheme.typography.titleLarge, color = Cyan, modifier = Modifier.weight(1f))
            Text(
                if (access.isPro) "PRO · ${toFa(access.daysLeft)} روز" else "رایگان · ${toFa(access.remaining)}/${toFa(access.limit)}",
                color = if (access.isPro) Color(0xFF00212A) else Cyan, fontSize = 12.sp,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    .background(if (access.isPro) Cyan else Surface2)
                    .clickable(onClick = onOpenPro).padding(horizontal = 10.dp, vertical = 6.dp)
            )
            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "تنظیمات", tint = Color.White) }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (msgs.isEmpty()) item {
                Text("سلام! بگو «روبیکا رو باز کن»، «فردا ساعت هفت بیدارم کن» یا هر سوالی داری.", color = Color(0xFF8FA3BF))
            }
            items(msgs, key = { it.id }) { m ->
                val mine = m.role == "user"
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.Start else Arrangement.End) {
                    Text(
                        m.text, color = Color.White,
                        modifier = Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (mine) Blue.copy(alpha = 0.55f) else Surface2).padding(12.dp)
                    )
                }
            }
            if (ui.busy) item { Text("…", color = Cyan) }
        }
        ui.notice?.let { Text(it, color = Color(0xFFFFB4AB), modifier = Modifier.padding(bottom = 6.dp)) }
        if (ui.listening) {
            if (ui.partial.isNotEmpty()) Text(ui.partial, color = Color(0xFFB8C7DC), modifier = Modifier.align(Alignment.CenterHorizontally))
            Waveform(ui.level, Modifier.fillMaxWidth().height(48.dp))
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MicButton(ui.listening, ::onMic) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f),
                placeholder = { Text("دستور را بنویس…") }, singleLine = true, shape = RoundedCornerShape(24.dp)
            )
            IconButton(onClick = { vm.submit(input); input = "" }, enabled = input.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.Send, "ارسال", tint = Cyan)
            }
        }
    }
}
