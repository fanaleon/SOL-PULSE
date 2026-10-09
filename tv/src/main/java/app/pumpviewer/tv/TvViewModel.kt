package app.pumpviewer.tv

import android.app.Application
import androidx.annotation.StringRes
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

    private val _sol = MutableStateFlow<Token?>(null)
    /** Precio de SOL (para la barra de abajo). */
    val sol: StateFlow<Token?> = _sol.asStateFlow()

    private val _place = MutableStateFlow(PlaceStore.load(app))
    val place: StateFlow<Place> = _place.asStateFlow()

    private val _weather = MutableStateFlow<WeatherInfo?>(null)
    val weather: StateFlow<WeatherInfo?> = _weather.asStateFlow()

    private val _placeResults = MutableStateFlow<List<Place>>(emptyList())
    val placeResults: StateFlow<List<Place>> = _placeResults.asStateFlow()

    private val _placeMessage = MutableStateFlow<String?>(null)
    val placeMessage: StateFlow<String?> = _placeMessage.asStateFlow()

    private var lastWeatherAt = 0L

    private val mintRegex = Regex("[1-9A-HJ-NP-Za-km-z]{32,44}")

    /** Texto en el idioma elegido (los mensajes se arman acá, fuera de la pantalla). */
    private fun str(@StringRes id: Int, vararg args: Any): String =
        Lang.wrap(getApplication<Application>()).getString(id, *args)

    /** Se ejecuta mientras la app está a la vista: baja los precios cada tanto. Se cancela sola al salir. */
    suspend fun pollLoop() {
        while (true) {
            refreshOnce()
            refreshSol()
            if (System.currentTimeMillis() - lastWeatherAt > WEATHER_MS) refreshWeather()
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

    private suspend fun refreshSol() {
        try {
            val quote = DexApi.fetch(listOf(SOL_MINT))[SOL_MINT]
            if (quote != null && quote.priceUsd > 0.0) _sol.value = quote
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // la barra de abajo es opcional: se queda con el último valor
        }
    }

    private suspend fun refreshWeather() {
        try {
            _weather.value = WeatherApi.current(_place.value)
            lastWeatherAt = System.currentTimeMillis()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // se reintenta en el próximo ciclo
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
                val res = if (alert.type == AlertType.ABOVE) R.string.banner_alert_above else R.string.banner_alert_below
                showBanner(str(res, token.symbol, Fmt.price(p), Fmt.price(alert.target)))
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
            _addState.value = AddState.Error(str(R.string.err_invalid_mint))
            return
        }
        if (Repo.tokens.value.any { it.mint == mint }) {
            _addState.value = AddState.Error(str(R.string.err_already_following))
            return
        }
        _addState.value = AddState.Loading
        viewModelScope.launch {
            _addState.value = try {
                val token = DexApi.fetch(listOf(mint))[mint]
                if (token == null) {
                    AddState.Error(str(R.string.err_not_found))
                } else {
                    AddState.Found(token)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AddState.Error(str(R.string.err_connection))
            }
        }
    }

    fun confirmAdd() {
        val s = _addState.value
        if (s is AddState.Found) {
            Repo.addToken(s.token)
            _addState.value = AddState.Idle
            showBanner(str(R.string.banner_following, s.token.symbol))
        }
    }

    /** Llega desde el servidor local (formulario web del celu): puede traer varios mints. */
    fun importFromText(text: String) {
        val mints = mintRegex.findAll(text).map { it.value }.distinct()
            .filter { m -> Repo.tokens.value.none { it.mint == m } }
            .toList()
        if (mints.isEmpty()) {
            showBanner(str(R.string.banner_no_new))
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
                    if (added > 0) str(R.string.banner_imported, added)
                    else str(R.string.banner_import_not_found)
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showBanner(str(R.string.banner_import_connection))
            }
        }
    }

    // ---------- Ciudad del clima ----------

    fun searchPlace(query: String) {
        if (query.isBlank()) return
        _placeMessage.value = null
        viewModelScope.launch {
            try {
                val found = WeatherApi.search(query, Lang.language(getApplication<Application>()))
                _placeResults.value = found
                _placeMessage.value = if (found.isEmpty()) str(R.string.place_not_found) else null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _placeMessage.value = str(R.string.err_connection)
            }
        }
    }

    fun clearPlaceSearch() {
        _placeResults.value = emptyList()
        _placeMessage.value = null
    }

    fun setPlace(place: Place) {
        PlaceStore.save(getApplication<Application>(), place)
        _place.value = place
        _weather.value = null
        viewModelScope.launch { refreshWeather() }
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
        const val WEATHER_MS = 10 * 60_000L
        const val SOL_MINT = "So11111111111111111111111111111111111111112"
        const val BANNER_MS = 30_000L
    }
}
