package davila.anton.selfpotify.ui.auth

import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import davila.anton.selfpotify.R
import davila.anton.selfpotify.ui.common.ServerLogo
import davila.anton.selfpotify.ui.theme.Spacing

@Composable
fun AuthScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToServer: () -> Unit,
    vm: AuthViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        vm.navigateToHome.collect { onNavigateToHome() }
    }
    LaunchedEffect(Unit) {
        vm.navigateToServer.collect { onNavigateToServer() }
    }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLogin = state.mode == AuthMode.LOGIN

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
                text = stringResource(if (isLogin) R.string.auth_login_title else R.string.auth_register_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.xl))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it; vm.clearError() },
                label = { Text(stringResource(R.string.auth_username_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.m))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; vm.clearError() },
                label = { Text(stringResource(R.string.auth_password_hint)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboard?.hide()
                    vm.submit(username, password)
                }),
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.error != null) {
                Spacer(Modifier.height(Spacing.m))
                val errorRes = when (state.error) {
                    AuthError.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
                    AuthError.USERNAME_TAKEN -> R.string.auth_error_username_taken
                    AuthError.EMPTY_FIELDS -> R.string.auth_error_empty_fields
                    AuthError.NETWORK -> R.string.auth_error_network
                    else -> R.string.auth_error_unknown
                }
                Text(
                    text = stringResource(errorRes),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(Spacing.l))
            Box(modifier = Modifier.fillMaxWidth().height(Spacing.button)) {
                Button(
                    onClick = { vm.submit(username, password) },
                    enabled = !state.loading,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (!state.loading) {
                        Text(stringResource(if (isLogin) R.string.auth_login_button else R.string.auth_register_button))
                    }
                }
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            TextButton(onClick = { vm.toggleMode() }) {
                Text(
                    text = stringResource(if (isLogin) R.string.auth_switch_to_register else R.string.auth_switch_to_login),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            TextButton(onClick = { vm.changeServer() }) {
                Text(
                    text = stringResource(R.string.auth_change_server),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
