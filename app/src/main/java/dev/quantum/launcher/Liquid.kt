package dev.quantum.launcher

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * 透明ガラス(リキッドガラス)用の描画プログラム(AGSL)。
 * ふちに近いほど、中の絵を引きのばして(レンズみたいに)見せて、ふちを光らせるよ。
 */
private const val LIQUID_AGSL = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float band;
uniform float strength;

float sdRound(float2 p, float2 hs, float r) {
    float2 q = abs(p) - hs + r;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

half4 main(float2 fc) {
    float2 hs = size * 0.5;
    float2 p = fc - hs;
    float d = sdRound(p, hs, radius);
    float inside = -d;
    float t = clamp(1.0 - inside / band, 0.0, 1.0);
    t = t * t;
    float2 n = float2(
        sdRound(p + float2(1.0, 0.0), hs, radius) - sdRound(p - float2(1.0, 0.0), hs, radius),
        sdRound(p + float2(0.0, 1.0), hs, radius) - sdRound(p - float2(0.0, 1.0), hs, radius)
    );
    float nl = length(n);
    if (nl > 0.0001) {
        n = n / nl;
    } else {
        n = float2(0.0, 0.0);
    }
    float2 q = fc - n * (strength * t * band);
    half4 c = content.eval(q);
    float rim = smoothstep(2.5, 0.0, inside);
    float spec = 0.5 + 0.5 * dot(n, normalize(float2(-0.7, -0.7)));
    float glow = rim * (0.18 + 0.32 * spec);
    c.rgb = c.rgb + half3(glow);
    return c;
}
"""

/** 透明ガラス: Android 13以上で使えるよ */
@RequiresApi(33)
fun liquidEffect(wPx: Float, hPx: Float, radiusPx: Float): androidx.compose.ui.graphics.RenderEffect {
    val shader = RuntimeShader(LIQUID_AGSL)
    shader.setFloatUniform("size", wPx, hPx)
    shader.setFloatUniform("radius", radiusPx)
    val band = (minOf(wPx, hPx) * 0.35f).coerceIn(8f, 60f)
    shader.setFloatUniform("band", band)
    shader.setFloatUniform("strength", 0.9f)
    return AndroidRenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
}

/** すりガラス: ぼかして、色を少しあざやかに、明るくするよ(Android 12以上) */
@RequiresApi(31)
fun frostedEffect(blurPx: Float): androidx.compose.ui.graphics.RenderEffect {
    val blur = AndroidRenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP)
    val sat = ColorMatrix().apply { setSaturation(1.6f) }
    val bright = ColorMatrix(
        floatArrayOf(
            1.08f, 0f, 0f, 0f, 6f,
            0f, 1.08f, 0f, 0f, 6f,
            0f, 0f, 1.08f, 0f, 6f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    sat.postConcat(bright)
    val fx = AndroidRenderEffect.createColorFilterEffect(ColorMatrixColorFilter(sat), blur)
    return fx.asComposeRenderEffect()
}
