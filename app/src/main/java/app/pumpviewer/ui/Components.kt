package app.pumpviewer.ui

import androidx.compose.animation.Animatable as ColorAnimatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pumpviewer.data.Fmt
import app.pumpviewer.data.Token
import coil.compose.SubcomposeAsyncImage
import kotlin.math.abs

/** Cifras de ancho parejo: los precios no "bailan" cuando cambia un dígito. */
const val TABULAR = "tnum"

/** Tarjeta: panel en relieve con borde visible. */
fun Modifier.glass(radius: Dp = 22.dp): Modifier = this
    .clip(RoundedCornerShape(radius))
    .background(Brush.verticalGradient(listOf(Surface2, Surface1)))
    .border(1.dp, Border, RoundedCornerShape(radius))

/** Tarjeta teñida con un color (verde si el token sube, rojo si baja): el tinte entra desde la derecha. */
fun Modifier.tintedGlass(tint: Color, radius: Dp = 24.dp): Modifier = this
    .clip(RoundedCornerShape(radius))
    .background(Brush.verticalGradient(listOf(Surface2, Surface1)))
    .background(Brush.horizontalGradient(listOf(Color.Transparent, tint.copy(alpha = 0.05f), tint.copy(alpha = 0.20f))))
    .border(
        BorderStroke(1.dp, Brush.horizontalGradient(listOf(Border, tint.copy(alpha = 0.55f)))),
        RoundedCornerShape(radius)
    )

@Composable
fun chipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    containerColor = Color.Transparent,
    labelColor = TextDim,
    selectedContainerColor = PurpleDeep,
    selectedLabelColor = Color.White
)

@Composable
fun primaryButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = PurpleDeep,
    contentColor = Color.White,
    disabledContainerColor = Surface2,
    disabledContentColor = TextFaint
)

@Composable
fun quietButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = Surface2,
    contentColor = TextMain
)

@Composable
fun fieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextMain,
    unfocusedTextColor = TextMain,
    focusedBorderColor = Purple,
    unfocusedBorderColor = Border,
    focusedLabelColor = Purple,
    unfocusedLabelColor = TextDim,
    cursorColor = Purple
)

@Composable
fun switchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = PurpleDeep,
    uncheckedThumbColor = TextDim,
    uncheckedTrackColor = Surface1,
    uncheckedBorderColor = Border
)

/** Punto verde con un anillo que se expande: la app está actualizando precios. */
@Composable
fun LiveDot() {
    val wave = if (Motion.enabled) {
        val t = rememberInfiniteTransition(label = "live")
        val v by t.animateFloat(
            0f,
            1f,
            infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
            label = "wave"
        )
        v
    } else {
        0.35f
    }
    Canvas(Modifier.size(14.dp)) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val dot = 3.6.dp.toPx()
        drawCircle(Green.copy(alpha = 0.55f * (1f - wave)), radius = dot + (size.width / 2f - dot) * wave, center = c)
        drawCircle(Green, radius = dot, center = c)
    }
}

/**
 * Precio que reacciona cuando cambia: los dígitos entran desde abajo si subió (o desde arriba
 * si bajó) y el número destella en verde o rojo antes de volver a blanco.
 */
@Composable
fun FlashingPrice(price: Double, style: TextStyle) {
    val color = remember { ColorAnimatable(TextMain) }
    var last by remember { mutableStateOf(price) }
    LaunchedEffect(price) {
        if (last > 0.0 && price != last) {
            color.snapTo(if (price > last) Green else Red)
            color.animateTo(TextMain, tween(1500))
        }
        last = price
    }
    AnimatedContent(
        targetState = price,
        transitionSpec = {
            val dir = if (targetState >= initialState) 1 else -1
            (slideInVertically(tween(280)) { it * dir / 2 } + fadeIn(tween(280))) togetherWith
                (slideOutVertically(tween(280)) { -it * dir / 2 } + fadeOut(tween(160)))
        },
        label = "price"
    ) { shown ->
        Text(
            Fmt.price(shown),
            style = style.copy(fontFeatureSettings = TABULAR),
            color = color.value,
            maxLines = 1
        )
    }
}

