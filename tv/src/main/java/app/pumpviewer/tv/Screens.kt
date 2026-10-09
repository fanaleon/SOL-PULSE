package app.pumpviewer.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.pumpviewer.data.Fmt
import app.pumpviewer.data.Token
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface Screen {
    data object Dashboard : Screen
    data object Add : Screen
    data class Detail(val mint: String) : Screen
}

@Composable
fun TvRoot(vm: TvViewModel) {
    var screen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    val banner by vm.banner.collectAsStateWithLifecycle()

    // Atrás vuelve al tablero; estando en el tablero sale de la app.
    BackHandler(enabled = screen != Screen.Dashboard) { screen = Screen.Dashboard }

    TvBackground {
        when (val s = screen) {
            Screen.Dashboard -> DashboardScreen(
                vm = vm,
                onOpen = { screen = Screen.Detail(it) },
                onAdd = { screen = Screen.Add }
            )
            Screen.Add -> AddScreen(vm = vm, onBack = { screen = Screen.Dashboard })
            is Screen.Detail -> DetailScreen(vm = vm, mint = s.mint, onBack = { screen = Screen.Dashboard })
        }
        banner?.let { BannerPill(it, Modifier.align(Alignment.TopCenter)) }
    }
}

@Composable
private fun BannerPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(top = 28.dp)
            .clip(RoundedCornerShape(50))
            .background(Amber)
            .padding(horizontal = 32.dp, vertical = 14.dp)
    ) {
        Text(text, style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = OnAmber))
    }
}

private val OnAmber = Color(0xFF2B1D00)

// ---------------------------------------------------------------------------------------------
// Tablero
// ---------------------------------------------------------------------------------------------

