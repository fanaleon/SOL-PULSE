package app.solpulse.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.solpulse.data.PriceChecker

class PriceWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        PriceChecker.refresh(applicationContext)
        return Result.success()
    }
}
