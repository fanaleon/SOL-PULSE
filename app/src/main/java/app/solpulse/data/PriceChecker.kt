package app.solpulse.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import app.solpulse.service.Notifier
import app.solpulse.widget.TokenWidget
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Un ciclo completo: baja precios, guarda, dispara alertas y refresca el widget. */
object PriceChecker {
    private val mutex = Mutex()

    /** @return true si se actualizó (o no había nada que actualizar), false si falló la red. */
    suspend fun refresh(context: Context): Boolean {
        val ctx = context.applicationContext
        Repo.init(ctx)
        return mutex.withLock {
            val mints = Repo.tokens.value.map { it.mint }
            if (mints.isEmpty()) return@withLock true

            val fetched = try {
                DexApi.fetch(mints)
            } catch (e: Exception) {
                return@withLock false
            }
            if (fetched.isEmpty()) return@withLock false

            Repo.applyQuotes(fetched)
            evaluateAlerts(ctx)
            try {
                TokenWidget().updateAll(ctx)
            } catch (e: Exception) {
                // el widget es opcional
            }
            true
        }
    }

    private fun evaluateAlerts(ctx: Context) {
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
                Repo.setAlertEnabled(alert.id, false) // una sola vez; se rearma desde la app
                Notifier.alert(ctx, token, alert)
            }
        }
    }
}
