package app.pumpviewer.tv

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.pumpviewer.data.AlertType
import app.pumpviewer.data.DexApi
import app.pumpviewer.data.Fmt
import app.pumpviewer.data.Repo
import app.pumpviewer.data.Token
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AddState {
    data object Idle : AddState
    data object Loading : AddState
    data class Found(val token: Token) : AddState
    data class Error(val message: String) : AddState
}

class TvViewModel(app: Application) : AndroidViewModel(app) {
    val tokens = Repo.tokens
    val alerts = Repo.alerts

    private val _lastOk = MutableStateFlow(0L)
    /** Momento (ms) del último precio bajado bien; 0 si todavía no hubo ninguno. */
    val lastOk: StateFlow<Long> = _lastOk.asStateFlow()

    private val _offline = MutableStateFlow(false)
    val offline: StateFlow<Boolean> = _offline.asStateFlow()

    private val _banner = MutableStateFlow<String?>(null)
    /** Aviso que aparece arriba de todo (alerta disparada, tokens importados). Se borra solo. */
    val banner: StateFlow<String?> = _banner.asStateFlow()

    private val _addState = MutableStateFlow<AddState>(AddState.Idle)
    val addState: StateFlow<AddState> = _addState.asStateFlow()

    private val mintRegex = Regex("[1-9A-HJ-NP-Za-km-z]{32,44}")

    /** Se ejecuta mientras la app está a la vista: baja los precios cada tanto. Se cancela sola al salir. */
    suspend fun pollLoop() {
        while (true) {
            refreshOnce()
            delay(POLL_MS)
        }
    }

    private suspend fun refreshOnce() {
        val mints = Repo.tokens.value.map { it.mint }
        if (mints.isEmpty()) return
        try {
            val quotes = DexApi.fetch(mints)
            if (quotes.isEmpty()) {
                _offline.value = true
                return
            }
            Repo.applyQuotes(quotes)
            _offline.value = false
            _lastOk.value = System.currentTimeMillis()
            checkAlerts()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _offline.value = true
        }
    }

    private fun checkAlerts() {
        val byMint = Repo.tokens.value.associateBy { it.mint }
        Repo.alerts.value.filter { it.enabled }.forEach { alert ->
            val token = byMint[alert.mint] ?: return@forEach
            val p = token.priceUsd
            if (p <= 0.0) return@forEach
            val hit = when (alert.type) {
                AlertType.ABOVE -> p >= alert.target
                AlertType.BELOW -> p <= alert.target
            }
            if (hit) {
                Repo.setAlertEnabled(alert.id, false) // una sola vez; se rearma desde el detalle
                val verb = if (alert.type == AlertType.ABOVE) "subió a" else "bajó a"
                showBanner("${token.symbol} $verb ${Fmt.price(p)}  (alerta en ${Fmt.price(alert.target)})")
            }
        }
    }

    private fun showBanner(text: String) {
        _banner.value = text
        viewModelScope.launch {
            delay(BANNER_MS)
            if (_banner.value == text) _banner.value = null
        }
    }

    // ---------- Agregar tokens ----------

    fun resetAdd() {
        _addState.value = AddState.Idle
    }

    fun lookup(raw: String) {
        val mint = raw.trim()
        if (!mintRegex.matches(mint)) {
            _addState.value = AddState.Error("Esa dirección no parece un mint de Solana válido.")
            return
        }
        if (Repo.tokens.value.any { it.mint == mint }) {
            _addState.value = AddState.Error("Ya estás siguiendo este token.")
            return
        }
        _addState.value = AddState.Loading
        viewModelScope.launch {
            _addState.value = try {
                val token = DexApi.fetch(listOf(mint))[mint]
                if (token == null) {
                    AddState.Error("No encontré pares de trading para ese token en DexScreener.")
                } else {
                    AddState.Found(token)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AddState.Error("No pude conectar. Revisá la conexión de la TV.")
            }
        }
    }

    fun confirmAdd() {
        val s = _addState.value
        if (s is AddState.Found) {
            Repo.addToken(s.token)
            _addState.value = AddState.Idle
            showBanner("Ahora seguís ${s.token.symbol}")
        }
    }

    /** Llega desde el servidor local (formulario web del celu): puede traer varios mints. */
    fun importFromText(text: String) {
        val mints = mintRegex.findAll(text).map { it.value }.distinct()
            .filter { m -> Repo.tokens.value.none { it.mint == m } }
            .toList()
        if (mints.isEmpty()) {
            showBanner("No encontré direcciones nuevas para agregar")
            return
        }
        viewModelScope.launch {
            try {
                val found = DexApi.fetch(mints)
                var added = 0
                for (m in mints) {
                    val t = found[m] ?: continue
                    Repo.addToken(t)
                    added++
                }
                showBanner(
                    if (added > 0) "Se agregaron $added token(s) desde el celu"
                    else "No encontré pares de trading para esas direcciones"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showBanner("No pude conectar para buscar los tokens")
            }
        }
    }

    // ---------- Token y alertas ----------

    fun removeToken(mint: String) = Repo.removeToken(mint)

    fun addAlertPct(token: Token, pct: Double) {
        if (token.priceUsd <= 0.0) return
        val type = if (pct >= 0) AlertType.ABOVE else AlertType.BELOW
        Repo.addAlert(token.mint, type, token.priceUsd * (1.0 + pct / 100.0))
    }

    fun setAlertEnabled(id: String, enabled: Boolean) = Repo.setAlertEnabled(id, enabled)

    fun removeAlert(id: String) = Repo.removeAlert(id)

    private companion object {
        const val POLL_MS = 20_000L
        const val BANNER_MS = 30_000L
    }
}
