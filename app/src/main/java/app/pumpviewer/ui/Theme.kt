package app.pumpviewer.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Paleta. Todos los textos tienen contraste AA o mejor sobre los tres fondos.
val Bg = Color(0xFF0A0C1B)          // fondo: azul tinta, casi negro
val Surface1 = Color(0xFF151930)    // tarjetas
val Surface2 = Color(0xFF1D2342)    // tarjetas en relieve, diálogos
val Border = Color(0xFF313A68)      // borde de tarjetas
val TextMain = Color(0xFFF7F8FF)    // texto principal
val TextDim = Color(0xFFAEB6DC)     // texto secundario
val TextFaint = Color(0xFF8C95C0)   // notas chicas
val Purple = Color(0xFFA77BFF)      // violeta de marca (textos e íconos)
val PurpleDeep = Color(0xFF7C4DFF)  // violeta para rellenos con texto blanco
val Green = Color(0xFF2DF0A6)       // sube
val Red = Color(0xFFFF6E88)         // baja
val Amber = Color(0xFFFFC857)       // alertas
val Cyan = Color(0xFF3FD8FF)        // acento frío del degradé de marca

private val scheme = darkColorScheme(
    primary = Purple,
    onPrimary = Color(0xFF1B0B45),
    primaryContainer = PurpleDeep,
    onPrimaryContainer = Color.White,
    secondary = Green,
    onSecondary = Color(0xFF06281B),
    secondaryContainer = Color(0xFF3A2C7A),
    onSecondaryContainer = TextMain,
    tertiary = Amber,
    onTertiary = Color(0xFF2B1D00),
    background = Bg,
    onBackground = TextMain,
    surface = Surface1,
    onSurface = TextMain,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextDim,
    outline = Border,
    outlineVariant = Border,
    error = Red,
    onError = Color(0xFF3A0A14)
)

/** Interruptor general de animaciones. Las pruebas de captura lo apagan para ver el estado final. */
object Motion {
    var enabled = true
}

@Composable
fun PumpViewerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = Typography()) {
        // Sin esto, los textos que no fijan color salen negros: las pantallas no usan Surface.
        CompositionLocalProvider(LocalContentColor provides TextMain, content = content)
    }
}

/** Fondo oscuro con una "aurora" que se mueve muy despacio: violeta arriba, verde abajo y un toque cian. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    val drift = if (Motion.enabled) {
        val t = rememberInfiniteTransition(label = "aurora")
        val phase by t.animateFloat(
            0f,
            1f,
            infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Restart),
            label = "phase"
        )
        phase
    } else {
        0.15f
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .drawBehind {
                val a = drift * 2f * PI.toFloat()
                val r = size.width * 0.95f
                fun glow(color: Color, alpha: Float, cx: Float, cy: Float, radius: Float) {
                    val c = Offset(cx, cy)
                    drawCircle(
                        brush = Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center = c, radius = radius),
                        radius = radius,
                        center = c
                    )
                }
                glow(PurpleDeep, 0.34f, size.width * (0.88f + 0.10f * cos(a)), size.height * (0.04f + 0.05f * sin(a)), r)
                glow(Green, 0.13f, size.width * (0.05f + 0.10f * sin(a)), size.height * (0.98f - 0.05f * cos(a)), r)
                glow(Cyan, 0.08f, size.width * (0.20f + 0.25f * cos(a * 2f)), size.height * 0.42f, r * 0.7f)
            },
        content = content
    )
}
