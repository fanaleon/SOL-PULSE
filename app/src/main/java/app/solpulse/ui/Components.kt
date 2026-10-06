package app.solpulse.ui

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.solpulse.data.Fmt
import app.solpulse.data.Token
import coil.compose.SubcomposeAsyncImage
import kotlin.math.abs

/** Tarjeta "vidrio": degradé sutil + borde fino. */
fun Modifier.glass(radius: Dp = 22.dp): Modifier = this
    .clip(RoundedCornerShape(radius))
    .background(
        Brush.linearGradient(
            listOf(Surface2.copy(alpha = 0.92f), Surface1.copy(alpha = 0.92f))
        )
    )
    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(radius))

@Composable
fun chipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Purple.copy(alpha = 0.35f),
    selectedLabelColor = Color.White
)

@Composable
fun LiveDot() {
    val t = rememberInfiniteTransition(label = "live")
    val a by t.animateFloat(
        0.3f,
        1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Green.copy(alpha = a))
    )
}

/** Precio que destella en verde/rojo cuando cambia y vuelve suave al color base. */
@Composable
fun FlashingPrice(price: Double, style: TextStyle) {
    val base = TextMain
    val color = remember { Animatable(base) }
    var last by remember { mutableStateOf(price) }
    LaunchedEffect(price) {
        if (last > 0.0 && price != last) {
            color.snapTo(if (price > last) Green else Red)
            color.animateTo(base, tween(1100))
        }
        last = price
    }
    Text(Fmt.price(price), style = style, color = color.value, maxLines = 1)
}

@Composable
fun ChangeChip(pct: Double, modifier: Modifier = Modifier, fontSize: Int = 12) {
    val up = pct > 0.005
    val down = pct < -0.005
    val c = if (up) Green else if (down) Red else TextDim
    val arrow = if (up) "▲" else if (down) "▼" else "•"
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            "$arrow ${Fmt.pctAbs(abs(pct))}",
            color = c,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.SemiBold,
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
                .background(Brush.linearGradient(listOf(Purple, Green))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                token.symbol.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
        }
    }
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Surface2)
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

@Composable
fun Sparkline(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) {
            drawLine(
                color = color.copy(alpha = 0.35f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
            return@Canvas
        }
        val min = values.min()
        val max = values.max()
        val range = if (max - min > 0.0) max - min else 1.0
        val stepX = size.width / (values.size - 1)
        fun yOf(v: Double): Float =
            size.height - ((v - min) / range).toFloat() * size.height * 0.8f - size.height * 0.1f

        val line = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            if (i == 0) line.moveTo(x, yOf(v)) else line.lineTo(x, yOf(v))
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            fill,
            Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent))
        )
        drawPath(
            line,
            color,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun gradientTitle(size: Int): TextStyle = TextStyle(
    brush = Brush.linearGradient(listOf(Purple, Green)),
    fontSize = size.sp,
    fontWeight = FontWeight.ExtraBold
)
