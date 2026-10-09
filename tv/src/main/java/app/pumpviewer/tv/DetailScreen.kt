package app.pumpviewer.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pumpviewer.data.AlertType
import app.pumpviewer.data.Fmt
import app.pumpviewer.data.PriceAlert
import app.pumpviewer.data.Token
import kotlinx.coroutines.delay

@Composable
fun DetailScreen(vm: TvViewModel, mint: String, onBack: () -> Unit) {
    val tokens by vm.tokens.collectAsStateWithLifecycle()
    val alerts by vm.alerts.collectAsStateWithLifecycle()
    val token = tokens.firstOrNull { it.mint == mint }
    if (token == null) {
        // El token se dejó de seguir: volvemos al tablero.
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val mine = alerts.filter { it.mint == mint }
    var confirmDelete by remember { mutableStateOf(false) }
    val tint = changeColor(token.change24h)
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(250)
        runCatching { firstFocus.requestFocus() }
    }

    Row(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(40.dp)
    ) {
        // ---------- Izquierda: precio, gráfico y datos ----------
        Column(
            Modifier
                .weight(1.5f)
                .fillMaxHeight()
                .padding(start = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TokenLogo(token.imageUrl, token.symbol, 76.dp)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(token.symbol, style = TextStyle(fontSize = 42.sp, fontWeight = FontWeight.ExtraBold), maxLines = 1)
                    Text(
                        token.name,
                        style = TextStyle(fontSize = 20.sp, color = TextDim),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ChangePill(token.change24h, 26.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text(Fmt.price(token.priceUsd), style = tnum(88.sp, FontWeight.ExtraBold), maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Sparkline(token.history, tint, Modifier.fillMaxWidth().height(200.dp), strokeWidth = 4.dp)
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Stat(stringResource(R.string.stat_5min), Fmt.pct(token.change5m), changeColor(token.change5m), Modifier.weight(1f))
                Stat(stringResource(R.string.stat_1h), Fmt.pct(token.change1h), changeColor(token.change1h), Modifier.weight(1f))
                Stat(stringResource(R.string.stat_24h), Fmt.pct(token.change24h), changeColor(token.change24h), Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Stat(stringResource(R.string.stat_liquidity), Fmt.compact(token.liquidityUsd), TextMain, Modifier.weight(1f))
                Stat(stringResource(R.string.stat_volume), Fmt.compact(token.volume24h), TextMain, Modifier.weight(1f))
                Stat(stringResource(R.string.stat_market_cap), Fmt.compact(token.marketCap), TextMain, Modifier.weight(1f))
            }
        }

        // ---------- Derecha: alertas y acciones ----------
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Text(stringResource(R.string.alerts_title), style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.alerts_subtitle),
                style = TextStyle(fontSize = 16.sp, color = TextDim)
            )
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Shortcut("+5%", Green, Modifier.focusRequester(firstFocus)) { vm.addAlertPct(token, 5.0) }
                Shortcut("+10%", Green) { vm.addAlertPct(token, 10.0) }
                Shortcut("−5%", Red) { vm.addAlertPct(token, -5.0) }
                Shortcut("−10%", Red) { vm.addAlertPct(token, -10.0) }
            }
            Spacer(Modifier.height(16.dp))

            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (mine.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.alerts_empty),
                            style = TextStyle(fontSize = 18.sp, color = TextDim, lineHeight = 26.sp)
                        )
                    }
                }
                items(mine, key = { it.id }) { alert ->
                    AlertRow(
                        alert = alert,
                        onToggle = { vm.setAlertEnabled(alert.id, !alert.enabled) },
                        onDelete = { vm.removeAlert(alert.id) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TvButton(stringResource(R.string.back_button), onClick = onBack)
                TvButton(
                    text = stringResource(if (confirmDelete) R.string.unfollow_confirm else R.string.unfollow),
                    container = if (confirmDelete) Red else Surface1,
                    textColor = if (confirmDelete) Color(0xFF3A0A14) else Red,
                    onClick = {
                        if (confirmDelete) {
                            vm.removeToken(token.mint)
                        } else {
                            confirmDelete = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(label, style = TextStyle(fontSize = 16.sp, color = TextDim))
        Text(value, style = tnum(28.sp, FontWeight.Bold, valueColor), maxLines = 1)
    }
}

@Composable
private fun Shortcut(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .tvClickable(RoundedCornerShape(14.dp), onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = tnum(22.sp, FontWeight.Bold, color))
    }
}

@Composable
private fun AlertRow(alert: PriceAlert, onToggle: () -> Unit, onDelete: () -> Unit) {
    val above = alert.type == AlertType.ABOVE
    val accent = if (alert.enabled) Amber else TextDim
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier
                .weight(1f)
                .tvClickable(RoundedCornerShape(16.dp), onClick = onToggle)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text(
                stringResource(if (above) R.string.alert_above else R.string.alert_below, Fmt.price(alert.target)),
                style = tnum(22.sp, FontWeight.Bold, accent),
                maxLines = 1
            )
            Text(
                stringResource(if (alert.enabled) R.string.alert_active else R.string.alert_paused),
                style = TextStyle(fontSize = 15.sp, color = TextDim)
            )
        }
        Box(
            Modifier
                .tvClickable(RoundedCornerShape(16.dp), onClick = onDelete)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.alert_delete), style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Red))
        }
    }
}
