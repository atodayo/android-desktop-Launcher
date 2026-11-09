package dev.quantum.launcher

import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContent { QuantumApp(resumeTick) }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}

enum class Mode { Home, Search, Panel, Power, Sleep, Restart, Lock, Settings }

private const val MAX_DOCK = 12

/** どの画面か: 0=ロック 1=スリープ 2=再起動 3=ホーム(パネルやメニューもふくむ) */
private fun screenOf(m: Mode): Int = when (m) {
    Mode.Lock -> 0
    Mode.Sleep -> 1
    Mode.Restart -> 2
    else -> 3
}

@Composable
fun rememberNow(): State<LocalDateTime> = produceState(LocalDateTime.now()) {
    while (true) {
        value = LocalDateTime.now()
        delay(1000)
    }
}

@Composable
fun rememberBattery(): Int {
    val ctx = LocalContext.current
    var level by remember { mutableIntStateOf(100) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (l >= 0 && s > 0) level = l * 100 / s
            }
        }
        val sticky = ContextCompat.registerReceiver(
            ctx, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        sticky?.let { receiver.onReceive(ctx, it) }
        onDispose { ctx.unregisterReceiver(receiver) }
    }
    return level
}

@Composable
fun QuantumApp(tick: Int) {
    val ctx = LocalContext.current
    val store = remember { Store(ctx) }

    var apps by remember { mutableStateOf(emptyList<AppItem>()) }
    LaunchedEffect(tick) { apps = withContext(Dispatchers.IO) { loadApps(ctx) } }

    var dock by remember { mutableStateOf(store.dock()) }
    var hasPw by remember { mutableStateOf(store.hasPw()) }
    var mode by rememberSaveable { mutableStateOf(if (Session.unlocked) Mode.Home else Mode.Lock) }
    var lang by remember { mutableStateOf(store.lang()) }
    var glass by remember { mutableStateOf(GlassStyle.fromTag(store.glass())) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    val clock = rememberCosmicClock()

    var bgMode by remember { mutableStateOf(getBgMode(ctx)) }
    var bgImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var bgVersion by remember { mutableIntStateOf(0) }
    LaunchedEffect(bgVersion) {
        bgImage = withContext(Dispatchers.IO) { loadBgBitmap(ctx, 2200) }
    }
    val img = bgImage
    var hasVideo by remember { mutableStateOf(videoExists(ctx)) }
    var videoVersion by remember { mutableIntStateOf(0) }
    val videoFrames = remember { VideoFrames() }
    val backdrop = remember(clock, bgMode, img, hasVideo, videoVersion) {
        when {
            bgMode == "video" && hasVideo -> Backdrop.Video(videoFile(ctx), videoVersion, videoFrames)
            bgMode == "image" && img != null -> Backdrop.Photo(img)
            else -> Backdrop.Cosmic(clock)
        }
    }

    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                if (importImage(ctx, uri)) {
                    setBgMode(ctx, "image")
                    bgMode = "image"
                    bgVersion++
                }
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                if (importVideo(ctx, uri)) {
                    setBgMode(ctx, "video")
                    bgMode = "video"
                    hasVideo = true
                    videoVersion++
                }
            }
        }
    }

    val nm = remember { ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    var dnd by remember { mutableStateOf(false) }
    LaunchedEffect(tick) {
        val f = nm.currentInterruptionFilter
        dnd = f != NotificationManager.INTERRUPTION_FILTER_ALL &&
            f != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    val battery = rememberBattery()
    val now by rememberNow()
    val hh = "%02d".format(now.hour)
    val mm = "%02d".format(now.minute)
    val dateFormat = stringResource(R.string.date_format)
    val date = now.format(DateTimeFormatter.ofPattern(dateFormat, Locale.getDefault()))
    val dndAccessMsg = stringResource(R.string.toast_dnd_access)
    val dockFullMsg = stringResource(R.string.toast_dock_full, MAX_DOCK)

    fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    fun toggleDnd() {
        if (!nm.isNotificationPolicyAccessGranted) {
            toast(dndAccessMsg)
            runCatching {
                ctx.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            return
        }
        runCatching {
            nm.setInterruptionFilter(
                if (dnd) NotificationManager.INTERRUPTION_FILTER_ALL
                else NotificationManager.INTERRUPTION_FILTER_PRIORITY
            )
            dnd = !dnd
        }
    }

    fun toggleDock(pkg: String) {
        if (pkg in dock) {
            dock = dock - pkg
        } else if (dock.size >= MAX_DOCK) {
            toast(dockFullMsg)
            return
        } else {
            dock = dock + pkg
        }
        store.saveDock(dock)
    }

    fun changeLang(tag: String) {
        if (tag == lang) return
        store.setLang(tag)
        lang = tag
        (ctx as? Activity)?.recreate()
    }

    fun changeBg(m: String) {
        if (m == "image" && bgImage == null) {
            picker.launch(arrayOf("image/*"))
            return
        }
        if (m == "video" && !hasVideo) {
            videoPicker.launch(arrayOf("video/*"))
            return
        }
        bgMode = m
        setBgMode(ctx, m)
    }

    fun changeGlass(g: GlassStyle) {
        glass = g
        store.setGlass(g.tag)
    }

    // アンインストール済みのアプリはバーに出さない
    val shownDock = if (apps.isEmpty()) dock else dock.filter { p -> apps.any { it.pkg == p } }

    CompositionLocalProvider(
        LocalGlassStyle provides glass,
        LocalBackdrop provides backdrop,
        LocalRootSize provides rootSize
    ) {
    Box(Modifier.fillMaxSize().background(Color.Black).onSizeChanged { rootSize = it }) {
        AnimatedContent(
            targetState = screenOf(mode),
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.96f)) togetherWith
                    (fadeOut(tween(220)) + scaleOut(tween(320), targetScale = 1.04f))
            },
            label = "screen"
        ) { screen ->
            when (screen) {
                0 -> LockScreen(
                    hasPassword = hasPw,
                    hour = hh, minute = mm, date = date,
                    onSetPassword = { store.setPw(it); hasPw = true },
                    onCheck = { store.checkPw(it) },
                    onUnlock = { Session.unlocked = true; mode = Mode.Home },
                    lang = lang,
                    onLang = { changeLang(it) }
                )
                1 -> SleepScreen(onWake = { mode = Mode.Home })
                2 -> RestartScreen(onDone = { Session.unlocked = false; mode = Mode.Lock })
                else -> Box(Modifier.fillMaxSize()) {
                    BackdropLayer(backdrop)
                    TopBar(
                        date = date, time = "$hh:$mm", battery = battery, dnd = dnd,
                        onGroup = { mode = Mode.Panel },
                        onBell = { toggleDnd() },
                        onPower = { mode = Mode.Power }
                    )
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 14.dp)
                        ) {
                            Taskbar(
                                dock = shownDock,
                                maxW = this@BoxWithConstraints.maxWidth - 24.dp,
                                onSearch = { mode = Mode.Search },
                                onRemove = { toggleDock(it) },
                                onMove = { a, b ->
                                    val ia = dock.indexOf(a)
                                    val ib = dock.indexOf(b)
                                    if (ia >= 0 && ib >= 0) {
                                        dock = dock.toMutableList().also { it.add(ib, it.removeAt(ia)) }
                                    }
                                },
                                onCommit = { store.saveDock(dock) }
                            )
                        }
                    }

                    // 検索: 下からふわっと
                    AnimatedVisibility(
                        visible = mode == Mode.Search,
                        enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it / 10 },
                        exit = fadeOut(tween(160)) + slideOutVertically(tween(200)) { it / 10 }
                    ) {
                        SearchScreen(
                            apps = apps, dock = shownDock,
                            onClose = { mode = Mode.Home },
                            onToggle = { toggleDock(it) }
                        )
                    }

                    // クイック設定: 右上からぽんっと
                    AnimatedVisibility(
                        visible = mode == Mode.Panel,
                        enter = fadeIn(tween(160)) +
                            scaleIn(tween(240), initialScale = 0.9f, transformOrigin = TransformOrigin(1f, 0f)),
                        exit = fadeOut(tween(120)) +
                            scaleOut(tween(160), targetScale = 0.9f, transformOrigin = TransformOrigin(1f, 0f))
                    ) {
                        QuickPanel(
                            battery = battery,
                            onDismiss = { mode = Mode.Home },
                            onOpenSettings = { mode = Mode.Settings }
                        )
                    }

                    // 設定: 右からすっと
                    AnimatedVisibility(
                        visible = mode == Mode.Settings,
                        enter = fadeIn(tween(200)) + slideInHorizontally(tween(260)) { it / 8 },
                        exit = fadeOut(tween(160)) + slideOutHorizontally(tween(200)) { it / 8 }
                    ) {
                        SettingsScreen(
                            currentLang = lang,
                            onLang = { changeLang(it) },
                            glass = glass,
                            onGlass = { changeGlass(it) },
                            bgMode = bgMode,
                            onBgMode = { changeBg(it) },
                            onPickImage = { picker.launch(arrayOf("image/*")) },
                            onPickVideo = { videoPicker.launch(arrayOf("video/*")) },
                            onClose = { mode = Mode.Home }
                        )
                    }

                    // 電源メニュー: まん中からふわっと
                    AnimatedVisibility(
                        visible = mode == Mode.Power,
                        enter = fadeIn(tween(200)) + scaleIn(tween(240), initialScale = 0.92f),
                        exit = fadeOut(tween(140)) + scaleOut(tween(180), targetScale = 0.92f)
                    ) {
                        PowerMenu(
                            time = "$hh:$mm",
                            onDismiss = { mode = Mode.Home },
                            onSleep = { mode = Mode.Sleep },
                            onRestart = { mode = Mode.Restart },
                            onOff = { (ctx as? Activity)?.finishAffinity() }
                        )
                    }
                }
            }
        }
    }
    }
}
