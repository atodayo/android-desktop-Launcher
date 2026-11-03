package dev.quantum.launcher

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 背景のしゅるい: "cosmic"(標準の宇宙) か "image"(えらんだ写真) */
fun getBgMode(ctx: Context): String =
    ctx.getSharedPreferences("quantum", Context.MODE_PRIVATE).getString("bg_mode", "cosmic") ?: "cosmic"

fun setBgMode(ctx: Context, mode: String) {
    ctx.getSharedPreferences("quantum", Context.MODE_PRIVATE).edit().putString("bg_mode", mode).apply()
}

private fun bgFile(ctx: Context) = File(ctx.filesDir, "bg_image")

/** えらんだ写真を、アプリの中にコピーする */
suspend fun importImage(ctx: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        ctx.contentResolver.openInputStream(uri)!!.use { input ->
            bgFile(ctx).outputStream().use { out -> input.copyTo(out) }
        }
        true
    }.getOrDefault(false)
}

/** 写真を読みこむ(大きすぎる写真は小さくするよ) */
fun loadBgBitmap(ctx: Context, maxSide: Int): ImageBitmap? {
    val f = bgFile(ctx)
    if (!f.exists()) return null
    return runCatching {
        if (Build.VERSION.SDK_INT >= 28) {
            val src = ImageDecoder.createSource(f)
            ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                val w = info.size.width
                val h = info.size.height
                val scale = maxSide.toFloat() / maxOf(w, h)
                if (scale < 1f) {
                    decoder.setTargetSize((w * scale).roundToInt(), (h * scale).roundToInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }.asImageBitmap()
        } else {
            BitmapFactory.decodeFile(f.path)?.asImageBitmap()
        }
    }.getOrNull()
}
