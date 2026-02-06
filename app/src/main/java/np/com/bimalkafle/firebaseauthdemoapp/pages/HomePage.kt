package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.FirebaseAuthDemoAppTheme

@Composable
fun HomePage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {

    val authState = authViewModel.authState.observeAsState()

    LaunchedEffect(authState.value) {
        when (val state = authState.value) {
            is AuthState.Authenticated -> {
                if (state.role == "BRAND") {
                    navController.navigate("brand_home") {
                        popUpTo("home") { inclusive = true }
                    }
                } else if (state.role == "INFLUENCER") {
                    navController.navigate("influencer_home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            }
            is AuthState.Unauthenticated -> navController.navigate("login")
            else -> Unit
        }
    }

    HomePageContent(modifier = modifier) {
        authViewModel.signout()
    }

}

@Composable
fun HomePageContent(modifier: Modifier = Modifier, onSignOut: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Home Page", fontSize = 32.sp)

        TextButton(onClick = onSignOut) {
            Text(text = "Sign out")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePagePreview() {
    FirebaseAuthDemoAppTheme {
        HomePageContent(onSignOut = {})
    }
}