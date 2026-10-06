package app.pumpviewer

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.Looper
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import app.pumpviewer.data.AlertType
import app.pumpviewer.data.Repo
import app.pumpviewer.data.Token
import app.pumpviewer.ui.AddTokenSheet
import app.pumpviewer.ui.AppBackground
import app.pumpviewer.ui.DetailScreen
import app.pumpviewer.ui.HomeScreen
import app.pumpviewer.ui.MainViewModel
import app.pumpviewer.ui.Motion
import app.pumpviewer.ui.NewAlertDialog
import app.pumpviewer.ui.PumpViewerTheme
import app.pumpviewer.ui.SettingsSheet
import app.pumpviewer.ui.Surface1
import app.pumpviewer.widget.WidgetContent
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.sin

/**
 * No prueba lógica: dibuja las pantallas y el widget a imágenes (app/build/capturas) para poder
 * revisar a ojo cómo quedan los colores y la distribución sin tener un teléfono a mano.
 */
// Se usa una Application vacía: la de la app arranca WorkManager y el servicio, que acá no hacen falta.
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w393dp-h873dp-xxhdpi")
class Capturas {

    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        Motion.enabled = false
        Repo.init(app)
        Repo.tokens.value.forEach { Repo.removeToken(it.mint) }
        shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
    }

    // ------------------------------------------------------------------ datos de muestra

    private val slop = Token(
        mint = "SLoPcoRe1111111111111111111111111111111111",
        symbol = "SLOPCORE",
        name = "SLOPCORE",
        priceUsd = 0.0005282,
        change5m = 1.0,
        change1h = -0.39,
        change24h = -21.83,
        liquidityUsd = 78_800.0,
        volume24h = 570_100.0,
        marketCap = 515_000.0
    )
    private val bonk = Token(
        mint = "BoNK222222222222222222222222222222222222222",
        symbol = "BONK",
        name = "Bonk",
        priceUsd = 0.00002431,
        change5m = -0.2,
        change1h = 2.1,
        change24h = 8.42,
        liquidityUsd = 3_400_000.0,
        volume24h = 91_000_000.0,
        marketCap = 1_900_000_000.0
    )
    private val wif = Token(
        mint = "WiF3333333333333333333333333333333333333333",
        symbol = "WIF",
        name = "dogwifhat",
        priceUsd = 2.413,
        change5m = 0.0,
        change1h = 0.31,
        change24h = 1.27,
        liquidityUsd = 12_000_000.0,
        volume24h = 240_000_000.0,
        marketCap = 2_410_000_000.0
    )

    /** Curva parecida a la de la captura real: sube, hace pico, cae y se aplana. */
    private fun curve(end: Double, shape: Int): List<Double> = (0 until 120).map { i ->
        val t = i / 119.0
        val wobble = 0.03 * sin(i * 0.9) + 0.02 * sin(i * 0.37 + shape)
        val body = when (shape) {
            0 -> if (t < 0.45) 0.75 + 1.3 * t else if (t < 0.6) 1.5 - 2.2 * (t - 0.5) else 1.0 - 0.1 * (t - 0.6)
            1 -> 0.7 + 0.35 * t + 0.1 * sin(t * 9)
            else -> 1.0 + 0.04 * sin(t * 14)
        }
        end * (body + wobble) / (if (shape == 0) 0.96 else if (shape == 1) 1.05 else 1.0)
    }

    private fun follow(token: Token, shape: Int): Token {
        Repo.addToken(token)
        curve(token.priceUsd, shape).forEach { p -> Repo.applyQuotes(mapOf(token.mint to token.copy(priceUsd = p))) }
        Repo.applyQuotes(mapOf(token.mint to token))
        return Repo.tokens.value.first { it.mint == token.mint }
    }

    private fun seedOne(): Token {
        val t = follow(slop, 0)
        Repo.addAlert(t.mint, AlertType.ABOVE, 0.001)
        Repo.addAlert(t.mint, AlertType.ABOVE, 0.0007)
        Repo.addAlert(t.mint, AlertType.ABOVE, 0.0006)
        return t
    }

    private fun seedThree(): List<Token> {
        val a = seedOne()
        val b = follow(bonk, 1)
        val c = follow(wif, 2)
        Repo.addAlert(b.mint, AlertType.BELOW, 0.00002)
        return listOf(a, b, c)
    }

    private fun screen(name: String, content: @Composable () -> Unit) {
        captureRoboImage("build/capturas/$name.png") {
            PumpViewerTheme { AppBackground { content() } }
        }
    }

    // ------------------------------------------------------------------ pantallas

    @Test
    fun a1_inicio_un_token() {
        seedOne()
        val vm = MainViewModel(app)
        screen("01-inicio") { HomeScreen(vm = vm, onOpen = {}) }
    }

    @Test
    fun a2_inicio_tres_tokens() {
        seedThree()
        val vm = MainViewModel(app)
        screen("02-inicio-tres-tokens") { HomeScreen(vm = vm, onOpen = {}) }
    }

    @Test
    fun a3_inicio_vacio() {
        val vm = MainViewModel(app)
        screen("03-inicio-vacio") { HomeScreen(vm = vm, onOpen = {}) }
    }

    @Test
    @Config(sdk = [34], application = Application::class, qualifiers = "w393dp-h1500dp-xxhdpi")
    fun b1_detalle() {
        val t = seedOne()
        Repo.setAlertEnabled(Repo.alerts.value.first().id, false)
        val vm = MainViewModel(app)
        screen("04-detalle") { DetailScreen(vm = vm, mint = t.mint, onBack = {}) }
    }

    @Test
    @Config(sdk = [34], application = Application::class, qualifiers = "w393dp-h1100dp-xxhdpi")
    fun b2_hojas() {
        val vm = MainViewModel(app)
        screen("05-agregar-y-ajustes") {
            Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Box(Modifier.fillMaxWidth().background(Surface1).padding(top = 20.dp)) { AddTokenSheet(vm = vm, onDone = {}) }
                Box(Modifier.fillMaxWidth().background(Surface1).padding(top = 20.dp)) { SettingsSheet() }
            }
        }
    }

    @Test
    fun b3_dialogo_nueva_alerta() {
        val t = seedOne()
        val vm = MainViewModel(app)
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        activity.setContent {
            PumpViewerTheme {
                AppBackground {
                    DetailScreen(vm = vm, mint = t.mint, onBack = {})
                    NewAlertDialog(token = t, onDismiss = {}, onCreate = { _, _ -> })
                }
            }
        }
        shadowOf(Looper.getMainLooper()).idle()
        captureScreenRoboImage("build/capturas/06-nueva-alerta.png")
    }

    // ------------------------------------------------------------------ widget

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    private fun widget(name: String, width: Int, height: Int, tokens: List<Token>) {
        val ctx: Context = app
        val views = runBlocking {
            GlanceRemoteViews().compose(ctx, DpSize(width.dp, height.dp)) { WidgetContent(tokens) }.remoteViews
        }
        captureRoboImage("build/capturas/$name.png") {
            // Fondo parecido a un fondo de pantalla oscuro, para ver el borde del widget.
            Box(Modifier.background(Color(0xFF2B2440)).padding(14.dp)) {
                AndroidView(
                    factory = { c -> views.apply(c, FrameLayout(c)) },
                    modifier = Modifier.size(width.dp, height.dp)
                )
            }
        }
    }

    @Test
    fun c1_widget_tira_un_token() = widget("10-widget-4x1-un-token", 340, 64, listOf(seedOne()))

    @Test
    fun c2_widget_tira_tres_tokens() = widget("11-widget-4x1-tres-tokens", 340, 64, seedThree())

    @Test
    fun c3_widget_tira_angosta() = widget("12-widget-3x1", 250, 58, listOf(seedOne()))

    @Test
    fun c4_widget_lista_un_token() = widget("13-widget-4x2-un-token", 340, 150, listOf(seedOne()))

    @Test
    fun c5_widget_lista_tres_tokens() = widget("14-widget-4x2-tres-tokens", 340, 150, seedThree())

    @Test
    fun c6_widget_vacio() = widget("15-widget-vacio", 340, 64, emptyList())
}
