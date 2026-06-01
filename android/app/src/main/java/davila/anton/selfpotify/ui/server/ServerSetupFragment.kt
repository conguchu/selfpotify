package davila.anton.selfpotify.ui.server

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import davila.anton.selfpotify.R
import davila.anton.selfpotify.databinding.FragmentServerSetupBinding
import davila.anton.selfpotify.ui.theme.BrandingColors
import davila.anton.selfpotify.ui.theme.ThemeViewModel
import davila.anton.selfpotify.ui.theme.applyBranding
import davila.anton.selfpotify.ui.theme.applyFilled
import kotlinx.coroutines.launch

/** Pantalla 1: el usuario introduce la dirección del servidor; se valida y se persiste. */
class ServerSetupFragment : Fragment() {

    private var _binding: FragmentServerSetupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ServerSetupViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by activityViewModels()

    private var uiState: ServerUiState = ServerUiState.Idle
    private var colors: BrandingColors = BrandingColors.fallback()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentServerSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.serverAddress.doAfterTextChanged {
            viewModel.onAddressChanged(it?.toString().orEmpty())
        }
        binding.nextButton.setOnClickListener { viewModel.onNextClicked() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.state.collect {
                        uiState = it
                        render()
                    }
                }
                launch {
                    themeViewModel.colors.collect {
                        colors = it
                        applyColors()
                        render()
                    }
                }
                launch {
                    viewModel.navigateToAuth.collect {
                        findNavController().navigate(R.id.action_server_to_auth)
                    }
                }
            }
        }
    }

    /** Aplica la paleta del servidor a las vistas estáticas de la pantalla. */
    private fun applyColors() {
        binding.root.setBackgroundColor(colors.background)
        binding.serverInputLayout.applyBranding(colors)
        binding.progress.applyBranding(colors)
        binding.nextButton.applyFilled(colors)
    }

    private fun render() {
        val checking = uiState is ServerUiState.Validating
        binding.progress.visibility = if (checking) View.VISIBLE else View.GONE
        binding.nextButton.isEnabled = uiState is ServerUiState.Valid

        when (val state = uiState) {
            ServerUiState.Idle -> binding.statusText.text = ""
            ServerUiState.Validating -> {
                binding.statusText.text = getString(R.string.server_setup_checking)
                binding.statusText.setTextColor(colors.textSecondary)
            }
            is ServerUiState.Valid -> {
                binding.statusText.text = getString(R.string.server_setup_valid, state.appName)
                binding.statusText.setTextColor(colors.accent)
            }
            is ServerUiState.Invalid -> {
                val msg = when (state.error) {
                    ServerError.NOT_SELFPOTIFY -> R.string.server_setup_error_not_selfpotify
                    ServerError.UNREACHABLE -> R.string.server_setup_error_unreachable
                }
                binding.statusText.text = getString(msg)
                binding.statusText.setTextColor(colors.error)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
