package app.pumpviewer.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.pumpviewer.MainActivity
import app.pumpviewer.R
import app.pumpviewer.data.Fmt
import app.pumpviewer.data.Repo
import app.pumpviewer.data.Token
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

// Los mismos colores que la app (Theme.kt).
private val WGreen = Color(0xFF2DF0A6)
private val WRed = Color(0xFFFF6E88)
private val WWhite = Color(0xFFF7F8FF)
private val WDim = Color(0xFFAEB6DC)
private val WLine = Color(0xFF313A68)
private val WBrand = Color(0xFF3FD8FF)

/** Debajo de esta altura el widget es una sola fila (4x1) y muestra la tira; si es más alto, la lista. */
private val STRIP_BELOW = 100.dp

/** Widget en lista (4x2 al agregarlo). Se adapta al tamaño: achicado a una fila pasa a ser la tira. */
class TokenWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Repo.init(context)
        val tokens = Repo.tokens.value
        provideContent { WidgetContent(tokens) }
    }
}

/** El mismo widget, pero entra directo en 4x1 desde el selector de widgets. */
class TokenStripWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Repo.init(context)
        val tokens = Repo.tokens.value
        provideContent { WidgetContent(tokens) }
    }
}

class TokenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TokenWidget()
}

class TokenStripWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TokenStripWidget()
}

@Composable
internal fun WidgetContent(tokens: List<Token>) {
    val size = LocalSize.current
    val strip = size.height < STRIP_BELOW
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_bg), colorFilter = null)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        when {
            tokens.isEmpty() -> EmptyWidget(strip)
            strip && tokens.size == 1 -> StripOne(tokens[0], size)
            strip -> StripMany(tokens, size)
            else -> ListWidget(tokens, size)
        }
    }
}

@Composable
private fun EmptyWidget(strip: Boolean) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = if (strip) 8.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Pump Viewer", style = TextStyle(color = ColorProvider(WBrand), fontWeight = FontWeight.Bold, fontSize = 14.sp))
        Text(
            "Tocá para agregar tu primer token",
            maxLines = 1,
            style = TextStyle(color = ColorProvider(WDim), fontSize = 13.sp)
        )
    }
}

/** 4x1 con un solo token: nombre, mini gráfico a todo lo ancho que sobra, precio y variación. */
@Composable
private fun StripOne(t: Token, size: DpSize) {
    val accent = if (t.change24h >= 0) WGreen else WRed
    val chartWidth = (size.width - 230.dp).coerceIn(40.dp, 220.dp)
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalAlignment = Alignment.CenterVertically) {
            Text(
                t.symbol.take(9),
                maxLines = 1,
                style = TextStyle(color = ColorProvider(WWhite), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            )
            Text("Pump Viewer", maxLines = 1, style = TextStyle(color = ColorProvider(WBrand), fontSize = 11.sp))
        }
        Spacer(GlanceModifier.width(10.dp))
        Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight(), contentAlignment = Alignment.Center) {
            if (size.width >= 280.dp) Spark(t.history, accent, chartWidth, min(size.height.value - 20f, 44f).dp)
        }
        Spacer(GlanceModifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End, verticalAlignment = Alignment.CenterVertically) {
            Text(
                Fmt.price(t.priceUsd),
                maxLines = 1,
                style = TextStyle(color = ColorProvider(WWhite), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            )
            Spacer(GlanceModifier.height(2.dp))
            ChangePill(t.change24h)
        }
    }
}

