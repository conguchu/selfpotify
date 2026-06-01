package davila.anton.selfpotify.ui.auth

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
import davila.anton.selfpotify.databinding.FragmentAuthBinding
import kotlinx.coroutines.launch

/** Pantalla 2: login / registro. Al autenticarse, el JWT queda guardado y se navega al home. */
class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.submitButton.setOnClickListener {
            viewModel.submit(
                binding.username.text?.toString().orEmpty(),
                binding.password.text?.toString().orEmpty(),
            )
        }
        binding.toggleButton.setOnClickListener { viewModel.toggleMode() }
        binding.username.doAfterTextChanged { viewModel.clearError() }
        binding.password.doAfterTextChanged { viewModel.clearError() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch {
                    viewModel.navigateToHome.collect {
                        findNavController().navigate(R.id.action_auth_to_home)
                    }
                }
            }
        }
    }

    private fun render(state: AuthUiState) {
        val isLogin = state.mode == AuthMode.LOGIN
        binding.title.setText(if (isLogin) R.string.auth_login_title else R.string.auth_register_title)
        binding.submitButton.setText(if (isLogin) R.string.auth_login_button else R.string.auth_register_button)
        binding.toggleButton.setText(
            if (isLogin) R.string.auth_switch_to_register else R.string.auth_switch_to_login,
        )

        binding.progress.visibility = if (state.loading) View.VISIBLE else View.GONE
        // Mientras carga, oculta el texto del botón pero mantiene su tamaño (el progress va encima).
        binding.submitButton.isEnabled = !state.loading
        binding.submitButton.text =
            if (state.loading) "" else getString(
                if (isLogin) R.string.auth_login_button else R.string.auth_register_button,
            )

        val errorRes = when (state.error) {
            AuthError.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
            AuthError.USERNAME_TAKEN -> R.string.auth_error_username_taken
            AuthError.EMPTY_FIELDS -> R.string.auth_error_empty_fields
            AuthError.NETWORK -> R.string.auth_error_network
            AuthError.UNKNOWN -> R.string.auth_error_unknown
            null -> null
        }
        if (errorRes == null) {
            binding.errorText.visibility = View.GONE
        } else {
            binding.errorText.setText(errorRes)
            binding.errorText.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
