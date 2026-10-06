@file:OptIn(ExperimentalMaterial3Api::class)

package app.solpulse.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.solpulse.data.AlertType
import app.solpulse.data.Fmt
import app.solpulse.data.PriceAlert
import app.solpulse.data.Token

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
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextMain)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Dejar de seguir", tint = TextDim)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TokenLogo(token, 60.dp)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(token.symbol, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text(token.name, color = TextDim, fontSize = 14.sp, maxLines = 1)
            }
        }

        Spacer(Modifier.height(18.dp))
        FlashingPrice(
            token.priceUsd,
            TextStyle(fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("5m", color = TextDim, fontSize = 12.sp)
            ChangeChip(token.change5m)
            Text("1h", color = TextDim, fontSize = 12.sp)
            ChangeChip(token.change1h)
            Text("24h", color = TextDim, fontSize = 12.sp)
            ChangeChip(token.change24h)
        }

        Spacer(Modifier.height(18.dp))
        Column(Modifier.fillMaxWidth().glass().padding(14.dp)) {
            Sparkline(token.history, accent, Modifier.fillMaxWidth().height(150.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                "Historial desde que seguís el token · ${token.history.size} muestras",
                color = TextDim,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Liquidez", Fmt.compact(token.liquidityUsd), Modifier.weight(1f))
            StatCard("Vol. 24h", Fmt.compact(token.volume24h), Modifier.weight(1f))
            StatCard("Mkt cap", Fmt.compact(token.marketCap), Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Alertas", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(
                onClick = { showNewAlert = true },
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Nueva")
            }
        }
        Spacer(Modifier.height(10.dp))

        if (alerts.isEmpty()) {
            Text(
                "Sin alertas. Creá una y te avisamos al celu y al reloj cuando el precio llegue.",
                color = TextDim,
                fontSize = 14.sp
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
                modifier = Modifier.weight(1f)
            ) { Text(Fmt.shortMint(token.mint), maxLines = 1) }
            OutlinedButton(
                onClick = {
                    val url = token.pairUrl ?: "https://dexscreener.com/solana/${token.mint}"
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                modifier = Modifier.weight(1f)
            ) { Text("DexScreener", maxLines = 1) }
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
            containerColor = Surface1,
            title = { Text("¿Dejar de seguir ${token.symbol}?") },
            text = { Text("Se borran también sus alertas.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.removeToken(token.mint)
                }) { Text("Dejar de seguir", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.glass(16.dp).padding(12.dp)) {
        Text(label, color = TextDim, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
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
    val c = if (up) Green else Red
    val diff = if (token.priceUsd > 0) (alert.target - token.priceUsd) / token.priceUsd * 100.0 else 0.0
    Row(
        Modifier.fillMaxWidth().glass(18.dp).padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (up) "▲" else "▼", color = c, fontSize = 20.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                (if (up) "Sube a " else "Baja a ") + Fmt.price(alert.target),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                if (alert.enabled) "${Fmt.pct(diff)} desde el precio actual" else "Disparada o pausada",
                color = TextDim,
                fontSize = 12.sp
            )
        }
        Switch(checked = alert.enabled, onCheckedChange = onToggle)
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Borrar alerta", tint = TextDim)
        }
    }
}

@Composable
private fun QuickChip(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, fontSize = 13.sp)
    }
}

@Composable
private fun NewAlertDialog(
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
        containerColor = Surface1,
        title = { Text("Nueva alerta · ${token.symbol}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Ahora: ${Fmt.price(token.priceUsd)}", color = TextDim, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == AlertType.ABOVE,
                        onClick = { type = AlertType.ABOVE },
                        label = { Text("▲ Sube a") },
                        colors = chipColors()
                    )
                    FilterChip(
                        selected = type == AlertType.BELOW,
                        onClick = { type = AlertType.BELOW },
                        label = { Text("▼ Baja a") },
                        colors = chipColors()
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.replace(',', '.') },
                    label = { Text("Precio objetivo (USD)") },
                    singleLine = true,
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
                        color = Red,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onCreate(type, target!!) }
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
