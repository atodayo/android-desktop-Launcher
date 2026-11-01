@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package dev.quantum.launcher

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

// ---------- 共通パーツ ----------

@Composable
fun QIcon(v: ImageVector, size: Dp = 14.dp, tint: Color = Color.White) {
    Icon(v, contentDescription = null, modifier = Modifier.size(size), tint = tint)
}

@Composable
fun Pill(
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    var m = modifier
        .clip(RoundedCornerShape(50))
        .background(if (active) Color(0x778B7CFF) else Color(0x66000000))
    if (onClick != null) m = m.clickable(onClick = onClick)
    Row(
        m.padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        content = content
    )
}

@Composable
fun AppIcon(pkg: String, size: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val bmp = remember(pkg) {
        runCatching {
            ctx.packageManager.getApplicationIcon(pkg).toBitmap(120, 120).asImageBitmap()
        }.getOrNull()
    }
    if (bmp != null) {
        Image(bmp, contentDescription = null, modifier = modifier.size(size))
    } else {
        Box(modifier.size(size).background(Color.Gray, CircleShape))
    }
}

@Composable
fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color(0x33FFFFFF),
    unfocusedContainerColor = Color(0x26FFFFFF),
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = Color.White,
    focusedPlaceholderColor = Color(0x99FFFFFF),
    unfocusedPlaceholderColor = Color(0x99FFFFFF),
    focusedLeadingIconColor = Color.White,
    unfocusedLeadingIconColor = Color.White,
    focusedTrailingIconColor = Color.White,
    unfocusedTrailingIconColor = Color.White
)

fun openSettings(ctx: Context, action: String) {
    runCatching {
        ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

// ---------- 上バー ----------

@Composable
fun TopBar(
    date: String,
    time: String,
    battery: Int,
    dnd: Boolean,
    onGroup: () -> Unit,
    onBell: () -> Unit,
    onPower: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Pill {
            AtomLogo(Modifier.size(14.dp))
            Text("Quantum", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Wi-Fi / Bluetooth / 音量 は1つのグループボタン
            Pill(onClick = onGroup) {
                QIcon(Icons.Filled.Wifi, 13.dp)
                QIcon(Icons.Filled.Bluetooth, 13.dp)
                QIcon(Icons.Filled.VolumeUp, 13.dp)
            }
            // 電池・時間・月日(押しても反応なし)
            Pill {
                QIcon(Icons.Filled.BatteryFull, 13.dp)
                Text("$battery%", color = Color.White, fontSize = 10.sp, maxLines = 1)
                Text(time, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(date, color = Color(0xAAFFFFFF), fontSize = 10.sp, maxLines = 1)
            }
            Pill(active = dnd, onClick = onBell) {
                QIcon(if (dnd) Icons.Filled.NotificationsOff else Icons.Filled.Notifications, 14.dp)
            }
            Pill(onClick = onPower) {
                QIcon(Icons.Filled.PowerSettingsNew, 14.dp)
            }
        }
    }
}

// ---------- タスクバー ----------

@Composable
fun DockApp(pkg: String, onRemove: () -> Unit) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Box {
        AppIcon(
            pkg, 34.dp,
            Modifier
                .padding(5.dp)
                .clip(RoundedCornerShape(9.dp))
                .combinedClickable(
                    onClick = { launchApp(ctx, pkg) },
                    onLongClick = { menu = true }
                )
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("タスクバーから削除") },
                onClick = { menu = false; onRemove() }
            )
        }
    }
}

@Composable
fun Taskbar(dock: List<String>, maxW: Dp, onSearch: () -> Unit, onRemove: (String) -> Unit) {
    Row(
        Modifier
            .widthIn(max = maxW)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xAA16151F))
            .animateContentSize()
            .horizontalScroll(rememberScrollState())
            .padding(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onSearch),
            contentAlignment = Alignment.Center
        ) {
            QIcon(Icons.Filled.Search, 22.dp)
        }
        if (dock.isNotEmpty()) {
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .width(1.dp)
                    .height(26.dp)
                    .background(Color(0x33FFFFFF))
            )
        }
        dock.forEach { pkg ->
            key(pkg) { DockApp(pkg) { onRemove(pkg) } }
        }
    }
}

// ---------- 検索 ----------

@Composable
fun AppRow(app: AppItem, inDock: Boolean, onLaunch: () -> Unit, onToggle: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(onClick = onLaunch, onLongClick = { menu = true })
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(app.pkg, 40.dp)
            Spacer(Modifier.width(14.dp))
            Text(app.label, color = Color.White, fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f))
            if (inDock) Text("タスクバー", color = QViolet, fontSize = 11.sp)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (inDock) "タスクバーから削除" else "タスクバーに追加") },
                onClick = { menu = false; onToggle() }
            )
        }
    }
}

@Composable
fun SearchScreen(
    apps: List<AppItem>,
    dock: List<String>,
    onClose: () -> Unit,
    onToggle: (String) -> Unit
) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { fr.requestFocus() }
    BackHandler(onBack = onClose)
    val list = remember(apps, q) {
        if (q.isBlank()) apps else apps.filter { it.label.contains(q.trim(), ignoreCase = true) }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xF2090912))
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = q,
                onValueChange = { q = it },
                singleLine = true,
                placeholder = { Text("アプリを検索") },
                leadingIcon = { QIcon(Icons.Filled.Search, 20.dp) },
                shape = RoundedCornerShape(50),
                colors = fieldColors(),
                modifier = Modifier.weight(1f).focusRequester(fr)
            )
            TextButton(onClick = onClose) { Text("閉じる", color = Color.White) }
        }
        Text(
            "長押しでタスクバーに追加・削除できるよ(最大12個)",
            color = Color(0x99FFFFFF),
            fontSize = 11.sp,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(list, key = { it.pkg }) { app ->
                AppRow(
                    app = app,
                    inDock = app.pkg in dock,
                    onLaunch = { launchApp(ctx, app.pkg); onClose() },
                    onToggle = { onToggle(app.pkg) }
                )
            }
        }
    }
}
