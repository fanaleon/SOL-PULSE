@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package app.solpulse.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.solpulse.data.Fmt
import app.solpulse.data.Repo
import app.solpulse.data.Token
import app.solpulse.service.Notifier

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
                refreshing = refreshing,
                onRefresh = { vm.refresh() },
                onSettings = { showSettings = true }
            )

            if (tokens.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "summary") {
                        val active = alerts.count { it.enabled }
                        Text(
                            "${tokens.size} tokens  ·  $active alertas activas",
                            color = TextDim,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(tokens, key = { it.mint }) { token ->
                        TokenCard(
                            token = token,
                            alertCount = alerts.count { it.mint == token.mint && it.enabled },
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
            containerColor = Purple,
            contentColor = Color.White,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Agregar token", fontWeight = FontWeight.SemiBold) },
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
            containerColor = Surface1
        ) {
            AddTokenSheet(vm = vm, onDone = { showAdd = false })
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor = Surface1
        ) {
            SettingsSheet()
        }
    }
}

@Composable
private fun Header(refreshing: Boolean, onRefresh: () -> Unit, onSettings: () -> Unit) {
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(
        0f,
        360f,
        infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("SolPulse", style = gradientTitle(30))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot()
                Spacer(Modifier.width(6.dp))
                Text("en vivo", color = TextDim, fontSize = 12.sp)
            }
        }
        IconButton(onClick = onRefresh) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Actualizar",
                tint = TextMain,
                modifier = Modifier.rotate(if (refreshing) angle else 0f)
            )
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Ajustes", tint = TextMain)
        }
    }
}

@Composable
private fun EmptyState() {
    val t = rememberInfiniteTransition(label = "empty")
    val scale by t.animateFloat(
        0.9f,
        1.1f,
        infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "scale"
    )
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(110.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Purple.copy(alpha = 0.55f), Green.copy(alpha = 0.10f), Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("◎", color = Color.White, fontSize = 44.sp)
        }
        Spacer(Modifier.size(20.dp))
        Text("Todavía no seguís ningún token", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.size(6.dp))
        Text(
            "Tocá “Agregar token”, pegá el contrato y listo: precio en vivo, alertas y widget.",
            color = TextDim,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun TokenCard(
    token: Token,
    alertCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = if (token.change24h >= 0) Green else Red
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")

    Row(
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .glass()
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TokenLogo(token, 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(token.symbol, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    token.name,
                    color = TextDim,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (alertCount > 0) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Purple,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(" $alertCount", color = Purple, fontSize = 12.sp)
                }
            }
        }
        Sparkline(token.history, accent, Modifier.width(64.dp).size(width = 64.dp, height = 36.dp))
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            FlashingPrice(
                token.priceUsd,
                androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.size(4.dp))
            ChangeChip(token.change24h)
        }
    }
}

@Composable
private fun AddTokenSheet(vm: MainViewModel, onDone: () -> Unit) {
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
        Text("Agregar token", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Pegá la dirección del contrato (mint) del token en Solana.", color = TextDim, fontSize = 14.sp)

        OutlinedTextField(
            value = mint,
            onValueChange = {
                mint = it.trim()
                vm.resetAdd()
            },
            label = { Text("Contract address") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                TextButton(onClick = {
                    val pasted = clipboard.getText()?.text?.trim().orEmpty()
                    if (pasted.isNotEmpty()) {
                        mint = pasted
                        vm.lookup(pasted)
                    }
                }) { Text("Pegar") }
            }
        )

        when (val s = state) {
            AddState.Idle -> {}
            AddState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = Purple)
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
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) { Text("Seguir ${found.token.symbol}", fontWeight = FontWeight.SemiBold) }
        } else {
            Button(
                onClick = { vm.lookup(mint) },
                enabled = mint.isNotBlank() && state !is AddState.Loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) { Text("Buscar", fontWeight = FontWeight.SemiBold) }
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
            Text(token.symbol, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(token.name, color = TextDim, fontSize = 12.sp, maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Fmt.price(token.priceUsd), fontWeight = FontWeight.SemiBold)
            ChangeChip(token.change24h)
        }
    }
}

@Composable
private fun SettingsSheet() {
    val ctx = LocalContext.current
    val enabled by Repo.serviceEnabled.collectAsStateWithLifecycle()
    val interval by Repo.intervalSec.collectAsStateWithLifecycle()

    Column(
        Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp).navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Ajustes", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Monitoreo en segundo plano", fontWeight = FontWeight.SemiBold)
                Text("Revisa precios y manda alertas aunque cierres la app", color = TextDim, fontSize = 13.sp)
            }
            Switch(checked = enabled, onCheckedChange = { Repo.setServiceEnabled(it) })
        }

        Text("Frecuencia de chequeo", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15 to "15 s", 30 to "30 s", 60 to "1 min", 300 to "5 min").forEach { (sec, label) ->
                FilterChip(
                    selected = interval == sec,
                    onClick = { Repo.setIntervalSec(sec) },
                    label = { Text(label) },
                    colors = chipColors()
                )
            }
        }

        Button(
            onClick = { Notifier.test(ctx) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Surface2)
        ) { Text("Probar notificación (reloj)") }

        Text(
            "Para que el reloj reciba las alertas: en la app de tu reloj (Mi Fitness / Zepp) " +
                "activá las notificaciones para SolPulse. En Xiaomi/HyperOS: Ajustes › Apps › SolPulse › " +
                "Ahorro de batería: “Sin restricciones” e “Inicio automático” activado.",
            color = TextDim,
            fontSize = 12.sp
        )
    }
}
