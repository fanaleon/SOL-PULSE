package app.pumpviewer.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Fuente única de verdad. La usan la UI, el servicio, el worker y el widget.
 * Persiste en SharedPreferences (JSON) y expone StateFlows.
 */
object Repo {
    private const val MAX_HISTORY = 120

    private lateinit var prefs: SharedPreferences
    @Volatile
    private var ready = false
    private val lock = Any()

    private val _tokens = MutableStateFlow<List<Token>>(emptyList())
    val tokens: StateFlow<List<Token>> = _tokens.asStateFlow()

    private val _alerts = MutableStateFlow<List<PriceAlert>>(emptyList())
    val alerts: StateFlow<List<PriceAlert>> = _alerts.asStateFlow()

    private val _intervalSec = MutableStateFlow(30)
    val intervalSec: StateFlow<Int> = _intervalSec.asStateFlow()

    private val _serviceEnabled = MutableStateFlow(true)
    val serviceEnabled: StateFlow<Boolean> = _serviceEnabled.asStateFlow()

    fun init(context: Context) {
        if (ready) return
        synchronized(lock) {
            if (ready) return
            prefs = context.applicationContext.getSharedPreferences("pumpviewer", Context.MODE_PRIVATE)
            _tokens.value = readTokens(prefs.getString("tokens", null))
            _alerts.value = readAlerts(prefs.getString("alerts", null))
            _intervalSec.value = prefs.getInt("interval", 30)
            _serviceEnabled.value = prefs.getBoolean("service", true)
            ready = true
        }
    }

    // ---------- Tokens ----------

    fun addToken(t: Token) = synchronized(lock) {
        if (_tokens.value.any { it.mint == t.mint }) return@synchronized
        val seeded = t.copy(
            history = if (t.priceUsd > 0) listOf(t.priceUsd) else emptyList(),
            updatedAt = System.currentTimeMillis()
        )
        _tokens.value = _tokens.value + seeded
        saveTokens()
    }

    fun removeToken(mint: String) = synchronized(lock) {
        _tokens.value = _tokens.value.filterNot { it.mint == mint }
        _alerts.value = _alerts.value.filterNot { it.mint == mint }
        saveTokens()
        saveAlerts()
    }

    fun applyQuotes(quotes: Map<String, Token>) = synchronized(lock) {
        val now = System.currentTimeMillis()
        _tokens.value = _tokens.value.map { old ->
            val q = quotes[old.mint]
            if (q == null || q.priceUsd <= 0.0) return@map old
            q.copy(history = (old.history + q.priceUsd).takeLast(MAX_HISTORY), updatedAt = now)
        }
        saveTokens()
    }

    // ---------- Alertas ----------

    fun addAlert(mint: String, type: AlertType, target: Double) = synchronized(lock) {
        _alerts.value = _alerts.value + PriceAlert(UUID.randomUUID().toString(), mint, type, target, true)
        saveAlerts()
    }

    fun setAlertEnabled(id: String, enabled: Boolean) = synchronized(lock) {
        _alerts.value = _alerts.value.map { if (it.id == id) it.copy(enabled = enabled) else it }
        saveAlerts()
    }

    fun removeAlert(id: String) = synchronized(lock) {
        _alerts.value = _alerts.value.filterNot { it.id == id }
        saveAlerts()
    }

    // ---------- Ajustes ----------

    fun setIntervalSec(sec: Int) {
        _intervalSec.value = sec
        prefs.edit().putInt("interval", sec).apply()
    }

    fun setServiceEnabled(enabled: Boolean) {
        _serviceEnabled.value = enabled
        prefs.edit().putBoolean("service", enabled).apply()
    }

    // ---------- Persistencia ----------

    private fun saveTokens() {
        val arr = JSONArray()
        _tokens.value.forEach { arr.put(it.toJson()) }
        prefs.edit().putString("tokens", arr.toString()).apply()
    }

    private fun saveAlerts() {
        val arr = JSONArray()
        _alerts.value.forEach { a ->
            arr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("mint", a.mint)
                    .put("type", a.type.name)
                    .put("target", a.target)
                    .put("enabled", a.enabled)
            )
        }
        prefs.edit().putString("alerts", arr.toString()).apply()
    }

    private fun Token.toJson(): JSONObject = JSONObject()
        .put("mint", mint)
        .put("symbol", symbol)
        .put("name", name)
        .put("imageUrl", imageUrl ?: "")
        .put("priceUsd", priceUsd)
        .put("change5m", change5m)
        .put("change1h", change1h)
        .put("change24h", change24h)
        .put("liquidityUsd", liquidityUsd)
        .put("volume24h", volume24h)
        .put("marketCap", marketCap)
        .put("pairUrl", pairUrl ?: "")
        .put("updatedAt", updatedAt)
        .put("history", JSONArray(history))

    private fun readTokens(s: String?): List<Token> {
        if (s.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(s)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val h = o.optJSONArray("history")
                Token(
                    mint = o.getString("mint"),
                    symbol = o.optString("symbol", "?"),
                    name = o.optString("name", ""),
                    imageUrl = o.optString("imageUrl", "").ifBlank { null },
                    priceUsd = o.optDouble("priceUsd", 0.0),
                    change5m = o.optDouble("change5m", 0.0),
                    change1h = o.optDouble("change1h", 0.0),
                    change24h = o.optDouble("change24h", 0.0),
                    liquidityUsd = o.optDouble("liquidityUsd", 0.0),
                    volume24h = o.optDouble("volume24h", 0.0),
                    marketCap = o.optDouble("marketCap", 0.0),
                    pairUrl = o.optString("pairUrl", "").ifBlank { null },
                    updatedAt = o.optLong("updatedAt", 0L),
                    history = (0 until (h?.length() ?: 0)).map { j -> h!!.getDouble(j) }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun readAlerts(s: String?): List<PriceAlert> {
        if (s.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(s)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                PriceAlert(
                    id = o.getString("id"),
                    mint = o.getString("mint"),
                    type = AlertType.valueOf(o.getString("type")),
                    target = o.getDouble("target"),
                    enabled = o.optBoolean("enabled", true)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
