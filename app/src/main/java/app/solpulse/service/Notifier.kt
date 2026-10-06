package app.solpulse.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.solpulse.MainActivity
import app.solpulse.R
import app.solpulse.data.AlertType
import app.solpulse.data.Fmt
import app.solpulse.data.PriceAlert
import app.solpulse.data.Token

object Notifier {
    const val CH_ALERTS = "alerts"
    const val CH_SERVICE = "service"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)

        val alerts = NotificationChannel(
            CH_ALERTS, "Alertas de precio", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos cuando un token llega a tu precio objetivo"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 150, 250, 150, 400)
        }
        nm.createNotificationChannel(alerts)

        val service = NotificationChannel(
            CH_SERVICE, "Monitoreo en segundo plano", NotificationManager.IMPORTANCE_LOW
        ).apply { setShowBadge(false) }
        nm.createNotificationChannel(service)
    }

    private fun canNotify(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx,
        0,
        Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun serviceNotification(ctx: Context): Notification =
        NotificationCompat.Builder(ctx, CH_SERVICE)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("SolPulse activo")
            .setContentText("Vigilando tus tokens")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openApp(ctx))
            .build()

    @SuppressLint("MissingPermission")
    fun alert(ctx: Context, token: Token, alert: PriceAlert) {
        if (!canNotify(ctx)) return
        val up = alert.type == AlertType.ABOVE
        val title = if (up) {
            "▲ ${token.symbol} superó ${Fmt.price(alert.target)}"
        } else {
            "▼ ${token.symbol} cayó a ${Fmt.price(alert.target)}"
        }
        val text = "Ahora: ${Fmt.price(token.priceUsd)}  ·  24h ${Fmt.pct(token.change24h)}"
        val n = NotificationCompat.Builder(ctx, CH_ALERTS)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .build()
        NotificationManagerCompat.from(ctx).notify(alert.id.hashCode(), n)
    }

    @SuppressLint("MissingPermission")
    fun test(ctx: Context) {
        if (!canNotify(ctx)) return
        val n = NotificationCompat.Builder(ctx, CH_ALERTS)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("▲ SolPulse: prueba de alerta")
            .setContentText("Si lo ves en el reloj, todo está bien configurado")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .build()
        NotificationManagerCompat.from(ctx).notify(9001, n)
    }
}
