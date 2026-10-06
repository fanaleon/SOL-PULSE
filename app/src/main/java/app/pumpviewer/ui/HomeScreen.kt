@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package app.pumpviewer.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pumpviewer.data.Fmt
import app.pumpviewer.data.Repo
import app.pumpviewer.data.Token
import app.pumpviewer.service.Notifier
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(vm: MainViewModel, onOpen: (String) -> Unit) {
    val tokens by vm.tokens.collectAsStateWithLifecycle()
    val alerts by vm.alerts.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Header(
                updatedAt = tokens.maxOfOrNull { it.updatedAt } ?: 0L,
                refreshing = refreshing,
                onRefresh = { vm.refresh() },
                onSettings = { showSettings = true }
            )

            if (tokens.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 6.dp, 16.dp, 120.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "summary") {
                        Summary(tokens = tokens, activeAlerts = alerts.count { it.enabled })
                    }
                    // Con pocos tokens sobra pantalla: el gráfico de cada tarjeta crece para aprovecharla.
                    val chartHeight = when (tokens.size) {
                        1 -> 132.dp
                        2 -> 88.dp
                        else -> 58.dp
                    }
                    items(tokens, key = { it.mint }) { token ->
                        TokenCard(
                            token = token,
                            alertCount = alerts.count { it.mint == token.mint && it.enabled },
                            chartHeight = chartHeight,
                            onClick = { onOpen(token.mint) },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                vm.resetAdd()
                showAdd = true
            },
            containerColor = PurpleDeep,
            contentColor = Color.White,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Agregar token", fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp)
        )
    }

    if (showAdd) {
        ModalBottomSheet(
            onDismissRequest = { showAdd = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Surface1,
            contentColor = TextMain
        ) {
            AddTokenSheet(vm = vm, onDone = { showAdd = false })
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor = Surface1,
            contentColor = TextMain
        ) {
            SettingsSheet()
        }
    }
}

@Composable
private fun Header(updatedAt: Long, refreshing: Boolean, onRefresh: () -> Unit, onSettings: () -> Unit) {
    val angle = if (Motion.enabled) {
        val spin = rememberInfiniteTransition(label = "spin")
        val v by spin.animateFloat(
            0f,
            360f,
            infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
            label = "angle"
        )
        v
    } else {
        0f
    }
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Pump Viewer", style = gradientTitle(32), maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot()
                Spacer(Modifier.width(6.dp))
                Text(liveLabel(updatedAt), color = TextDim, fontSize = 13.sp, maxLines = 1)
            }
        }
        RoundIcon(Icons.Default.Refresh, "Actualizar", onRefresh, Modifier.rotate(if (refreshing) angle else 0f))
        Spacer(Modifier.width(10.dp))
        RoundIcon(Icons.Default.Settings, "Ajustes", onSettings)
    }
}

/** "En vivo, hace 8 s": cuánto pasó desde la última actualización de precios. */
@Composable
private fun liveLabel(updatedAt: Long): String {
    if (updatedAt <= 0L) return "En vivo"
    if (!Motion.enabled) return "En vivo, hace 4 s"
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(1000)
            value = System.currentTimeMillis()
        }
    }
    val sec = ((now - updatedAt) / 1000).coerceAtLeast(0)
    return when {
        sec < 3 -> "En vivo, recién actualizado"
        sec < 90 -> "En vivo, hace $sec s"
        sec < 5400 -> "Última actualización hace ${sec / 60} min"
        else -> "Última actualización hace ${sec / 3600} h"
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, description: String, onClick: () -> Unit, iconModifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Surface2.copy(alpha = 0.85f))
            .border(1.dp, Border, CircleShape)
    ) {
        Icon(icon, contentDescription = description, tint = TextMain, modifier = iconModifier)
    }
}

/** Resumen arriba de la lista: cuántos tokens y alertas hay, y cuántos suben o bajan hoy. */
@Composable
private fun Summary(tokens: List<Token>, activeAlerts: Int) {
    val up = tokens.count { it.change24h > 0.005 }
    val down = tokens.count { it.change24h < -0.005 }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill(if (tokens.size == 1) "1 token" else "${tokens.size} tokens", Cyan)
            Pill(
                when (activeAlerts) {
                    0 -> "Sin alertas activas"
                    1 -> "1 alerta activa"
                    else -> "$activeAlerts alertas activas"
                },
                Amber,
                Icons.Default.Notifications
            )
        }
        if (tokens.size > 1) {
            // Barra de ánimo: la parte verde son los tokens que suben en 24 h; la roja, los que bajan.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Surface2)
                ) {
                    if (up > 0) Box(Modifier.weight(up.toFloat()).fillMaxHeight().background(Green))
                    if (tokens.size - up - down > 0) Box(Modifier.weight((tokens.size - up - down).toFloat()).fillMaxHeight().background(TextFaint))
                    if (down > 0) Box(Modifier.weight(down.toFloat()).fillMaxHeight().background(Red))
                }
                Spacer(Modifier.width(10.dp))
                Text("$up suben", color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("  $down bajan", color = Red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun Pill(text: String, color: Color, icon: ImageVector? = null) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun EmptyState() {
    val scale = if (Motion.enabled) {
        val t = rememberInfiniteTransition(label = "empty")
        val v by t.animateFloat(
            0.92f,
            1.08f,
            infiniteRepeatable(tween(1600), RepeatMode.Reverse),
            label = "scale"
        )
        v
    } else {
        1f
    }
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(120.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(PurpleDeep.copy(alpha = 0.7f), Cyan.copy(alpha = 0.15f), Color.Transparent))),
            contentAlignment = Alignment.Center
        ) {
            Sparkline(
                listOf(1.0, 1.1, 1.05, 1.3, 1.2, 1.6, 1.5, 2.0),
                Green,
                Modifier.size(width = 64.dp, height = 40.dp)
            )
        }
        Spacer(Modifier.size(22.dp))
        Text("Todavía no seguís ningún token", color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(8.dp))
        Text(
            "Tocá “Agregar token” y pegá el contrato. Vas a ver el precio en vivo y podés ponerle alertas.",
            color = TextDim,
            fontSize = 15.sp,
            lineHeight = 21.sp
        )
    }
}

