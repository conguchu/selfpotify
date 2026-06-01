package davila.anton.selfpotify.ui

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import davila.anton.selfpotify.R
import davila.anton.selfpotify.data.local.SessionStore
import davila.anton.selfpotify.ui.theme.BrandingColors
import kotlinx.coroutines.runBlocking

/**
 * Única Activity: aloja el NavHost. El destino inicial se decide según el estado de
 * la sesión persistida (CLAUDE.md §2, navegación con Navigation Component):
 *  - sin servidor          -> pantalla de configuración de servidor
 *  - servidor pero sin JWT  -> login / registro
 *  - servidor + JWT válido  -> home
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host) as NavHostFragment
        val navController = navHost.navController

        // Lectura puntual de la sesión y la paleta al arrancar.
        val store = SessionStore(this@MainActivity)
        val session = runBlocking { store.current() }

        // Aplica el fondo del servidor a la ventana y barras del sistema desde el primer frame.
        val colors = BrandingColors.from(runBlocking { store.currentBrandingColors() })
        window.setBackgroundDrawable(ColorDrawable(colors.background))
        window.statusBarColor = colors.background
        window.navigationBarColor = colors.background
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(
            when {
                session.isLoggedIn -> R.id.homeFragment
                session.hasServer -> R.id.authFragment
                else -> R.id.serverSetupFragment
            }
        )
        navController.graph = graph
    }
}
