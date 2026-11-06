package dev.quantum.launcher

import android.net.Uri
import android.view.TextureView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** 動画の「いまの1コマ」を入れておく箱。ガラスは、この絵を後ろにしいて、ぼかしたり、ゆがめたりするよ */
class VideoFrames {
    var frame by mutableStateOf<ImageBitmap?>(null)
}

/** ガラスの後ろにしく、動画の1コマ */
@Composable
fun VideoFrameImage(frames: VideoFrames) {
    val f = frames.frame
    if (f != null) {
        Image(
            bitmap = f,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * 動画の背景。音は出さずに、ずっとくり返して流すよ。
 * アプリが見えていないときは止めて、電池をまもるよ。
 * ガラスを使っているときは、動画の1コマを少しずつ取って、ガラスの後ろにも見せるよ。
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoBackground(file: File, version: Int, frames: VideoFrames, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val root = LocalRootSize.current
    val wantFrames = LocalGlassStyle.current != GlassStyle.Off

    var videoSize by remember(file.path, version) { mutableStateOf(IntSize.Zero) }
    var textureView by remember { mutableStateOf<TextureView?>(null) }

    val player = remember(file.path, version) {
        ExoPlayer.Builder(ctx).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player, lifecycleOwner) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                GlassDiag.video = "error: " + error.errorCodeName
            }

            override fun onPlaybackStateChanged(state: Int) {
                GlassDiag.video = when (state) {
                    Player.STATE_IDLE -> "idle"
                    Player.STATE_BUFFERING -> "buffering"
                    Player.STATE_READY -> "ready"
                    Player.STATE_ENDED -> "ended"
                    else -> "?"
                }
            }

            override fun onVideoSizeChanged(size: VideoSize) {
                if (size.width > 0 && size.height > 0) {
                    videoSize = IntSize(
                        (size.width * size.pixelWidthHeightRatio).roundToInt(),
                        size.height
                    )
                }
            }
        }
        player.addListener(listener)
        listener.onVideoSizeChanged(player.videoSize)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> player.play()
                Lifecycle.Event.ON_STOP -> player.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }

    // 画面いっぱいに(はみ出した分はカットして)動画を出す
    // ※ 動画の大きさがわかる前から、TextureViewを先に作っておく(これがないと動画が始まらないよ)
    BoxWithConstraints(
        modifier.fillMaxSize().clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val rootW = constraints.maxWidth.toFloat()
        val rootH = constraints.maxHeight.toFloat()
        val known = videoSize.width > 0 && videoSize.height > 0
        val sizeMod = if (known) {
            val s = maxOf(rootW / videoSize.width, rootH / videoSize.height)
            Modifier.requiredSize(
                with(density) { (videoSize.width * s).toDp() },
                with(density) { (videoSize.height * s).toDp() }
            )
        } else {
            Modifier.fillMaxSize()
        }
        AndroidView(
            factory = { c -> TextureView(c) },
            update = { tv ->
                player.setVideoTextureView(tv)
                if (textureView !== tv) textureView = tv
            },
            modifier = sizeMod
        )
    }

    // ガラス用に、動画の1コマを小さくして、ときどき取る
    LaunchedEffect(player, textureView, videoSize, wantFrames, root) {
        val tv = textureView
        if (!wantFrames || tv == null || videoSize.width <= 0 || root.width <= 0 || root.height <= 0) {
            frames.frame = null
            return@LaunchedEffect
        }
        val s = maxOf(
            root.width.toFloat() / videoSize.width,
            root.height.toFloat() / videoSize.height
        )
        val dW = videoSize.width * s
        val dH = videoSize.height * s
        val k = maxOf(1f, maxOf(dW, dH) / 900f)
        val bw = maxOf(16, (dW / k).roundToInt())
        val bh = maxOf(16, (dH / k).roundToInt())
        while (isActive) {
            if (tv.isAvailable && player.isPlaying) {
                val bmp = tv.getBitmap(bw, bh)
                if (bmp != null) frames.frame = bmp.asImageBitmap()
            }
            delay(90)
        }
    }
}
