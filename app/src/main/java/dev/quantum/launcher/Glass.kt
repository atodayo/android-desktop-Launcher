package dev.quantum.launcher

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** ガラスの種類 */
enum class GlassStyle(val tag: String) {
    Off("off"),
    Frosted("frosted"),
    Clear("clear");

    companion object {
        fun fromTag(tag: String): GlassStyle = values().firstOrNull { it.tag == tag } ?: Frosted
    }
}

/** ガラスの後ろに見せる背景(宇宙の背景か、えらんだ写真) */
sealed interface Backdrop {
    class Cosmic(val clock: CosmicClock) : Backdrop
    class Photo(val bitmap: ImageBitmap) : Backdrop
}

val LocalGlassStyle = compositionLocalOf { GlassStyle.Frosted }
val LocalBackdrop = compositionLocalOf<Backdrop?> { null }
val LocalRootSize = compositionLocalOf { IntSize.Zero }

/** 背景そのものを描く(ホーム画面・ロック画面・ガラスの後ろで、おなじものを使うよ) */
@Composable
fun BackdropLayer(b: Backdrop, modifier: Modifier = Modifier) {
    when (b) {
        is Backdrop.Cosmic -> CosmicBackground(b.clock, modifier)
        is Backdrop.Photo -> Box(modifier.fillMaxSize()) {
            Image(
                bitmap = b.bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(Modifier.fillMaxSize().background(Color(0x22000000)))
        }
    }
}

/**
 * ガラスの板。
 * すりガラス: 後ろの背景をぼかして、色をあざやかにして見せる
 * 透明ガラス: ふちが水みたいにぐにゃっとのびて、ふちが光る(Android 13以上。それより古いとレンズみたいに拡大するだけ)
 * オフ: ふつうの半透明
 */
@Composable
fun GlassBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    offFill: Color = Color(0x66000000),
    extraDim: Color = Color.Transparent,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    val style = LocalGlassStyle.current
    val backdrop = LocalBackdrop.current
    val root = LocalRootSize.current
    val density = LocalDensity.current
    var pos by remember { mutableStateOf(Offset.Zero) }
    var panel by remember { mutableStateOf(IntSize.Zero) }
    val glassOn = style != GlassStyle.Off && backdrop != null && root.width > 0 && root.height > 0
    val frosted = style == GlassStyle.Frosted

    // ふちの丸みを、形から調べる
    val radiusPx = remember(panel, shape) {
        if (panel.width <= 0 || panel.height <= 0) {
            0f
        } else {
            val o = shape.createOutline(
                Size(panel.width.toFloat(), panel.height.toFloat()),
                LayoutDirection.Ltr,
                density
            )
            if (o is Outline.Rounded) o.roundRect.topLeftCornerRadius.x else 0f
        }
    }

    val frostedFx = remember(density) {
        if (Build.VERSION.SDK_INT >= 31) {
            runCatching { frostedEffect(with(density) { 20.dp.toPx() }) }.getOrNull()
        } else {
            null
        }
    }
    val liquidFx = if (!frosted && Build.VERSION.SDK_INT >= 33 && panel.width > 0 && panel.height > 0) {
        remember(panel, radiusPx) {
            runCatching {
                liquidEffect(panel.width.toFloat(), panel.height.toFloat(), radiusPx)
            }.getOrNull()
        }
    } else {
        null
    }

    Box(
        modifier
            .onGloballyPositioned { pos = it.positionInRoot() }
            .onSizeChanged { panel = it }
            .clip(shape)
            .background(if (glassOn) Color.Transparent else offFill),
        contentAlignment = contentAlignment
    ) {
        if (glassOn && backdrop != null) {
            val layerMod = when {
                frosted && frostedFx != null -> Modifier.graphicsLayer {
                    renderEffect = frostedFx
                    clip = true
                }
                frosted -> Modifier
                liquidFx != null -> Modifier.graphicsLayer {
                    renderEffect = liquidFx
                    clip = true
                }
                else -> Modifier.graphicsLayer {
                    scaleX = 1.06f
                    scaleY = 1.06f
                }
            }
            // 後ろの背景を、この板の場所に合わせて重ねる
            Box(Modifier.matchParentSize().then(layerMod)) {
                Box(
                    Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(Constraints.fixed(root.width, root.height))
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            p.place(-pos.x.roundToInt(), -pos.y.roundToInt())
                        }
                    }
                ) { BackdropLayer(backdrop) }
            }
            // 色のうすい膜
            Box(
                Modifier
                    .matchParentSize()
                    .background(if (frosted) Color(0x22000000) else Color(0x08FFFFFF))
            )
            if (frosted) {
                Box(Modifier.matchParentSize().background(Color(0x14FFFFFF)))
            }
            if (extraDim != Color.Transparent) {
                Box(Modifier.matchParentSize().background(extraDim))
            }
            // ふちの光
            Box(
                Modifier
                    .matchParentSize()
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(Color(0x99FFFFFF), Color(0x14FFFFFF), Color(0x55FFFFFF))),
                        shape
                    )
            )
        }
        content()
    }
}
