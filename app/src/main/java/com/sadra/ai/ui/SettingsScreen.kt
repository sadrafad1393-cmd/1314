package com.sadra.ai.ui

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sadra.ai.permissions.AssistantRole

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, onOpenPro: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = vm.prefs
    var isDefault by remember { mutableStateOf(AssistantRole.isDefault(ctx)) }
    var save by remember { mutableStateOf(prefs.saveHistory) }
    var speak by remember { mutableStateOf(prefs.speakReplies) }
    var cont by remember { mutableStateOf(prefs.continuous) }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefault = AssistantRole.isDefault(ctx)
    }

    Column(Modifier.fillMaxSize().background(Bg).systemBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("تنظیمات", style = MaterialTheme.typography.titleLarge, color = Cyan, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("بازگشت") }
        }

        OutlinedButton(onClick = onOpenPro) { Text("اشتراک رایگان / PRO") }
        Text("دستیار پیش‌فرض", color = Cyan)
        Text(if (isDefault) "صدرا اکنون دستیار پیش‌فرض است ✓" else "صدرا هنوز دستیار پیش‌فرض نیست.", color = Color.White)
        Button(onClick = {
            try { roleLauncher.launch(AssistantRole.requestIntent(ctx)) }
            catch (e: ActivityNotFoundException) { Toast.makeText(ctx, "این گوشی صفحه‌ی انتخاب دستیار را ندارد.", Toast.LENGTH_LONG).show() }
        }) { Text("انتخاب صدرا به‌عنوان دستیار پیش‌فرض") }
        OutlinedButton(onClick = {
            try { ctx.startActivity(AssistantRole.settingsIntent().addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            catch (e: ActivityNotFoundException) { Toast.makeText(ctx, "تنظیمات → برنامه‌ها → برنامه‌های پیش‌فرض → دستیار دیجیتال", Toast.LENGTH_LONG).show() }
        }) { Text("باز کردن تنظیمات دستیار سیستم") }
        Text(
            "توجه: اجرای دستیار با نگه داشتن دکمه‌ی پاور یا کلید کناری فقط روی گوشی‌هایی کار می‌کند که سازنده آن را به دستیار پیش‌فرض متصل کرده است (مثلاً در برخی گوشی‌ها باید در تنظیمات دکمه‌ی کناری، گزینه‌ی «دستیار دیجیتال» انتخاب شود؛ در بعضی مدل‌ها مثل سامسونگ ممکن است دستیار خودِ سازنده اولویت داشته باشد). هیچ تضمینی برای همه‌ی گوشی‌ها وجود ندارد و برنامه از روت یا دور زدن سیستم استفاده نمی‌کند.",
            color = Color(0xFF8FA3BF)
        )

        HorizontalDivider()
        Text("هوش مصنوعی محلی", color = Cyan)
        Text(
            "در بسته‌ی فعلی مدل زبانی و موتور اجرای محلی وجود ندارد؛ بنابراین گفت‌وگوی آزاد هوشمند هنوز فعال نیست. دستورهای دستگاه که در برنامه پیاده‌سازی شده‌اند بدون ارسال گفتگو به اینترنت اجرا می‌شوند.",
            color = Color(0xFF8FA3BF)
        )

        HorizontalDivider()
        Text("صدا و حافظه", color = Cyan)
        SwitchRow("خواندن پاسخ‌ها با صدا", speak) { speak = it; prefs.speakReplies = it }
        SwitchRow("مکالمه‌ی پیوسته (ویژه‌ی PRO، فقط وقتی برنامه باز است)", cont) { cont = it; prefs.continuous = it }
        SwitchRow("ذخیره‌ی تاریخچه‌ی گفتگو روی گوشی", save) { save = it; prefs.saveHistory = it }
        OutlinedButton(onClick = { vm.clearHistory(); Toast.makeText(ctx, "تاریخچه پاک شد.", Toast.LENGTH_SHORT).show() }) { Text("پاک کردن تاریخچه") }
        Text("محتوای گفتگو برای پردازش هوش مصنوعی به سرور ارسال نمی‌شود. میکروفون فقط بعد از لمس دکمه یا فراخوانی دستیار روشن می‌شود.", color = Color(0xFF8FA3BF))
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
