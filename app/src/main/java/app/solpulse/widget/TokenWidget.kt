package app.solpulse.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.solpulse.MainActivity
import app.solpulse.data.Fmt
import app.solpulse.data.Repo
import app.solpulse.data.Token

private val WBg = Color(0xFF0B0B14)
private val WPurple = Color(0xFF9945FF)
private val WGreen = Color(0xFF14F195)
private val WRed = Color(0xFFFF4D6D)
private val WWhite = Color(0xFFF2F2FA)
private val WDim = Color(0xFF8B8BA3)

class TokenWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Repo.init(context)
        val tokens = Repo.tokens.value.take(5)
        provideContent { WidgetContent(tokens) }
    }
}

class TokenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TokenWidget()
}

@Composable
private fun WidgetContent(tokens: List<Token>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WBg))
            .cornerRadius(22.dp)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Text(
            "SolPulse",
            style = TextStyle(
                color = ColorProvider(WPurple),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        )
        Spacer(GlanceModifier.height(6.dp))

        if (tokens.isEmpty()) {
            Text(
                "Agregá un token desde la app",
                style = TextStyle(color = ColorProvider(WDim), fontSize = 12.sp)
            )
        }

        tokens.forEach { t ->
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    t.symbol,
                    modifier = GlanceModifier.defaultWeight(),
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(WWhite),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    Fmt.price(t.priceUsd),
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(WWhite), fontSize = 13.sp)
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    Fmt.pct(t.change24h),
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(if (t.change24h >= 0) WGreen else WRed),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
