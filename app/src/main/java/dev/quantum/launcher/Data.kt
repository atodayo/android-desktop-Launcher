package dev.quantum.launcher

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import java.security.MessageDigest
import java.util.Locale

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

    fun lang(): String = sp.getString("lang", "") ?: ""
    fun setLang(tag: String) { sp.edit().putString("lang", tag).apply() }

    fun glass(): String = sp.getString("glass", "frosted") ?: "frosted"
    fun setGlass(tag: String) { sp.edit().putString("glass", tag).apply() }

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

/** アプリ内の言語設定を反映する(空 = スマホの言語に合わせる) */
object LocaleHelper {
    fun wrap(ctx: Context): Context {
        val tag = ctx.getSharedPreferences("quantum", Context.MODE_PRIVATE).getString("lang", "") ?: ""
        if (tag.isEmpty()) {
            Locale.setDefault(Resources.getSystem().configuration.locales[0])
            return ctx
        }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val conf = Configuration(ctx.resources.configuration)
        conf.setLocale(locale)
        return ctx.createConfigurationContext(conf)
    }
}