/** 4x1 con varios tokens: dos o tres columnas, según el ancho. */
@Composable
private fun StripMany(tokens: List<Token>, size: DpSize) {
    val columns = if (size.width >= 330.dp) 3 else 2
    val shown = tokens.take(columns)
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        shown.forEachIndexed { i, t ->
            if (i > 0) Box(GlanceModifier.width(1.dp).height(34.dp).background(WLine)) {}
            Column(
                modifier = GlanceModifier.defaultWeight().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    t.symbol.take(9),
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(WDim), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                )
                Text(
                    Fmt.price(t.priceUsd),
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(WWhite), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                )
                Text(
                    changeText(t.change24h),
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(if (t.change24h >= 0) WGreen else WRed),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

/** Widget alto: encabezado con la hora de la última actualización y una fila por token. */
@Composable
private fun ListWidget(tokens: List<Token>, size: DpSize) {
    val rows = ((size.height.value - 52f) / 34f).toInt().coerceIn(1, 8)
    val shown = tokens.take(rows)
    val withChart = size.width >= 260.dp
    Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Pump Viewer",
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = ColorProvider(WBrand), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            )
            val updated = tokens.maxOf { it.updatedAt }
            if (updated > 0L) {
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(updated)),
                    style = TextStyle(color = ColorProvider(WDim), fontSize = 12.sp)
                )
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        shown.forEach { t ->
            val accent = if (t.change24h >= 0) WGreen else WRed
            Row(
                modifier = GlanceModifier.fillMaxWidth().height(34.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(GlanceModifier.width(4.dp).height(22.dp).background(accent).cornerRadius(2.dp)) {}
                Spacer(GlanceModifier.width(9.dp))
                Text(
                    t.symbol.take(10),
                    modifier = GlanceModifier.defaultWeight(),
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(WWhite), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                )
                if (withChart) {
                    Spark(t.history, accent, 54.dp, 22.dp)
                    Spacer(GlanceModifier.width(10.dp))
                }
                Text(
                    Fmt.price(t.priceUsd),
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(WWhite),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.End
                    )
                )
                Spacer(GlanceModifier.width(8.dp))
                ChangePill(t.change24h)
            }
        }
        if (tokens.size > shown.size) {
            Text(
                "y ${tokens.size - shown.size} más en la app",
                style = TextStyle(color = ColorProvider(WDim), fontSize = 11.sp)
            )
        }
    }
}

@Composable
private fun ChangePill(pct: Double) {
    val c = if (pct >= 0) WGreen else WRed
    Box(
        modifier = GlanceModifier
            .background(c.copy(alpha = 0.20f))
            .cornerRadius(10.dp)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            changeText(pct),
            maxLines = 1,
            style = TextStyle(color = ColorProvider(c), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        )
    }
}

private fun changeText(pct: Double): String =
    (if (pct >= 0) "▲ " else "▼ ") + Fmt.pctAbs(abs(pct))

@Composable
private fun Spark(values: List<Double>, color: Color, width: Dp, height: Dp) {
    val density = LocalContext.current.resources.displayMetrics.density
    // Tope de tamaño: los widgets viajan por un canal con poco lugar para imágenes.
    val w = (width.value * density).toInt().coerceIn(24, 420)
    val h = (height.value * density).toInt().coerceIn(12, 130)
    Image(
        provider = ImageProvider(sparkBitmap(values, color.toArgb(), w, h, density)),
        contentDescription = null,
        modifier = GlanceModifier.width(width).height(height),
        contentScale = ContentScale.FillBounds
    )
}

/** Dibuja el mini gráfico como imagen: línea, relleno degradado y un punto en el último precio. */
internal fun sparkBitmap(values: List<Double>, color: Int, w: Int, h: Int, density: Float): Bitmap {
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val stroke = 1.8f * density
    val dot = 2.4f * density
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    if (values.size < 2) {
        paint.alpha = 120
        canvas.drawLine(0f, h / 2f, w.toFloat(), h / 2f, paint)
        return bmp
    }
    val lo = values.min()
    val hi = values.max()
    val range = if (hi - lo > 0.0) hi - lo else 1.0
    val right = w - dot * 1.6f
    val top = dot + stroke
    val usable = h - 2f * top
    fun x(i: Int) = right * i / (values.size - 1)
    fun y(v: Double) = top + usable * (1f - ((v - lo) / range).toFloat())

    val line = Path()
    values.forEachIndexed { i, v -> if (i == 0) line.moveTo(x(i), y(v)) else line.lineTo(x(i), y(v)) }
    val fill = Path(line).apply {
        lineTo(right, h.toFloat())
        lineTo(0f, h.toFloat())
        close()
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader = LinearGradient(0f, 0f, 0f, h.toFloat(), (color and 0x00FFFFFF) or 0x66000000, color and 0x00FFFFFF, Shader.TileMode.CLAMP)
    }
    canvas.drawPath(fill, fillPaint)
    canvas.drawPath(line, paint)
    paint.style = Paint.Style.FILL
    canvas.drawCircle(right, y(values.last()), dot, paint)
    return bmp
}
