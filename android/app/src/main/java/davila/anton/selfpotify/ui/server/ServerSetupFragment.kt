package davila.anton.selfpotify.ui.server

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import davila.anton.selfpotify.R
import davila.anton.selfpotify.databinding.FragmentServerSetupBinding
import kotlinx.coroutines.launch

/** Pantalla 1: el usuario introduce la dirección del servidor; se valida y se persiste. */
class ServerSetupFragment : Fragment() {

    private var _binding: FragmentServerSetupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ServerSetupViewModel by viewModels()

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
                launch { viewModel.state.collect(::render) }
                launch {
                    viewModel.navigateToAuth.collect {
                        findNavController().navigate(R.id.action_server_to_auth)
                    }
                }
            }
        }
    }

    private fun render(state: ServerUiState) {
        val checking = state is ServerUiState.Validating
        binding.progress.visibility = if (checking) View.VISIBLE else View.GONE
        binding.nextButton.isEnabled = state is ServerUiState.Valid

        when (state) {
            ServerUiState.Idle -> binding.statusText.text = ""
            ServerUiState.Validating -> {
                binding.statusText.text = getString(R.string.server_setup_checking)
                binding.statusText.setTextColor(colorSecondary())
            }
            is ServerUiState.Valid -> {
                binding.statusText.text = getString(R.string.server_setup_valid, state.appName)
                binding.statusText.setTextColor(colorAccent())
            }
            is ServerUiState.Invalid -> {
                val msg = when (state.error) {
                    ServerError.NOT_SELFPOTIFY -> R.string.server_setup_error_not_selfpotify
                    ServerError.UNREACHABLE -> R.string.server_setup_error_unreachable
                }
                binding.statusText.text = getString(msg)
                binding.statusText.setTextColor(colorError())
            }
        }
    }

    private fun colorAccent() =
        binding.root.context.getColor(R.color.fallback_accent)

    private fun colorSecondary() =
        binding.root.context.getColor(R.color.fallback_text_secondary)

    private fun colorError() =
        binding.root.context.getColor(R.color.fallback_error)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
