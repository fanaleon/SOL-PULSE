package app.pumpviewer

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.pumpviewer.data.Repo
import app.pumpviewer.service.Notifier
import app.pumpviewer.service.PriceWorker
import java.util.concurrent.TimeUnit

class PumpViewerApp : Application() {
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
