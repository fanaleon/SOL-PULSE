package app.pumpviewer.tv

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Idioma de la app: "auto" sigue el idioma de la TV (español si es español, inglés para el resto);
 * "en" y "es" lo fuerzan. Se guarda aparte de los datos de tokens.
 */
object Lang {
    const val AUTO = "auto"
    const val EN = "en"
    const val ES = "es"

    private const val PREFS = "tv_settings"
    private const val KEY = "language"

    fun get(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, AUTO) ?: AUTO

    fun set(context: Context, lang: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, lang).apply()
    }

    /** Orden del botón: Auto → English → Español → Auto. */
    fun next(current: String): String = when (current) {
        AUTO -> EN
        EN -> ES
        else -> AUTO
    }

    /** Contexto con el idioma elegido; si es "auto" devuelve el mismo contexto. */
    fun wrap(base: Context): Context {
        val lang = get(base)
        if (lang == AUTO) return base
        val config = Configuration(base.resources.configuration)
        config.setLocale(Locale.forLanguageTag(lang))
        return base.createConfigurationContext(config)
    }
}
