package np.com.bimalkafle.firebaseauthdemoapp.pages

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.utils.PrefsManager

@Composable
fun LoginPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val prefsManager = PrefsManager(context)

    LaunchedEffect(authState.value) {
        when (val state = authState.value) {
            is AuthState.Authenticated -> {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    if (prefsManager.isProfileCompleted(uid)) {
                        // Profile completed -> Dashboard
                        val route = if (state.role.equals("BRAND", ignoreCase = true)) "brand_home" else "influencer_home"
                        navController.navigate(route) {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        // Profile NOT completed -> Registration
                        val route = if (state.role.equals("BRAND", ignoreCase = true)) "brand_registration" else "influencer_registration"
                        navController.navigate(route) {
                            popUpTo("login") { inclusive = true }
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

    val gradientBg = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF37B6E9),
            Color(0xFF4B4CED)
        )
    )

    // Root Container with Gradient
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBg)
            .imePadding() // Keyboard avoiding handling
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp, bottom = 40.dp)
            ) {
                Text(
                    text = "Log In",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Please sign in to your existing account",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            // White Card Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Login Button with Gradient
                    Button(
                        onClick = { authViewModel.login(email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(),
                        enabled = authState.value != AuthState.Loading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = if (authState.value != AuthState.Loading) {
                                        Brush.horizontalGradient(listOf(Color(0xFF37B6E9), Color(0xFF4B4CED)))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color.Gray, Color.Gray)) // Disabled state visual
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                           if (authState.value == AuthState.Loading) {
                               CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                           } else {
                               Text(
                                   text = "LOG IN",
                                   color = Color.White,
                                   fontSize = 18.sp,
                                   fontWeight = FontWeight.Bold
                               )
                           }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sign up link
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append("Don't have an account? ")
                                withStyle(
                                    style = SpanStyle(
                                        color = Color(0xFF4B4CED),
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append("Sign Up")
                                }
                            },
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { navController.navigate("signup") }
                        )
                    }
                }
            }
        }
    }
}