/** Pastilla de variación: flecha y porcentaje, verde o roja. */
@Composable
fun ChangeChip(pct: Double, modifier: Modifier = Modifier, fontSize: Int = 12) {
    val up = pct > 0.005
    val down = pct < -0.005
    val c = if (up) Green else if (down) Red else TextDim
    val arrow = if (up) "▲" else if (down) "▼" else "•"
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.16f))
            .border(1.dp, c.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = (fontSize * 0.75f).dp, vertical = (fontSize * 0.25f).dp)
    ) {
        Text(
            "$arrow ${Fmt.pctAbs(abs(pct))}",
            color = c,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(fontFeatureSettings = TABULAR),
            maxLines = 1
        )
    }
}

@Composable
fun TokenLogo(token: Token, size: Dp) {
    val letter: @Composable () -> Unit = {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(PurpleDeep, Cyan))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                token.symbol.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Surface2)
            .border(1.5.dp, Color.White.copy(alpha = 0.22f), CircleShape)
    ) {
        if (token.imageUrl.isNullOrBlank()) {
            letter()
        } else {
            SubcomposeAsyncImage(
                model = token.imageUrl,
                contentDescription = token.symbol,
                modifier = Modifier.fillMaxSize(),
                loading = { letter() },
                error = { letter() }
            )
        }
    }
}

/**
 * Mini gráfico del historial. Se dibuja de izquierda a derecha la primera vez que aparece y
 * termina en un punto que late, marcando el último precio.
 */
@Composable
fun Sparkline(values: List<Double>, color: Color, modifier: Modifier = Modifier, endDot: Boolean = true) {
    val reveal = remember { Animatable(if (Motion.enabled) 0f else 1f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val pulse = if (Motion.enabled && endDot) {
        val t = rememberInfiniteTransition(label = "spark")
        val v by t.animateFloat(
            0f,
            1f,
            infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
            label = "pulse"
        )
        v
    } else {
        0.3f
    }
    Canvas(modifier) {
        if (values.size < 2) {
            drawLine(
                color = color.copy(alpha = 0.45f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
            return@Canvas
        }
        val dot = 3.2.dp.toPx()
        val right = size.width - if (endDot) dot * 2.4f else 0f
        val min = values.min()
        val max = values.max()
        val range = if (max - min > 0.0) max - min else 1.0
        val stepX = right / (values.size - 1)
        fun yOf(v: Double): Float =
            size.height - ((v - min) / range).toFloat() * size.height * 0.76f - size.height * 0.12f

        val line = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            if (i == 0) line.moveTo(x, yOf(v)) else line.lineTo(x, yOf(v))
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(right, size.height)
            lineTo(0f, size.height)
            close()
        }
        clipRect(right = size.width * reveal.value) {
            drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.38f), Color.Transparent)))
            // Un trazo ancho y tenue debajo da el efecto de resplandor.
            drawPath(line, color.copy(alpha = 0.22f), style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(line, color, style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (endDot && reveal.value > 0.97f) {
            val end = Offset(right, yOf(values.last()))
            drawCircle(color.copy(alpha = 0.5f * (1f - pulse)), radius = dot + dot * 1.6f * pulse, center = end)
            drawCircle(Bg, radius = dot + 1.dp.toPx(), center = end)
            drawCircle(color, radius = dot, center = end)
        }
    }
}

/** Degradé de marca para el título: verde, cian y violeta. */
@Composable
fun gradientTitle(size: Int): TextStyle = TextStyle(
    brush = Brush.linearGradient(listOf(Green, Cyan, Purple)),
    fontSize = size.sp,
    fontWeight = FontWeight.Black,
    letterSpacing = (-0.5).sp
)
