package davila.anton.selfpotify.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import davila.anton.selfpotify.R
import davila.anton.selfpotify.data.local.SessionStore
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

        // Lectura puntual de la sesión al arrancar para elegir el destino inicial.
        val session = runBlocking { SessionStore(this@MainActivity).current() }
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
