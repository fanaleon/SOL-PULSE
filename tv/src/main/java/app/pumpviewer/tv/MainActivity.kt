package app.pumpviewer.tv

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
                TvRoot(vm)
            }
        }
    }
}
