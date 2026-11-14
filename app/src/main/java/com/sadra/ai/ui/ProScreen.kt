package com.sadra.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadra.ai.billing.Plans

@Composable
fun ProScreen(vm: MainViewModel, onBack: () -> Unit, onBuy: () -> Unit, onRestore: () -> Unit) {
    val a by vm.access.collectAsStateWithLifecycle()
    val notice by vm.subNotice.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().background(Bg).systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("اشتراک", style = MaterialTheme.typography.titleLarge, color = Cyan, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("بازگشت") }
        }

        Card("رایگان", highlight = !a.isPro) {
            Text("• ${toFa(Plans.FREE_DAILY_LIMIT)} دستور موفق در روز (هر روز خودکار شارژ می‌شود)", color = Color.White)
            Text("• امروز مانده: ${toFa(a.remaining)} از ${toFa(a.limit)}", color = Cyan)
        }
        Card("PRO", highlight = a.isPro) {
            Text("• دستورهای عادی نامحدود", color = Color.White)
            Text("• مکالمه‌ی پیوسته و حافظه‌ی طولانی‌تر گفتگو", color = Color.White)
            Text("• ${toFa(Plans.PRO_DAYS)} روز · ۱۵۰٬۰۰۰ تومان", color = Cyan)
            if (a.isPro) Text("فعال است: ${toFa(a.daysLeft)} روز مانده", color = Cyan)
        }

        if (a.pending > 0) {
            Text("پرداخت شما ثبت شده و در حال تأیید است. اگر سرور موقتاً در دسترس نباشد، برنامه خودکار دوباره تلاش می‌کند و خرید شما از بین نمی‌رود.", color = Color(0xFFFFD27A))
        }
        notice?.let { Text(it, color = Color(0xFFB8C7DC)) }

        Button(onClick = onBuy, enabled = !a.isPro && a.pending == 0, modifier = Modifier.fillMaxWidth()) {
            Text("خرید PRO از مایکت · ۱۵۰٬۰۰۰ تومان")
        }
        OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) { Text("بازیابی خریدهای قبلی") }
        Text("پرداخت فقط از طریق مایکت انجام می‌شود. برنامه به اطلاعات کارت شما دسترسی ندارد.", color = Color(0xFF8FA3BF))
    }
}

@Composable
private fun Card(title: String, highlight: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (highlight) Surface2 else Surface1).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, color = if (highlight) Cyan else Color.White, style = MaterialTheme.typography.titleMedium)
        content()
    }
}
