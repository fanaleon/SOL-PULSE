package app.solpulse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF07070D)
val Surface1 = Color(0xFF12121C)
val Surface2 = Color(0xFF1B1B29)
val Purple = Color(0xFF9945FF)
val Green = Color(0xFF14F195)
val Red = Color(0xFFFF4D6D)
val TextMain = Color(0xFFF2F2FA)
val TextDim = Color(0xFF8B8BA3)

private val scheme = darkColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    secondary = Green,
    onSecondary = Color.Black,
    background = Bg,
    onBackground = TextMain,
    surface = Surface1,
    onSurface = TextMain,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextDim,
    error = Red
)

@Composable
fun SolPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}

/** Fondo oscuro con dos resplandores (violeta arriba a la derecha, verde abajo a la izquierda). */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .drawBehind {
                val r = size.width * 0.95f
                val c1 = Offset(size.width * 0.95f, 0f)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Purple.copy(alpha = 0.30f), Color.Transparent),
                        center = c1,
                        radius = r
                    ),
                    radius = r,
                    center = c1
                )
                val c2 = Offset(0f, size.height)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Green.copy(alpha = 0.14f), Color.Transparent),
                        center = c2,
                        radius = r
                    ),
                    radius = r,
                    center = c2
                )
            },
        content = content
    )
}
