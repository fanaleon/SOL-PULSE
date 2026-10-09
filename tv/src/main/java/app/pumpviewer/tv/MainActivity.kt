package app.pumpviewer.tv

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: TvViewModel by viewModels()

    // Aplica el idioma elegido en la app (o el de la TV si está en "Auto").
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Lang.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Es un tablero de precios: que la TV no se apague mientras está abierto.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Baja precios cada 20 s solo mientras la app está a la vista.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { vm.pollLoop() }
        }

        setContent {
            TvTheme {
                TvRoot(
                    vm = vm,
                    language = Lang.get(this),
                    onCycleLanguage = {
                        Lang.set(this, Lang.next(Lang.get(this)))
                        recreate()
                    }
                )
            }
        }
    }
}
