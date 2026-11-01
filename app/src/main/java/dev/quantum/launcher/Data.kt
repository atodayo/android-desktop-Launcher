package dev.quantum.launcher

import android.content.Context
import android.content.Intent
import java.security.MessageDigest

data class AppItem(val label: String, val pkg: String)

/** ランチャーのプロセスが生きてる間だけ「ロック解除済み」を覚えておく */
object Session {
    var unlocked = false
}

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("quantum", Context.MODE_PRIVATE)

    fun dock(): List<String> =
        (sp.getString("dock", "") ?: "").split(",").filter { it.isNotBlank() }

    fun saveDock(list: List<String>) {
        sp.edit().putString("dock", list.joinToString(",")).apply()
    }

    fun hasPw(): Boolean = sp.contains("pw")
    fun setPw(p: String) { sp.edit().putString("pw", hash(p)).apply() }
    fun checkPw(p: String): Boolean = sp.getString("pw", null) == hash(p)

    private fun hash(p: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(("quantum:$p").toByteArray())
            .joinToString("") { "%02x".format(it) }
}

fun loadApps(ctx: Context): List<AppItem> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map { AppItem(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        .filter { it.pkg != ctx.packageName }
        .distinctBy { it.pkg }
        .sortedBy { it.label.lowercase() }
}

fun launchApp(ctx: Context, pkg: String) {
    ctx.packageManager.getLaunchIntentForPackage(pkg)?.let { ctx.startActivity(it) }
}