@Composable
private fun TokenCard(
    token: Token,
    alertCount: Int,
    chartHeight: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (token.change24h >= 0) Green else Red
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")

    Column(
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .tintedGlass(accent)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TokenLogo(token, 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    token.symbol,
                    color = TextMain,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        token.name,
                        color = TextDim,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (alertCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Alertas activas",
                            tint = Amber,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(" $alertCount", color = Amber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                FlashingPrice(token.priceUsd, TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black))
                Spacer(Modifier.size(5.dp))
                ChangeChip(token.change24h, fontSize = 13)
            }
        }

        Spacer(Modifier.height(12.dp))
        Sparkline(token.history, accent, Modifier.fillMaxWidth().height(chartHeight))
        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniChange("5 min", token.change5m)
            Spacer(Modifier.width(14.dp))
            MiniChange("1 h", token.change1h)
            Spacer(Modifier.weight(1f))
            Text("Vol. 24 h ", color = TextDim, fontSize = 12.sp)
            Text(
                Fmt.compact(token.volume24h),
                color = TextMain,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(fontFeatureSettings = TABULAR)
            )
        }
    }
}

/** Variación corta dentro de la tarjeta: etiqueta gris y porcentaje en color. */
@Composable
private fun MiniChange(label: String, pct: Double) {
    val c = if (pct > 0.005) Green else if (pct < -0.005) Red else TextDim
    Text("$label ", color = TextDim, fontSize = 12.sp)
    Text(
        Fmt.pct(pct),
        color = c,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        style = TextStyle(fontFeatureSettings = TABULAR)
    )
}

@Composable
internal fun AddTokenSheet(vm: MainViewModel, onDone: () -> Unit) {
    var mint by remember { mutableStateOf("") }
    val state by vm.addState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Agregar token", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text("Pegá la dirección del contrato (mint) del token en Solana.", color = TextDim, fontSize = 14.sp)

        OutlinedTextField(
            value = mint,
            onValueChange = {
                mint = it.trim()
                vm.resetAdd()
            },
            label = { Text("Dirección del contrato") },
            singleLine = true,
            colors = fieldColors(),
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(onClick = {
                    val pasted = clipboard.getText()?.text?.trim().orEmpty()
                    if (pasted.isNotEmpty()) {
                        mint = pasted
                        vm.lookup(pasted)
                    }
                }) { Text("Pegar", color = Purple, fontWeight = FontWeight.Bold) }
            }
        )

        when (val s = state) {
            AddState.Idle -> {}
            AddState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = Purple, trackColor = Surface2)
            is AddState.Error -> Text(s.message, color = Red, fontSize = 14.sp)
            is AddState.Found -> TokenPreview(s.token)
        }

        val found = state as? AddState.Found
        if (found != null) {
            Button(
                onClick = {
                    vm.confirmAdd()
                    onDone()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = primaryButtonColors()
            ) { Text("Seguir ${found.token.symbol}", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        } else {
            Button(
                onClick = { vm.lookup(mint) },
                enabled = mint.isNotBlank() && state !is AddState.Loading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = primaryButtonColors()
            ) { Text("Buscar", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        }
    }
}

@Composable
private fun TokenPreview(token: Token) {
    Row(
        Modifier.fillMaxWidth().glass().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TokenLogo(token, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(token.symbol, color = TextMain, fontWeight = FontWeight.Black, fontSize = 17.sp, maxLines = 1)
            Text(token.name, color = TextDim, fontSize = 13.sp, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Fmt.price(token.priceUsd),
                color = TextMain,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                style = TextStyle(fontFeatureSettings = TABULAR)
            )
            Spacer(Modifier.height(4.dp))
            ChangeChip(token.change24h)
        }
    }
}

@Composable
internal fun SettingsSheet() {
    val ctx = LocalContext.current
    val enabled by Repo.serviceEnabled.collectAsStateWithLifecycle()
    val interval by Repo.intervalSec.collectAsStateWithLifecycle()

    Column(
        Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Ajustes", color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Black)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Vigilar en segundo plano", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Revisa precios y manda alertas aunque cierres la app", color = TextDim, fontSize = 13.sp)
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = enabled, onCheckedChange = { Repo.setServiceEnabled(it) }, colors = switchColors())
        }

        Text("Cada cuánto revisa los precios", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15 to "15 s", 30 to "30 s", 60 to "1 min", 300 to "5 min").forEach { (sec, label) ->
                FilterChip(
                    selected = interval == sec,
                    onClick = { Repo.setIntervalSec(sec) },
                    label = { Text(label, fontWeight = FontWeight.SemiBold) },
                    colors = chipColors()
                )
            }
        }

        Button(
            onClick = { Notifier.test(ctx) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = quietButtonColors(),
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) { Text("Probar una alerta en el reloj", fontWeight = FontWeight.SemiBold) }

        Text(
            "Para que el reloj reciba las alertas: en la app de tu reloj (Mi Fitness / Zepp) " +
                "activá las notificaciones para Pump Viewer. En Xiaomi/HyperOS: Ajustes › Apps › Pump Viewer › " +
                "Ahorro de batería: “Sin restricciones” e “Inicio automático” activado.",
            color = TextDim,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
