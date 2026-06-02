package davila.anton.selfpotify.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import davila.anton.selfpotify.R
import davila.anton.selfpotify.ui.theme.Spacing

@Composable
fun HomeScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToServer: () -> Unit,
    onNavigateToOffline: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val username by vm.username.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.navigate.collect { dest ->
            when (dest) {
                HomeNav.TO_AUTH -> onNavigateToAuth()
                HomeNav.TO_SERVER -> onNavigateToServer()
                HomeNav.TO_OFFLINE -> onNavigateToOffline()
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.checkConnection()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.page),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_selfpotify),
                contentDescription = stringResource(R.string.cd_logo),
                modifier = Modifier.size(Spacing.logo),
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(Spacing.l))

            Text(
                text = stringResource(R.string.home_greeting, username.orEmpty()),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(Spacing.xxl))

            Button(
                onClick = { vm.logout() },
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(Spacing.button),
            ) {
                Text(stringResource(R.string.home_logout))
            }
            Spacer(Modifier.height(Spacing.m))
            OutlinedButton(
                onClick = { vm.changeServer() },
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(Spacing.button),
            ) {
                Text(stringResource(R.string.home_change_server))
            }
        }
    }
}
