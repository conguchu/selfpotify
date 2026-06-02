package davila.anton.selfpotify.ui.server

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import davila.anton.selfpotify.R
import davila.anton.selfpotify.ui.common.ServerLogo
import davila.anton.selfpotify.ui.theme.Spacing

@Composable
fun ServerSetupScreen(
    onNavigateToAuth: () -> Unit,
    vm: ServerSetupViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        vm.navigateToAuth.collect { onNavigateToAuth() }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.page)
                .padding(top = Spacing.xxl, bottom = Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ServerLogo(modifier = Modifier.size(Spacing.logo))
            Spacer(Modifier.height(Spacing.l))

            Text(
                text = stringResource(R.string.server_setup_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.s))
            Text(
                text = stringResource(R.string.server_setup_subtitle),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xl))

            var address by remember { mutableStateOf("") }
            OutlinedTextField(
                value = address,
                onValueChange = { address = it; vm.onAddressChanged(it) },
                label = { Text(stringResource(R.string.server_setup_address_hint)) },
                supportingText = { Text(stringResource(R.string.server_setup_address_helper)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.m))

            when (val s = state) {
                ServerUiState.Idle -> Spacer(Modifier.height(20.dp))
                ServerUiState.Validating -> {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
                is ServerUiState.Valid -> Text(
                    text = stringResource(R.string.server_setup_valid, s.appName),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
                is ServerUiState.Invalid -> Text(
                    text = stringResource(
                        if (s.error == ServerError.NOT_SELFPOTIFY) R.string.server_setup_error_not_selfpotify
                        else R.string.server_setup_error_unreachable,
                    ),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(Spacing.l))
            Button(
                onClick = { vm.onNextClicked() },
                enabled = state is ServerUiState.Valid,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.button),
            ) {
                Text(stringResource(R.string.server_setup_next))
            }
        }
    }
}
