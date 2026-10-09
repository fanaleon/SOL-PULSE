package app.pumpviewer.tv

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pumpviewer.data.Token
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------------------------------------------------------------------------------------------
// Íconos del clima (dibujados: no dependen de que la TV tenga fuente de emojis)
// ---------------------------------------------------------------------------------------------

private val CloudLight = Color(0xFFD3DAF5)
private val CloudDark = Color(0xFF9AA4D0)
private val MoonColor = Color(0xFFDDE3FF)

@Composable
fun SkyIcon(sky: Sky, isDay: Boolean, iconSize: Dp) {
    Canvas(Modifier.size(iconSize)) {
        val s = size.width

        fun sun(cx: Float, cy: Float, r: Float) {
            drawCircle(Amber, r, Offset(cx, cy))
            for (i in 0 until 8) {
                val a = (i * PI / 4).toFloat()
                drawLine(
                    Amber,
                    Offset(cx + cos(a) * r * 1.35f, cy + sin(a) * r * 1.35f),
                    Offset(cx + cos(a) * r * 1.8f, cy + sin(a) * r * 1.8f),
                    strokeWidth = s * 0.05f,
                    cap = StrokeCap.Round
                )
            }
        }

        fun moon(cx: Float, cy: Float, r: Float) {
            val full = Path().apply { addOval(Rect(Offset(cx, cy), r)) }
            val cut = Path().apply { addOval(Rect(Offset(cx + r * 0.55f, cy - r * 0.35f), r * 0.85f)) }
            drawPath(Path.combine(PathOperation.Difference, full, cut), MoonColor)
        }

        fun cloud(cx: Float, cy: Float, k: Float, color: Color) {
            drawCircle(color, s * 0.17f * k, Offset(cx - s * 0.14f * k, cy + s * 0.02f * k))
            drawCircle(color, s * 0.22f * k, Offset(cx + s * 0.02f * k, cy - s * 0.06f * k))
            drawCircle(color, s * 0.16f * k, Offset(cx + s * 0.2f * k, cy + s * 0.03f * k))
            drawRoundRect(
                color,
                topLeft = Offset(cx - s * 0.31f * k, cy + s * 0.02f * k),
                size = Size(s * 0.62f * k, s * 0.17f * k),
                cornerRadius = CornerRadius(s * 0.085f * k)
            )
        }

        fun streaks(count: Int, color: Color) {
            val w = s * 0.045f
            for (i in 0 until count) {
                val x = s * (0.33f + 0.17f * i)
                drawLine(color, Offset(x, s * 0.68f), Offset(x - s * 0.06f, s * 0.86f), strokeWidth = w, cap = StrokeCap.Round)
            }
        }

        when (sky) {
            Sky.CLEAR -> if (isDay) sun(s * 0.5f, s * 0.5f, s * 0.2f) else moon(s * 0.5f, s * 0.5f, s * 0.3f)
            Sky.PARTLY -> {
                if (isDay) sun(s * 0.36f, s * 0.36f, s * 0.15f) else moon(s * 0.36f, s * 0.36f, s * 0.22f)
                cloud(s * 0.56f, s * 0.62f, 0.9f, CloudLight)
            }
            Sky.CLOUDY -> cloud(s * 0.5f, s * 0.5f, 1.2f, CloudLight)
            Sky.FOG -> {
                cloud(s * 0.5f, s * 0.36f, 1.0f, CloudLight)
                for (i in 0 until 3) {
                    val y = s * (0.66f + 0.11f * i)
                    drawLine(CloudDark, Offset(s * 0.2f, y), Offset(s * 0.8f, y), strokeWidth = s * 0.05f, cap = StrokeCap.Round)
                }
            }
            Sky.DRIZZLE -> {
                cloud(s * 0.5f, s * 0.4f, 1.0f, CloudDark)
                streaks(2, Cyan)
            }
            Sky.RAIN -> {
                cloud(s * 0.5f, s * 0.4f, 1.0f, CloudDark)
                streaks(3, Cyan)
            }
            Sky.SNOW -> {
                cloud(s * 0.5f, s * 0.4f, 1.0f, CloudLight)
                for (i in 0 until 3) {
                    drawCircle(Color.White, s * 0.04f, Offset(s * (0.33f + 0.17f * i), s * (0.74f + 0.06f * (i % 2))))
                }
            }
            Sky.STORM -> {
                cloud(s * 0.5f, s * 0.38f, 1.0f, CloudDark)
                val bolt = Path().apply {
                    moveTo(s * 0.54f, s * 0.58f)
                    lineTo(s * 0.4f, s * 0.8f)
                    lineTo(s * 0.5f, s * 0.8f)
                    lineTo(s * 0.44f, s * 0.97f)
                    lineTo(s * 0.64f, s * 0.7f)
                    lineTo(s * 0.53f, s * 0.7f)
                    close()
                }
                drawPath(bolt, Amber)
            }
        }
    }
}

