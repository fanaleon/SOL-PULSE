package app.solpulse.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.solpulse.data.AlertType
import app.solpulse.data.DexApi
import app.solpulse.data.PriceChecker
import app.solpulse.data.Repo
import app.solpulse.data.Token
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

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val tokens = Repo.tokens
    val alerts = Repo.alerts

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _addState = MutableStateFlow<AddState>(AddState.Idle)
    val addState: StateFlow<AddState> = _addState.asStateFlow()

    private val mintRegex = Regex("^[1-9A-HJ-NP-Za-km-z]{32,44}$")

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _refreshing.value = true
            PriceChecker.refresh(getApplication())
            if (!silent) _refreshing.value = false
        }
    }

    fun resetAdd() {
        _addState.value = AddState.Idle
    }

    fun lookup(rawMint: String) {
        val mint = rawMint.trim()
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
            } catch (e: Exception) {
                AddState.Error("No pude conectar. Revisá tu conexión e intentá de nuevo.")
            }
        }
    }

    fun confirmAdd() {
        val s = _addState.value
        if (s is AddState.Found) {
            Repo.addToken(s.token)
            _addState.value = AddState.Idle
            refresh(silent = true)
        }
    }

    fun removeToken(mint: String) = Repo.removeToken(mint)

    fun addAlert(mint: String, type: AlertType, target: Double) = Repo.addAlert(mint, type, target)

    fun setAlertEnabled(id: String, enabled: Boolean) = Repo.setAlertEnabled(id, enabled)

    fun removeAlert(id: String) = Repo.removeAlert(id)
}
