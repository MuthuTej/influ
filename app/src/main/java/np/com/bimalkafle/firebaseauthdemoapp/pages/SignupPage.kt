package np.com.bimalkafle.firebaseauthdemoapp.pages

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.utils.PrefsManager

@Composable
fun SignupPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("BRAND") } // Default role

    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val prefsManager = PrefsManager(context)

    LaunchedEffect(authState.value) {
        when (val state = authState.value) {
            is AuthState.Authenticated -> {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    // Start fresh -> Profile NOT completed -> Registration
                    // Even if by some persistent state it was true, we might want to check
                    // But for Signup, we can assume we want to go to registration usually.
                    // Strictly following logic: Check Prefs.
                    if (prefsManager.isProfileCompleted(uid)) {
                         val route = if (state.role.equals("BRAND", ignoreCase = true)) "brand_home" else "influencer_home"
                         navController.navigate(route) {
                            popUpTo("signup") { inclusive = true }
                         }
                    } else {
                        val route = if (state.role.equals("BRAND", ignoreCase = true)) "brand_registration" else "influencer_registration"
                        navController.navigate(route) {
                            popUpTo("signup") { inclusive = true }
                        }
                    }
                }
            }
            is AuthState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Signup Page", fontSize = 32.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(text = "Full Name") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(text = "Email") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(text = "Password") }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Simple Role Selection
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = role == "BRAND",
                onClick = { role = "BRAND" }
            )
            Text("Brand")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = role == "INFLUENCER",
                onClick = { role = "INFLUENCER" }
            )
            Text("Influencer")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                authViewModel.signup(email, password, name, role)
            },
            enabled = authState.value != AuthState.Loading
        ) {
            Text(text = "Signup")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = { navController.navigate("login") }) {
            Text(text = "Already have an account, Login")
        }
    }
}