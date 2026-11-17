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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

val QBlue = Color(0xFF4FC3F7)
val QViolet = Color(0xFFB388FF)

/** Atlasのロゴ: くるくるまわる、黒い銀河(うずまきの腕と、光るまんなか) */
@Composable
fun AppLogo(modifier: Modifier = Modifier, spin: Boolean = false) {
    val t = rememberInfiniteTransition(label = "logo")
    val angle by t.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "galaxySpin"
    )
    val twinkle by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "galaxyTwinkle"
    )
    Canvas(modifier) {
        val w = size.minDimension
        val c = center
        val brush = Brush.radialGradient(
            0f to Color.White,
            0.35f to Color(0xFFE3D8FF),
            0.7f to Color(0xFF9B8CFF),
            1f to Color(0x884FC3F7),
            center = c,
            radius = w * 0.46f
        )

        fun arm(base: Float): Path {
            val p = Path()
            val n = 56
            for (i in 0..n) {
                val u = i / n.toFloat()
                val th = base + u * 3.6f
                val r = w * (0.06f + 0.38f * u)
                val x = c.x + r * cos(th)
                val y = c.y + r * sin(th)
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            return p
        }

        val arm1 = arm(0f)
        val arm2 = arm(PI.toFloat())

        rotate(-25f, pivot = c) {
            scale(1f, 0.72f, pivot = c) {
                rotate(if (spin) angle else 0f, pivot = c) {
                    for ((width, alpha) in listOf(0.13f to 0.22f, 0.075f to 0.5f, 0.032f to 1f)) {
                        val st = Stroke(width = w * width, cap = StrokeCap.Round)
                        drawPath(arm1, brush, alpha = alpha, style = st)
                        drawPath(arm2, brush, alpha = alpha, style = st)
                    }
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White, Color(0xFFE3D8FF), Color.Transparent),
                        center = c,
                        radius = w * 0.17f
                    ),
                    radius = w * 0.17f,
                    center = c
                )
            }
        }

        drawCircle(Color.White.copy(alpha = 0.4f + 0.6f * twinkle.value), w * 0.022f, Offset(c.x + w * 0.40f, c.y - w * 0.32f))
        drawCircle(Color.White.copy(alpha = 1f - 0.6f * twinkle.value), w * 0.016f, Offset(c.x - w * 0.38f, c.y + w * 0.30f))
        drawCircle(Color.White.copy(alpha = 0.3f + 0.5f * twinkle.value), w * 0.012f, Offset(c.x - w * 0.30f, c.y - w * 0.40f))
    }
}

/** 古い名前(まえの呼びかたのまま使えるようにしてあるよ) */
@Composable
fun AtomLogo(modifier: Modifier = Modifier, spin: Boolean = false) {
    AppLogo(modifier, spin)
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

@Composable
fun CosmicBackground(modifier: Modifier = Modifier) {
    CosmicBackground(rememberCosmicClock(), modifier)
}

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
        drawCircle(Color.White.copy(alpha = 0.4f + 0.6f * twinkle), w * 0.022f, Offset(c.x + w * 0.40f, c.y - w * 0.32f))
        drawCircle(Color.White.copy(alpha = 1f - 0.6f * twinkle), w * 0.016f, Offset(c.x - w * 0.38f, c.y + w * 0.30f))
        drawCircle(Color.White.copy(alpha = 0.3f + 0.5f * twinkle), w * 0.012f, Offset(c.x - w * 0.30f, c.y - w * 0.40f))
        }
    }
}
