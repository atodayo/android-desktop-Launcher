@file:OptIn(ExperimentalMaterial3Api::class)

package dev.quantum.launcher

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import androidx.compose.material3.ExperimentalMaterial3Api

// ---------- クイック設定パネル ----------

@Composable
private fun Tile(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x22FFFFFF))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { QIcon(icon, 22.dp) }
        Text(label, color = Color.White, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
fun QuickPanel(battery: Int, onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    val ctx = LocalContext.current
    BackHandler(onBack = onDismiss)
    val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    var vol by remember { mutableFloatStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }

    Box(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
    ) {
        GlassBox(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 46.dp, end = 8.dp)
                .fillMaxWidth(0.94f)
                .widthIn(max = 340.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            RoundedCornerShape(18.dp),
            offFill = Color(0xF01C1B26),
            extraDim = Color(0x33000000)
        ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 音量ゲージ(一番上)
            Row(verticalAlignment = Alignment.CenterVertically) {
                QIcon(Icons.Filled.VolumeUp, 20.dp)
                Slider(
                    value = vol,
                    onValueChange = {
                        vol = it
                        runCatching { am.setStreamVolume(AudioManager.STREAM_MUSIC, it.roundToInt(), 0) }
                    },
                    valueRange = 0f..maxVol,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = QViolet,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    ),
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            // Wi-Fi / インターネット / Bluetooth
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tile(Icons.Filled.Wifi, "Wi-Fi", Modifier.weight(1f)) {
                    openSettings(
                        ctx,
                        if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS
                    )
                }
                Tile(Icons.Filled.SwapVert, stringResource(R.string.tile_internet), Modifier.weight(1f)) {
                    openSettings(
                        ctx,
                        if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIRELESS_SETTINGS
                    )
                }
                Tile(Icons.Filled.Bluetooth, "Bluetooth", Modifier.weight(1f)) {
                    openSettings(ctx, Settings.ACTION_BLUETOOTH_SETTINGS)
                }
            }
            // 電池と設定
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QIcon(Icons.Filled.BatteryFull, 18.dp)
                    Text("$battery%", color = Color.White, fontSize = 13.sp)
                }
                IconButton(onClick = onOpenSettings) {
                    QIcon(Icons.Filled.SettingsIcon, 20.dp)
                }
            }
        }
        }
    }
}

// ---------- 電源メニュー ----------

@Composable
private fun PowerBtn(icon: ImageVector, label: String, highlight: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (highlight) {
            Box(
                Modifier
                    .size(66.dp)
                    .clip(shape)
                    .background(brush = Brush.linearGradient(listOf(QBlue, QViolet)), shape = shape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center
            ) { QIcon(icon, 26.dp) }
        } else {
            GlassBox(
                Modifier.size(66.dp).clip(shape).clickable(onClick = onClick),
                shape,
                offFill = Color(0xFF26252E),
                contentAlignment = Alignment.Center
            ) { QIcon(icon, 26.dp) }
        }
        Text(label, color = Color(0xCCFFFFFF), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun PowerMenu(time: String, onDismiss: () -> Unit, onSleep: () -> Unit, onRestart: () -> Unit, onOff: () -> Unit) {
    BackHandler(onBack = onDismiss)
    GlassBox(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        RectangleShape,
        offFill
