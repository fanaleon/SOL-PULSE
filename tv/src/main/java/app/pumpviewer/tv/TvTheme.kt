package app.pumpviewer.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Misma paleta que Pump Viewer del celu, para que se sientan la misma app.
val Bg = Color(0xFF0A0C1B)
val Surface1 = Color(0xFF151930)
val Surface2 = Color(0xFF1D2342)
val Border = Color(0xFF313A68)
val TextMain = Color(0xFFF7F8FF)
val TextDim = Color(0xFFAEB6DC)
val Purple = Color(0xFFA77BFF)
val PurpleDeep = Color(0xFF7C4DFF)
val Green = Color(0xFF2DF0A6)
val Red = Color(0xFFFF6E88)
val Amber = Color(0xFFFFC857)
val Cyan = Color(0xFF3FD8FF)

private val scheme = darkColorScheme(
    primary = Purple,
    onPrimary = Color(0xFF1B0B45),
    background = Bg,
    onBackground = TextMain,
    surface = Surface1,
    onSurface = TextMain,
    outline = Border,
    error = Red
)

/** Cifras de ancho parejo: los precios no "bailan" cuando cambia un dígito. */
const val TABULAR = "tnum"

@Composable
fun TvTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography()) {
        CompositionLocalProvider(LocalContentColor provides TextMain, content = content)
    }
}

/** Fondo oscuro con dos resplandores fijos (violeta arriba, verde abajo). Sin animación: cuida la TV. */
@Composable
fun TvBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .drawBehind {
                val r = size.width * 0.8f
                fun glow(color: Color, alpha: Float, cx: Float, cy: Float) {
                    val c = Offset(cx, cy)
                    drawCircle(
                        brush = Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center = c, radius = r),
                        radius = r,
                        center = c
                    )
                }
                glow(PurpleDeep, 0.30f, size.width * 0.92f, size.height * 0.02f)
                glow(Green, 0.10f, size.width * 0.04f, size.height * 1.0f)
            },
        content = content
    )
}

/**
 * Elemento que se maneja con el control remoto: se puede enfocar con las flechas y se activa con OK.
 * Cuando tiene el foco se agranda un poco y se marca con un borde cian bien visible desde el sillón.
 */
@Composable
fun Modifier.tvClickable(
    shape: Shape,
    container: Color = Surface1,
    onClick: () -> Unit
): Modifier {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (focused) 1.05f else 1f, label = "focusScale")
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(shape)
        .background(if (focused) Surface2 else container)
        .border(if (focused) 4.dp else 1.dp, if (focused) Cyan else Border, shape)
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

/** Mini gráfico de línea con relleno degradé. */
@Composable
fun Sparkline(values: List<Double>, color: Color, modifier: Modifier = Modifier, strokeWidth: Dp = 3.dp) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (values.size < 2) {
            drawLine(color.copy(alpha = 0.35f), Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth.toPx())
            return@Canvas
        }
        val min = values.min()
        val max = values.max()
        val range = if (max - min > 0.0) max - min else 1.0
        val stepX = w / (values.size - 1)
        fun yOf(v: Double): Float {
            val frac = ((v - min) / range).toFloat()
            return h - (frac * h * 0.84f + h * 0.08f)
        }

        val line = Path()
        values.forEachIndexed { i, v ->
            val x = stepX * i
            val y = yOf(v)
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
        }
        val fill = Path()
        values.forEachIndexed { i, v ->
            val x = stepX * i
            val y = yOf(v)
            if (i == 0) fill.moveTo(x, y) else fill.lineTo(x, y)
        }
        fill.lineTo(w, h)
        fill.lineTo(0f, h)
        fill.close()

        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent)))
        drawPath(
            line,
            color,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
