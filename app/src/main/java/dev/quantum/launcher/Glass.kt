package dev.quantum.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
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

/** ガラスの後ろに見せる背景(いまは宇宙の背景だけ。画像・動画はあとで足すよ) */
sealed interface Backdrop {
    class Cosmic(val clock: CosmicClock) : Backdrop
}

val LocalGlassStyle = compositionLocalOf { GlassStyle.Frosted }
val LocalBackdrop = compositionLocalOf<Backdrop?> { null }
val LocalRootSize = compositionLocalOf { IntSize.Zero }

@Composable
private fun BackdropContent(b: Backdrop) {
    when (b) {
        is Backdrop.Cosmic -> CosmicBackground(b.clock)
    }
}

/**
 * ガラスの板。
 * すりガラス: 後ろの背景をぼかして見せる
 * 透明ガラス: ぼかさずに、ちょっと拡大して(レンズみたいに)見せる
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
    var pos by remember { mutableStateOf(Offset.Zero) }
    val glassOn = style != GlassStyle.Off && backdrop != null && root.width > 0 && root.height > 0

    Box(
        modifier
            .onGloballyPositioned { pos = it.positionInRoot() }
            .clip(shape)
            .background(if (glassOn) Color.Transparent else offFill),
        contentAlignment = contentAlignment
    ) {
        if (glassOn && backdrop != null) {
            val frosted = style == GlassStyle.Frosted
            val layerMod = if (frosted) {
                Modifier.blur(22.dp, BlurredEdgeTreatment.Rectangle)
            } else {
                Modifier.graphicsLayer {
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
                ) { BackdropContent(backdrop) }
            }
            // 色のうすい膜
            Box(
                Modifier
                    .matchParentSize()
                    .background(if (frosted) Color(0x38101020) else Color(0x10FFFFFF))
            )
            if (frosted) {
                Box(Modifier.matchParentSize().background(Color(0x12FFFFFF)))
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
