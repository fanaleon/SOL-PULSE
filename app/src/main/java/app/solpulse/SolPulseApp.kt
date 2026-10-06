package app.solpulse

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.solpulse.data.Repo
import app.solpulse.service.Notifier
import app.solpulse.service.PriceWorker
import java.util.concurrent.TimeUnit

class SolPulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Repo.init(this)
        Notifier.createChannels(this)

        // Respaldo: si el sistema mata el servicio, WorkManager sigue chequeando cada 15 min.
        val request = PeriodicWorkRequestBuilder<PriceWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("price-poll", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
