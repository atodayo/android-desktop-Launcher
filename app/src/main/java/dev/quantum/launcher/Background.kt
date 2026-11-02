package dev.quantum.launcher

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

val QBlue = Color(0xFF4FC3F7)
val QViolet = Color(0xFFB388FF)

/** 原子ロゴ(3つの軌道) */
@Composable
fun AtomLogo(modifier: Modifier = Modifier, spin: Boolean = false) {
    val t = rememberInfiniteTransition(label = "atom")
    val angle by t.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "atomAngle"
    )
    Canvas(modifier) {
        val w = size.minDimension
        val brush = Brush.linearGradient(listOf(QBlue, QViolet))
        val stroke = Stroke(width = w * 0.07f)
        rotate(if (spin) angle else 0f) {
            for (k in 0..2) {
                rotate(k * 60f) {
                    drawOval(
                        brush = brush,
                        topLeft = Offset(w * 0.05f, w * 0.30f),
                        size = Size(w * 0.9f, w * 0.4f),
                        style = stroke
                    )
                }
            }
            drawCircle(brush = brush, radius = w * 0.09f, center = center)
        }
    }
}

/** 背景のアニメーションの時計(ガラスの中の背景とズレないように、みんなで共有する) */
class CosmicClock(val orbit: State<Float>, val twinkle: State<Float>)

@Composable
fun rememberCosmicClock(): CosmicClock {
    val t = rememberInfiniteTransition(label = "cosmic")
    val orbit = t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing)),
        label = "orbit"
    )
    val twinkle = t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(3500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "twinkle"
    )
    return remember { CosmicClock(orbit, twinkle) }
}

/** 時計を自分で作るバージョン(ロック画面などで使う) */
@Composable
fun CosmicBackground(modifier: Modifier = Modifier) {
    CosmicBackground(rememberCosmicClock(), modifier)
}

/** 宇宙っぽい背景:紫とシアンの星雲、またたく星、軌道を回る白い点 */
@Composable
fun CosmicBackground(clock: CosmicClock, modifier: Modifier = Modifier) {
    val stars = remember {
        val r = Random(7)
        List(110) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat()) }
    }

    Canvas(modifier.fillMaxSize()) {
        val orbit = clock.orbit.value
        val twinkle = clock.twinkle.value
        drawRect(Color(0xFF0B0A12))
        drawRect(
            Brush.radialGradient(
                listOf(Color(0xAA5B3FD0), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.22f),
                radius = size.width * 1.0f
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(Color(0x77235A6A), Color.Transparent),
                center = Offset(size.width * 0.1f, size.height * 0.82f),
                radius = size.width * 1.0f
            )
        )

        for ((x, y, s) in stars) {
            val a = 0.15f + 0.6f * ((sin(twinkle * 2f + s * 6.28f) + 1f) / 2f)
            drawCircle(
                Color.White.copy(alpha = a),
                radius = 0.6f + s * 1.2f,
                center = Offset(x * size.width, y * size.height)
            )
        }

        val c = Offset(size.width * 0.55f, size.height * 0.42f)
        rotate(-22f, pivot = c) {
            val rx = size.width * 0.75f
            val ry = rx * 0.3f
            val rings = listOf(1f to 0.7f, 0.78f to 0.28f, 1.2f to 0.18f)
            for ((k, alpha) in rings) {
                drawOval(
                    color = Color(0xFF7C86FF).copy(alpha = alpha),
                    topLeft = Offset(c.x - rx * k, c.y - ry * k),
                    size = Size(2f * rx * k, 2f * ry * k),
                    style = Stroke(1.5.dp.toPx())
                )
            }
            val th = orbit * 2f * PI.toFloat()
            val pos = Offset(c.x + rx * cos(th), c.y + ry * sin(th))
            drawCircle(Color.White.copy(alpha = 0.25f), radius = 12.dp.toPx(), center = pos)
            drawCircle(Color.White, radius = 4.dp.toPx(), center = pos)
        }
    }
}