@Composable
private fun skyText(sky: Sky): String = stringResource(
    when (sky) {
        Sky.CLEAR -> R.string.sky_clear
        Sky.PARTLY -> R.string.sky_partly
        Sky.CLOUDY -> R.string.sky_cloudy
        Sky.FOG -> R.string.sky_fog
        Sky.DRIZZLE -> R.string.sky_drizzle
        Sky.RAIN -> R.string.sky_rain
        Sky.SNOW -> R.string.sky_snow
        Sky.STORM -> R.string.sky_storm
    }
)

private fun deg(v: Double): String = if (v.isNaN()) "–°" else "${v.roundToInt()}°"

// ---------------------------------------------------------------------------------------------
// Barra de abajo del tablero: clima a la izquierda, precio de SOL a la derecha
// ---------------------------------------------------------------------------------------------

@Composable
fun Dock(sol: Token?, weather: WeatherInfo?, place: Place, onWeatherClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(Surface1.copy(alpha = 0.92f))
            .border(1.dp, Border, RoundedCornerShape(30.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // --- Clima (se enfoca; OK abre la pantalla para cambiar de ciudad) ---
        Row(
            Modifier
                .tvClickable(RoundedCornerShape(22.dp), container = Color.Transparent, onClick = onWeatherClick)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (weather != null) SkyIcon(weather.sky, weather.isDay, 56.dp) else Spacer(Modifier.size(56.dp))
            Spacer(Modifier.width(14.dp))
            Text(
                if (weather != null) deg(weather.temp) else "–°",
                style = tnum(44.sp, FontWeight.ExtraBold)
            )
            Spacer(Modifier.width(18.dp))
            Column {
                Text(
                    if (weather != null) "${skyText(weather.sky)} · ${place.name}" else stringResource(R.string.weather_loading),
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (weather != null) {
                    Text(
                        "↑ ${deg(weather.max)}   ↓ ${deg(weather.min)}   ·   " +
                            stringResource(R.string.wind_kmh, weather.windKmh.roundToInt()),
                        style = tnum(16.sp, FontWeight.Medium, TextDim),
                        maxLines = 1
                    )
                }
            }
        }

        // --- Precio de SOL ---
        Row(
            Modifier.padding(horizontal = 22.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TokenLogo(sol?.imageUrl, "SOL", 48.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("SOL", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextDim))
                Text(
                    if (sol != null) "$" + String.format(Locale.US, "%,.2f", sol.priceUsd) else "—",
                    style = tnum(34.sp, FontWeight.ExtraBold)
                )
            }
            if (sol != null) {
                Spacer(Modifier.width(18.dp))
                ChangePill(sol.change24h, 20.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Elegir la ciudad del clima
// ---------------------------------------------------------------------------------------------

@Composable
fun LocationScreen(vm: TvViewModel, onBack: () -> Unit) {
    val place by vm.place.collectAsStateWithLifecycle()
    val results by vm.placeResults.collectAsStateWithLifecycle()
    val message by vm.placeMessage.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val firstFocus = remember { FocusRequester() }

    DisposableEffect(Unit) {
        vm.clearPlaceSearch()
        onDispose { vm.clearPlaceSearch() }
    }
    LaunchedEffect(Unit) {
        delay(250)
        runCatching { firstFocus.requestFocus() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            TvButton(stringResource(R.string.back_button), modifier = Modifier.focusRequester(firstFocus), onClick = onBack)
            Spacer(Modifier.width(28.dp))
            Text(stringResource(R.string.place_title), style = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.ExtraBold))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.place_current, place.name),
            style = TextStyle(fontSize = 20.sp, color = TextDim),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(24.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Surface1)
                .padding(28.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.place_label)) },
                textStyle = TextStyle(fontSize = 20.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextMain,
                    unfocusedTextColor = TextMain,
                    focusedBorderColor = Cyan,
                    unfocusedBorderColor = Border,
                    focusedLabelColor = Cyan,
                    unfocusedLabelColor = TextDim,
                    cursorColor = Cyan
                )
            )
            Spacer(Modifier.height(14.dp))
            TvButton(stringResource(R.string.search_button), onClick = { vm.searchPlace(query) })
            Spacer(Modifier.height(18.dp))

            message?.let { Text(it, style = TextStyle(fontSize = 20.sp, color = Red)) }
            results.forEach { r ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .tvClickable(RoundedCornerShape(16.dp), onClick = {
                            vm.setPlace(r)
                            onBack()
                        })
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text(r.name, style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    if (r.region.isNotBlank()) {
                        Text(r.region, style = TextStyle(fontSize = 16.sp, color = TextDim))
                    }
                }
            }
        }
    }
}
