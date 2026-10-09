package app.pumpviewer.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pumpviewer.data.Fmt
import coil.compose.SubcomposeAsyncImage

fun changeColor(pct: Double): Color = if (pct >= 0.0) Green else Red

/** Estilo de texto con cifras de ancho parejo, para precios y porcentajes. */
fun tnum(size: TextUnit, weight: FontWeight = FontWeight.Bold, color: Color = Color.Unspecified): TextStyle =
    TextStyle(fontSize = size, fontWeight = weight, color = color, fontFeatureSettings = TABULAR)

@Composable
fun TokenLogo(url: String?, symbol: String, size: Dp) {
    val fallback: @Composable () -> Unit = {
        Box(
            Modifier
                .size(size)
                .background(PurpleDeep, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                symbol.take(1).uppercase(),
                style = TextStyle(fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Bold, color = Color.White)
            )
        }
    }
    if (url == null) {
        fallback()
    } else {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = symbol,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            loading = { fallback() },
            error = { fallback() }
        )
    }
}

/** Píldora con la variación: verde si sube, roja si baja. */
@Composable
fun ChangePill(pct: Double, size: TextUnit = 20.sp) {
    val c = changeColor(pct)
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(c.copy(alpha = 0.16f))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            (if (pct >= 0.0) "▲ " else "▼ ") + Fmt.pctAbs(pct),
            style = tnum(size, FontWeight.Bold, c)
        )
    }
}

/** Botón para el control remoto. */
@Composable
fun TvButton(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = Surface1,
    textColor: Color = TextMain,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .tvClickable(RoundedCornerShape(16.dp), container, onClick)
            .padding(horizontal = 26.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = textColor))
    }
}
