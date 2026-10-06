@file:OptIn(ExperimentalMaterial3Api::class)

package app.pumpviewer.ui

import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pumpviewer.data.AlertType
import app.pumpviewer.data.Fmt
import app.pumpviewer.data.PriceAlert
import app.pumpviewer.data.Token
import kotlin.math.roundToInt

@Composable
fun DetailScreen(vm: MainViewModel, mint: String, onBack: () -> Unit) {
    val tokens by vm.tokens.collectAsStateWithLifecycle()
    val allAlerts by vm.alerts.collectAsStateWithLifecycle()
    val token = tokens.firstOrNull { it.mint == mint }

    // Si el token se borró, volvemos a la lista.
    LaunchedEffect(token == null) {
        if (token == null) onBack()
    }
    if (token == null) return

    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showNewAlert by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val alerts = allAlerts.filter { it.mint == mint }.sortedByDescending { it.target }
    val accent = if (token.change24h >= 0) Green else Red

    Column(
        Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TopIcon(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextMain)
            }
            Spacer(Modifier.weight(1f))
            TopIcon(onClick = { confirmDelete = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Dejar de seguir", tint = TextDim)
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TokenLogo(token, 64.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    token.symbol,
                    color = TextMain,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(token.name, color = TextDim, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(Modifier.height(18.dp))
        FlashingPrice(token.priceUsd, TextStyle(fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChangeTile("5 min", token.change5m, Modifier.weight(1f))
            ChangeTile("1 hora", token.change1h, Modifier.weight(1f))
            ChangeTile("24 horas", token.change24h, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        PriceChart(
            values = token.history,
            color = accent,
            current = token.priceUsd,
            targets = alerts.filter { it.enabled }.map { it.target }
        )

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Liquidez", Fmt.compact(token.liquidityUsd), Cyan, Modifier.weight(1f))
            StatCard("Volumen 24 h", Fmt.compact(token.volume24h), Purple, Modifier.weight(1f))
            StatCard("Market cap", Fmt.compact(token.marketCap), Amber, Modifier.weight(1f))
        }

        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = Amber, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Alertas", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Button(onClick = { showNewAlert = true }, colors = primaryButtonColors()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nueva alerta", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))

        if (alerts.isEmpty()) {
            Text(
                "Todavía no hay alertas. Creá una y te avisamos al celu y al reloj cuando el precio llegue.",
                color = TextDim,
                fontSize = 15.sp,
                lineHeight = 21.sp
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                alerts.forEach { a ->
                    AlertRow(
                        alert = a,
                        token = token,
                        onToggle = { vm.setAlertEnabled(a.id, it) },
                        onDelete = { vm.removeAlert(a.id) }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(token.mint))
                    Toast.makeText(ctx, "Dirección copiada", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMain),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.weight(1f)
            ) { Text("Copiar contrato", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            OutlinedButton(
                onClick = {
                    val url = token.pairUrl ?: "https://dexscreener.com/solana/${token.mint}"
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMain),
                border = BorderStroke(1.dp, Border),
                modifier = Modifier.weight(1f)
            ) { Text("DexScreener", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }

    if (showNewAlert) {
        NewAlertDialog(
            token = token,
            onDismiss = { showNewAlert = false },
            onCreate = { type, target ->
                vm.addAlert(token.mint, type, target)
                showNewAlert = false
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Surface2,
            titleContentColor = TextMain,
            textContentColor = TextDim,
            title = { Text("¿Dejar de seguir ${token.symbol}?", fontWeight = FontWeight.Bold) },
            text = { Text("Se borran también sus alertas.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.removeToken(token.mint)
                }) { Text("Dejar de seguir", color = Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar", color = TextMain) }
            }
        )
    }
}

@Composable
private fun TopIcon(onClick: () -> Unit, content: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Surface2.copy(alpha = 0.85f))
            .border(1.dp, Border, CircleShape),
        content = content
    )
}

/** Variación de un período: etiqueta arriba y porcentaje grande, sobre un fondo del color que corresponde. */
@Composable
private fun ChangeTile(label: String, pct: Double, modifier: Modifier = Modifier) {
    val up = pct > 0.005
    val down = pct < -0.005
    val c = if (up) Green else if (down) Red else TextDim
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.copy(alpha = 0.13f))
            .border(1.dp, c.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(label, color = TextDim, fontSize = 12.sp, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(
            (if (up) "▲ " else if (down) "▼ " else "") + Fmt.pctAbs(pct),
            color = c,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            style = TextStyle(fontFeatureSettings = TABULAR),
            maxLines = 1
        )
    }
}

/**
 * Gráfico del historial. Marca el máximo y el mínimo, dibuja con línea punteada las alertas que
 * caen cerca, y al mantenerlo apretado deja recorrer los valores con el dedo.
 */
@Composable
private fun PriceChart(values: List<Double>, color: Color, current: Double, targets: List<Double>) {
    var scrub by remember { mutableStateOf<Float?>(null) }
    val reveal = remember { Animatable(if (Motion.enabled) 0f else 1f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    val pulse = if (Motion.enabled) {
        val t = rememberInfiniteTransition(label = "chart")
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

    val min = values.minOrNull() ?: current
    val max = values.maxOrNull() ?: current
    val span = if (max - min > 0.0) max - min else if (max > 0.0) max * 0.02 else 1.0
    // Solo se dibujan las alertas que caen cerca del rango visto; las lejanas aplastarían el gráfico.
    val near = targets.filter { it >= min - span * 0.6 && it <= max + span * 0.6 }
    val lo = minOf(min, near.minOrNull() ?: min)
    val hi = maxOf(max, near.maxOrNull() ?: max)
    val range = if (hi - lo > 0.0) hi - lo else span

    val picked: Int? = scrub?.let { f -> if (values.size < 2) null else (f * (values.size - 1)).roundToInt().coerceIn(0, values.size - 1) }

    Column(Modifier.fillMaxWidth().glass(24.dp).padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (picked != null) {
                val v = values[picked]
                val diff = if (current > 0.0) (v - current) / current * 100.0 else 0.0
                Text(
                    Fmt.price(v),
                    color = TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(fontFeatureSettings = TABULAR)
                )
                Spacer(Modifier.width(8.dp))
                Text("${Fmt.pct(diff)} contra ahora", color = TextDim, fontSize = 13.sp, maxLines = 1)
            } else {
                Text("Máx. ", color = TextDim, fontSize = 13.sp)
                Text(Fmt.price(max), color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold, style = TextStyle(fontFeatureSettings = TABULAR))
                Spacer(Modifier.weight(1f))
                Text("Mín. ", color = TextDim, fontSize = 13.sp)
                Text(Fmt.price(min), color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold, style = TextStyle(fontFeatureSettings = TABULAR))
            }
        }
        Spacer(Modifier.height(8.dp))

        val amber = Amber
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .pointerInput(values.size) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { scrub = (it.x / size.width).coerceIn(0f, 1f) },
                        onDrag = { change, _ -> scrub = (change.position.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = { scrub = null },
                        onDragCancel = { scrub = null }
                    )
                }
        ) {
            val dot = 4.dp.toPx()
            val right = size.width - dot * 2.4f
            fun yOf(v: Double): Float =
                size.height - ((v - lo) / range).toFloat() * size.height * 0.80f - size.height * 0.10f

            // Renglones de referencia.
            for (k in 0..3) {
                val y = size.height * (0.10f + 0.80f * k / 3f)
                drawLine(Border.copy(alpha = 0.55f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }

            if (values.size < 2) {
                drawLine(
                    color = color.copy(alpha = 0.5f),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
                return@Canvas
            }

            // Alertas cercanas: línea punteada ámbar con su precio.
            val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = amber.toArgb()
                textSize = 11.sp.toPx()
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            near.forEach { target ->
                val y = yOf(target)
                drawLine(
                    amber.copy(alpha = 0.9f),
                    Offset(0f, y),
                    Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
                )
                drawIntoCanvas { it.nativeCanvas.drawText("Alerta " + Fmt.price(target), 4.dp.toPx(), y - 4.dp.toPx(), label) }
            }

            val stepX = right / (values.size - 1)
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
                drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.42f), Color.Transparent)))
                drawPath(line, color.copy(alpha = 0.22f), style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(line, color, style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            if (reveal.value > 0.97f) {
                val end = Offset(right, yOf(values.last()))
                drawCircle(color.copy(alpha = 0.5f * (1f - pulse)), radius = dot + dot * 1.8f * pulse, center = end)
                drawCircle(Bg, radius = dot + 1.5.dp.toPx(), center = end)
                drawCircle(color, radius = dot, center = end)
            }

            if (picked != null) {
                val x = picked * stepX
                val y = yOf(values[picked])
                drawLine(TextMain.copy(alpha = 0.7f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                drawCircle(Bg, radius = dot + 2.dp.toPx(), center = Offset(x, y))
                drawCircle(TextMain, radius = dot, center = Offset(x, y))
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            if (values.size < 2) {
                "El gráfico se va armando con cada actualización de precio."
            } else {
                "${values.size} precios guardados desde que seguís el token. Mantené apretado para recorrerlos."
            },
            color = TextDim,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier.glass(18.dp).padding(horizontal = 12.dp, vertical = 12.dp)) {
        Box(Modifier.size(width = 22.dp, height = 4.dp).clip(RoundedCornerShape(50)).background(accent))
        Spacer(Modifier.height(8.dp))
        Text(label, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            color = TextMain,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            style = TextStyle(fontFeatureSettings = TABULAR),
            maxLines = 1
        )
    }
}

@Composable
private fun AlertRow(
    alert: PriceAlert,
    token: Token,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val up = alert.type == AlertType.ABOVE
    val c = if (!alert.enabled) TextFaint else if (up) Green else Red
    val diff = if (token.priceUsd > 0) (alert.target - token.priceUsd) / token.priceUsd * 100.0 else 0.0
    // Qué tan cerca está el precio del objetivo: 1 = llegó.
    val closeness = when {
        token.priceUsd <= 0.0 || alert.target <= 0.0 -> 0f
        up -> (token.priceUsd / alert.target).toFloat()
        else -> (alert.target / token.priceUsd).toFloat()
    }.coerceIn(0f, 1f)
    val shown by animateFloatAsState(closeness, tween(700), label = "closeness")

    Column(
        Modifier
            .fillMaxWidth()
            .glass(20.dp)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(c.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (up) "▲" else "▼", color = c, fontSize = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    (if (up) "Sube a " else "Baja a ") + Fmt.price(alert.target),
                    color = if (alert.enabled) TextMain else TextDim,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(fontFeatureSettings = TABULAR),
                    maxLines = 1
                )
                Text(
                    when {
                        !alert.enabled -> "En pausa"
                        up == (diff >= 0) -> (if (up) "Le falta subir " else "Le falta bajar ") + Fmt.pctAbs(diff)
                        else -> "Ya se cumple"
                    },
                    color = TextDim,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(checked = alert.enabled, onCheckedChange = onToggle, colors = switchColors())
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Borrar alerta", tint = TextDim)
            }
        }
        Spacer(Modifier.height(8.dp))
        // Barra de cercanía al objetivo.
        Box(
            Modifier
                .padding(end = 8.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Bg)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(shown)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(c.copy(alpha = 0.45f), c)))
            )
        }
    }
}

@Composable
private fun QuickChip(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Surface1)
            .border(1.dp, Border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(text, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun NewAlertDialog(
    token: Token,
    onDismiss: () -> Unit,
    onCreate: (AlertType, Double) -> Unit
) {
    var type by remember { mutableStateOf(AlertType.ABOVE) }
    var text by remember { mutableStateOf("") }
    val target = text.toDoubleOrNull()
    val valid = target != null && target > 0.0
    val firesNow = valid && token.priceUsd > 0.0 && (
        (type == AlertType.ABOVE && target!! <= token.priceUsd) ||
            (type == AlertType.BELOW && target!! >= token.priceUsd)
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface2,
        titleContentColor = TextMain,
        textContentColor = TextMain,
        title = { Text("Nueva alerta para ${token.symbol}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Precio de ahora: ${Fmt.price(token.priceUsd)}", color = TextDim, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == AlertType.ABOVE,
                        onClick = { type = AlertType.ABOVE },
                        label = { Text("▲ Sube a", fontWeight = FontWeight.SemiBold) },
                        colors = chipColors()
                    )
                    FilterChip(
                        selected = type == AlertType.BELOW,
                        onClick = { type = AlertType.BELOW },
                        label = { Text("▼ Baja a", fontWeight = FontWeight.SemiBold) },
                        colors = chipColors()
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.replace(',', '.') },
                    label = { Text("Precio objetivo en dólares") },
                    singleLine = true,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(-10, -5, 5, 10).forEach { p ->
                        val label = (if (p > 0) "+" else "") + "$p%"
                        QuickChip(label) {
                            val v = token.priceUsd * (1 + p / 100.0)
                            text = Fmt.plain(v, 6)
                            type = if (p > 0) AlertType.ABOVE else AlertType.BELOW
                        }
                    }
                }
                if (firesNow) {
                    Text(
                        "Ese precio ya se cumple: la alerta saltaría enseguida.",
                        color = Amber,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = { onCreate(type, target!!) },
                colors = primaryButtonColors()
            ) { Text("Crear alerta", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMain) } }
    )
}
