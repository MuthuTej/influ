package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SplashState
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SplashViewModel

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel
) {
    val splashState = viewModel.splashState.observeAsState()

    LaunchedEffect(key1 = true) {
        viewModel.checkAppState()
    }

    LaunchedEffect(key1 = splashState.value) {
        when (val state = splashState.value) {
            is SplashState.NavigateToOnboarding -> {
                navController.navigate("onboarding") {
                    popUpTo("splash") { inclusive = true }
                }
            }
            is SplashState.NavigateToLogin -> {
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            }
            is SplashState.NavigateToDashboard -> {
                val route = if (state.role.equals("BRAND", ignoreCase = true)) "brand_home" else "influencer_home"
                navController.navigate(route) {
                    popUpTo("splash") { inclusive = true }
                }
            }
            is SplashState.NavigateToRegistration -> {
                val route = if (state.role.equals("BRAND", ignoreCase = true)) "brand_registration" else "influencer_registration"
                navController.navigate(route) {
                    popUpTo("splash") { inclusive = true }
                }
            }
            is SplashState.Error -> {
                // Handle error (maybe show a dialog or navigate to login as fallback)
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // You would typically use a Logo here. For now using a loader.
        CircularProgressIndicator()
    }
}
