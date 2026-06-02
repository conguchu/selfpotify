package davila.anton.selfpotify.ui.offline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import davila.anton.selfpotify.R
import davila.anton.selfpotify.databinding.FragmentConnectionLostBinding
import davila.anton.selfpotify.ui.theme.BrandingColors
import davila.anton.selfpotify.ui.theme.ThemeViewModel
import davila.anton.selfpotify.ui.theme.applyBranding
import davila.anton.selfpotify.ui.theme.applyFilled
import davila.anton.selfpotify.ui.theme.applyOutlined
import kotlinx.coroutines.launch

/**
 * Pantalla de sin-conexión: el usuario está logueado pero el servidor no responde.
 * Permite reintentar la conexión o desconectarse del servidor (vuelve a la pantalla 1).
 */
class ConnectionLostFragment : Fragment() {

    private var _binding: FragmentConnectionLostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConnectionLostViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by activityViewModels()

    private var colors: BrandingColors = BrandingColors.fallback()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentConnectionLostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.retryButton.setOnClickListener { viewModel.retry() }
        binding.disconnectButton.setOnClickListener { viewModel.disconnect() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.retrying.collect(::renderRetrying) }
                launch {
                    themeViewModel.colors.collect {
                        colors = it
                        applyColors()
                    }
                }
                launch {
                    viewModel.navigate.collect { dest ->
                        val action = when (dest) {
                            OfflineNav.TO_HOME -> R.id.action_offline_to_home
                            OfflineNav.TO_SERVER -> R.id.action_offline_to_server
                        }
                        findNavController().navigate(action)
                    }
                }
            }
        }
    }

    /** Mientras reintenta: muestra el spinner y deshabilita los botones. */
    private fun renderRetrying(retrying: Boolean) {
        binding.progress.visibility = if (retrying) View.VISIBLE else View.GONE
        binding.retryButton.isEnabled = !retrying
        binding.disconnectButton.isEnabled = !retrying
    }

    /** Aplica la paleta del servidor a las vistas de la pantalla. */
    private fun applyColors() {
        binding.root.setBackgroundColor(colors.background)
        binding.message.setTextColor(colors.textPrimary)
        binding.progress.applyBranding(colors)
        binding.retryButton.applyFilled(colors)
        binding.disconnectButton.applyOutlined(colors)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