@Composable
fun DashboardScreen(vm: TvViewModel, onOpen: (String) -> Unit, onAdd: () -> Unit) {
    val tokens by vm.tokens.collectAsStateWithLifecycle()
    val offline by vm.offline.collectAsStateWithLifecycle()
    val lastOk by vm.lastOk.collectAsStateWithLifecycle()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(5_000)
        }
    }
    val firstCard = remember { FocusRequester() }
    val addButton = remember { FocusRequester() }
    val clock = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Al abrir, el foco arranca en el primer token (o en "Agregar" si todavía no hay ninguno).
    LaunchedEffect(tokens.isEmpty()) {
        delay(250)
        runCatching { if (tokens.isEmpty()) addButton.requestFocus() else firstCard.requestFocus() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 32.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Pump Viewer TV", style = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.ExtraBold))
                val status = when {
                    offline -> "Sin conexión, reintentando…"
                    tokens.isEmpty() -> "Sin tokens todavía"
                    lastOk == 0L -> "Cargando precios…"
                    else -> {
                        val secs = ((now - lastOk) / 1000).coerceAtLeast(0)
                        if (secs < 60) "Actualizado hace ${secs}s" else "Actualizado hace ${secs / 60} min"
                    }
                }
                Text(
                    status,
                    style = TextStyle(fontSize = 18.sp, color = if (offline) Amber else TextDim)
                )
            }
            Text(
                clock.format(Date(now)),
                style = tnum(40.sp, FontWeight.Light, TextDim)
            )
            Spacer(Modifier.width(32.dp))
            TvButton(
                text = "+  Agregar token",
                modifier = Modifier.focusRequester(addButton),
                container = PurpleDeep,
                textColor = Color.White,
                onClick = onAdd
            )
        }

        if (tokens.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Todavía no seguís ningún token", style = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(10.dp))
                Text(
                    "Apretá “Agregar token”: podés mandar las direcciones desde el celu, sin escribir con el control.",
                    style = TextStyle(fontSize = 20.sp, color = TextDim)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(400.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(26.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp)
            ) {
                itemsIndexed(tokens, key = { _, t -> t.mint }) { index, token ->
                    TokenCard(
                        token = token,
                        modifier = if (index == 0) Modifier.focusRequester(firstCard) else Modifier,
                        onClick = { onOpen(token.mint) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TokenCard(token: Token, modifier: Modifier, onClick: () -> Unit) {
    val tint = changeColor(token.change24h)
    Column(
        modifier
            .fillMaxWidth()
            .tvClickable(RoundedCornerShape(26.dp), onClick = onClick)
            .padding(22.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TokenLogo(token.imageUrl, token.symbol, 58.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    token.symbol,
                    style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    token.name,
                    style = TextStyle(fontSize = 16.sp, color = TextDim),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ChangePill(token.change24h)
        }
        Spacer(Modifier.height(14.dp))
        Text(Fmt.price(token.priceUsd), style = tnum(48.sp, FontWeight.ExtraBold), maxLines = 1)
        Spacer(Modifier.height(8.dp))
        Sparkline(token.history, tint, Modifier.fillMaxWidth().height(70.dp))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("5m ${Fmt.pct(token.change5m)}", style = tnum(17.sp, FontWeight.Medium, changeColor(token.change5m)))
            Text("1h ${Fmt.pct(token.change1h)}", style = tnum(17.sp, FontWeight.Medium, changeColor(token.change1h)))
            Text("MC ${Fmt.compact(token.marketCap)}", style = tnum(17.sp, FontWeight.Medium, TextDim))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Agregar token
// ---------------------------------------------------------------------------------------------

@Composable
fun AddScreen(vm: TvViewModel, onBack: () -> Unit) {
    val state by vm.addState.collectAsStateWithLifecycle()
    var text by remember { mutableStateOf("") }
    val ip = remember { LocalServer.localIp() }
    val firstFocus = remember { FocusRequester() }

    // El servidor para cargar desde el celu vive solo mientras esta pantalla está abierta.
    DisposableEffect(Unit) {
        val server = LocalServer { vm.importFromText(it) }
        server.start()
        vm.resetAdd()
        onDispose {
            server.stop()
            vm.resetAdd()
        }
    }
    LaunchedEffect(Unit) {
        delay(250)
        runCatching { firstFocus.requestFocus() }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 56.dp, vertical = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            TvButton("←  Volver", modifier = Modifier.focusRequester(firstFocus), onClick = onBack)
            Spacer(Modifier.width(28.dp))
            Text("Agregar token", style = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.ExtraBold))
        }
        Spacer(Modifier.height(30.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            // --- Desde el celu ---
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Surface1)
                    .padding(28.dp)
            ) {
                Text("Desde el celu (lo más fácil)", style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(14.dp))
                Text(
                    "1. Conectá el celu al mismo Wi‑Fi que la TV.\n2. Abrí en el navegador del celu:",
                    style = TextStyle(fontSize = 20.sp, color = TextDim, lineHeight = 30.sp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (ip != null) "http://$ip:${LocalServer.PORT}" else "No encuentro la red de la TV",
                    style = TextStyle(fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Cyan)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "3. Pegá las direcciones de los tokens y tocá “Enviar a la TV”.",
                    style = TextStyle(fontSize = 20.sp, color = TextDim, lineHeight = 30.sp)
                )
            }

            // --- A mano ---
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Surface1)
                    .padding(28.dp)
            ) {
                Text("O escribila acá", style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold))
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        if (state !is AddState.Idle) vm.resetAdd()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Dirección del token (mint)") },
                    textStyle = TextStyle(fontSize = 20.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain,
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = Border,
                        focusedLabelColor = Cyan,
                        unfocusedLabelColor = TextDim,
                        cursorColor = Cyan
                    )
                )
                Spacer(Modifier.height(14.dp))
                TvButton("Buscar", onClick = { vm.lookup(text) })
                Spacer(Modifier.height(18.dp))

                when (val s = state) {
                    AddState.Idle -> Unit
                    AddState.Loading -> Text("Buscando…", style = TextStyle(fontSize = 20.sp, color = TextDim))
                    is AddState.Error -> Text(s.message, style = TextStyle(fontSize = 20.sp, color = Red))
                    is AddState.Found -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TokenLogo(s.token.imageUrl, s.token.symbol, 56.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.token.symbol, style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold))
                                Text(Fmt.price(s.token.priceUsd), style = tnum(22.sp, FontWeight.Medium, TextDim))
                            }
                            ChangePill(s.token.change24h)
                        }
                        Spacer(Modifier.height(14.dp))
                        TvButton(
                            "Seguir este token",
                            container = PurpleDeep,
                            textColor = Color.White,
                            onClick = {
                                vm.confirmAdd()
                                text = ""
                            }
                        )
                    }
                }
            }
        }
    }
}
