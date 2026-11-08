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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

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
    var inner = Modifier.background(if (active) Color(0x778B7CFF) else Color.Transparent)
    if (onClick != null) inner = inner.clickable(onClick = onClick)
    GlassBox(modifier, RoundedCornerShape(50)) {
        Row(
            inner.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            content = content
        )
    }
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

/** アイコン1つぶんの横はば(アイコン34dp + 左右の余白5dpずつ) */
private val DOCK_PITCH = 44.dp

@Composable
fun DockApp(
    pkg: String,
    liftTarget: Float,
    dragging: Boolean,
    dragOffset: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onRemove: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val slop = LocalViewConfiguration.current.touchSlop
    var menu by remember { mutableStateOf(false) }
    var moved by remember { mutableStateOf(false) }
    var suppress by remember { mutableStateOf(false) }

    // 指(マウス)の近くのアイコンが、ふわっと上がる。つかんだアイコンは、もっと上がって少し大きくなる
    val lift by animateFloatAsState(
        targetValue = if (dragging) 10f else liftTarget,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "dockLift"
    )
    val scale by animateFloatAsState(
        targetValue = if (dragging) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "dockScale"
    )

    Box(
        Modifier
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                translationY = -lift * density
                translationX = if (dragging) dragOffset else 0f
                scaleX = scale
                scaleY = scale
            }
    ) {
        AppIcon(
            pkg, 34.dp,
            Modifier
                .padding(5.dp)
                .clip(RoundedCornerShape(9.dp))
                .clickable { if (!suppress) launchApp(ctx, pkg) }
                .pointerInput(pkg) {
                    var total = 0f
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            total = 0f
                            moved = false
                            suppress = true
                            onDragStart()
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            total += amount.x
                            if (kotlin.math.abs(total) > slop) moved = true
                            onDrag(amount.x)
                        },
                        onDragEnd = {
                            if (!moved) menu = true
                            onDragEnd()
                            scope.launch {
                                delay(300)
                                suppress = false
                            }
                        },
                        onDragCancel = {
                            onDragEnd()
                            scope.launch {
                                delay(300)
                                suppress = false
                            }
                        }
                    )
                }
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_from_dock)) },
                onClick = { menu = false; onRemove() }
            )
        }
    }
}

@Composable
fun Taskbar(
    dock: List<String>,
    maxW: Dp,
    onSearch: () -> Unit,
    onRemove: (String) -> Unit,
    onMove: (String, String) -> Unit,
    onCommit: () -> Unit
) {
    val dens = LocalDensity.current
    val pitch = with(dens) { DOCK_PITCH.toPx() }
    val pad = with(dens) { 5.dp.toPx() }
    val sepW = with(dens) { 9.dp.toPx() }

    var pointerX by remember { mutableStateOf<Float?>(null) }
    var draggingPkg by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val currentDock by rememberUpdatedState(dock)

    // 指(マウス)のいちばん近くが一番たかく、はなれるほどひくくなる
    fun liftFor(centerX: Float): Float {
        val p = pointerX ?: return 0f
        val d = kotlin.math.abs(p - centerX) / pitch
        return if (d > 2.6f) 0f else 7f * kotlin.math.exp(-(d / 1.4f) * (d / 1.4f))
    }

    GlassBox(
        Modifier.widthIn(max = maxW).animateContentSize(),
        RoundedCornerShape(22.dp),
        offFill = Color(0xAA16151F)
    ) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent(PointerEventPass.Initial)
                        val c = ev.changes.firstOrNull()
                        pointerX = when {
                            c == null || ev.type == PointerEventType.Exit -> null
                            c.type == PointerType.Touch -> if (c.pressed) c.position.x else null
                            else -> c.position.x
                        }
                    }
                }
            }
            .padding(horizontal = 5.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val searchLift by animateFloatAsState(
            targetValue = liftFor(pad + pitch / 2f),
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
            label = "searchLift"
        )
        Box(
            Modifier
                .size(44.dp)
                .graphicsLayer { translationY = -searchLift * density }
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
        dock.forEachIndexed { i, pkg ->
            key(pkg) {
                val cx = pad + pitch + (if (dock.isNotEmpty()) sepW else 0f) + i * pitch + pitch / 2f
                DockApp(
                    pkg = pkg,
                    liftTarget = liftFor(cx),
                    dragging = draggingPkg == pkg,
                    dragOffset = if (draggingPkg == pkg) dragOffset else 0f,
                    onDragStart = {
                        draggingPkg = pkg
                        dragOffset = 0f
                    },
                    onDrag = { dx ->
                        dragOffset += dx
                        val list = currentDock
                        val idx = list.indexOf(pkg)
                        if (dragOffset > pitch / 2f && idx >= 0 && idx < list.lastIndex) {
                            onMove(pkg, list[idx + 1])
                            dragOffset -= pitch
                        } else if (dragOffset < -pitch / 2f && idx > 0) {
                            onMove(pkg, list[idx - 1])
                            dragOffset += pitch
                        }
                    },
                    onDragEnd = {
                        draggingPkg = null
                        dragOffset = 0f
                        onCommit()
                    },
                    onRemove = { onRemove(pkg) }
                )
            }
        }
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
            if (inDock) Text(stringResource(R.string.dock_label), color = QViolet, fontSize = 11.sp)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(if (inDock) R.string.remove_from_dock else R.string.add_to_dock)) },
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
            .imePadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = q,
                onValueChange = { q = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = { QIcon(Icons.Filled.Search, 20.dp) },
                shape = RoundedCornerShape(50),
                colors = fieldColors(),
                modifier = Modifier.weight(1f).focusRequester(fr)
            )
            TextButton(onClick = onClose) { Text(stringResource(R.string.close), color = Color.White) }
        }
        Text(
            stringResource(R.string.search_hint),
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
}
