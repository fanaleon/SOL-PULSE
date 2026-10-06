package app.pumpviewer.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import app.pumpviewer.data.PriceChecker
import app.pumpviewer.data.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Servicio en primer plano que chequea precios cada N segundos aunque la app esté cerrada. */
class PriceService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Repo.init(this)
        Notifier.createChannels(this)
        startForeground(NOTIF_ID, Notifier.serviceNotification(this))

        if (job?.isActive != true) {
            job = scope.launch {
                while (isActive) {
                    PriceChecker.refresh(applicationContext)
                    delay(Repo.intervalSec.value * 1000L)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 1001
    }
}

/** Arranca o frena el servicio según los ajustes y si hay tokens cargados. */
object ServiceController {
    fun sync(ctx: Context) {
        Repo.init(ctx)
        val intent = Intent(ctx.applicationContext, PriceService::class.java)
        try {
            if (Repo.serviceEnabled.value && Repo.tokens.value.isNotEmpty()) {
                ContextCompat.startForegroundService(ctx.applicationContext, intent)
            } else {
                ctx.applicationContext.stopService(intent)
            }
        } catch (e: Exception) {
            // Android puede negar el arranque en segundo plano; el worker de respaldo cubre el hueco.
        }
    }
}
