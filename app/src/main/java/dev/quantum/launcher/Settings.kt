package dev.quantum.launcher

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- 言語 ----------

val LANGS = listOf("", "en", "ja", "zh-CN")

@Composable
fun langName(tag: String): String = when (tag) {
    "en" -> "English"
    "ja" -> "日本語"
    "zh-CN" -> "简体中文"
    else -> stringResource(R.string.lang_system)
}

/** ロック画面の左下に出す、言語えらびボタン */
@Composable
fun LanguagePill(current: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Pill(onClick = { open = true }) {
            QIcon(Icons.Filled.Language, 14.dp)
            Text(langName(current), color = Color.White, fontSize = 11.sp)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LANGS.forEach { tag ->
                DropdownMenuItem(
                    text = { Text(langName(tag)) },
                    onClick = { open = false; onSelect(tag) }
                )
            }
        }
    }
}

// ---------- Quantum設定 ----------

@Composable
fun glassName(g: GlassStyle): String = stringResource(
    when (g) {
        GlassStyle.Off -> R.string.glass_off
        GlassStyle.Frosted -> R.string.glass_frosted
        GlassStyle.Clear -> R.string.glass_clear
    }
)

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = QViolet, fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
    )
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (selected) QIcon(Icons.Filled.Check, 18.dp, QViolet)
    }
}

@Composable
fun SettingsScreen(
    currentLang: String,
    onLang: (String) -> Unit,
    glass: GlassStyle,
    onGlass: (GlassStyle) -> Unit,
    bgMode: String,
    onBgMode: (String) -> Unit,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    BackHandler(onBack = onClose)
    GlassBox(
        Modifier.fillMaxSize(),
        RectangleShape,
        offFill = Color(0xF2090912),
        extraDim = Color(0x66000000)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { QIcon(Icons.AutoMirrored.Filled.ArrowBack, 22.dp) }
                Text(
                    stringResource(R.string.settings_title),
                    color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(16.dp))

            SectionTitle(stringResource(R.string.language))
            LANGS.forEach { tag ->
                OptionRow(langName(tag), tag == currentLang) { onLang(tag) }
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.bg_title))
            OptionRow(stringResource(R.string.bg_cosmic), bgMode == "cosmic") { onBgMode("cosmic") }
            OptionRow(stringResource(R.string.bg_photo), bgMode == "image") { onBgMode("image") }
            OptionRow(stringResource(R.string.bg_pick), false) { onPickImage() }
            OptionRow(stringResource(R.string.bg_video), bgMode == "video") { onBgMode("video") }
            OptionRow(stringResource(R.string.bg_pick_video), false) { onPickVideo() }

            Spacer(Modifier.height(16.dp))
            SectionTitle(stringResource(R.string.glass_title))
            GlassStyle.values().forEach { g ->
                OptionRow(glassName(g), g == glass) { onGlass(g) }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { openSettings(ctx, Settings.ACTION_SETTINGS) }
                    .padding(horizontal = 8.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QIcon(Icons.Filled.SettingsIcon, 18.dp)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.open_system_settings), color = Color.White, fontSize = 15.sp)
            }
        }
    }
}
