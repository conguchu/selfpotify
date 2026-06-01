package davila.anton.selfpotify.ui.home

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
import davila.anton.selfpotify.databinding.FragmentHomeBinding
import davila.anton.selfpotify.ui.theme.BrandingColors
import davila.anton.selfpotify.ui.theme.ThemeViewModel
import davila.anton.selfpotify.ui.theme.applyFilled
import davila.anton.selfpotify.ui.theme.applyOutlined
import kotlinx.coroutines.launch

/** Pantalla 3: saludo, logout (borra JWT) y cambiar de servidor (borra servidor + JWT). */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.logoutButton.setOnClickListener { viewModel.logout() }
        binding.changeServerButton.setOnClickListener { viewModel.changeServer() }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.username.collect { name ->
                        binding.greeting.text = getString(R.string.home_greeting, name.orEmpty())
                    }
                }
                launch {
                    themeViewModel.colors.collect(::applyColors)
                }
                launch {
                    viewModel.navigate.collect { dest ->
                        val action = when (dest) {
                            HomeNav.TO_AUTH -> R.id.action_home_to_auth
                            HomeNav.TO_SERVER -> R.id.action_home_to_server
                        }
                        findNavController().navigate(action)
                    }
                }
            }
        }
    }

    /** Aplica la paleta del servidor a las vistas de la pantalla. */
    private fun applyColors(colors: BrandingColors) {
        binding.root.setBackgroundColor(colors.background)
        binding.greeting.setTextColor(colors.textPrimary)
        binding.logoutButton.applyFilled(colors)
        binding.changeServerButton.applyOutlined(colors)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
