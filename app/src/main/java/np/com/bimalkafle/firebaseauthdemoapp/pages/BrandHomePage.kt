package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel

@Composable
fun BrandHomePage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    val authState = authViewModel.authState.observeAsState()

    LaunchedEffect(authState.value) {
        if (authState.value is AuthState.Unauthenticated) {
            navController.navigate("login") {
                popUpTo("brand_home") { inclusive = true }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Brand Dashboard", fontSize = 32.sp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = "Welcome, Brand!", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            authViewModel.signout()
        }) {
            Text(text = "Sign Out")
        }
    }
}
