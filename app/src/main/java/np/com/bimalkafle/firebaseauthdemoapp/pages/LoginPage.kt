package np.com.bimalkafle.firebaseauthdemoapp.pages

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.utils.PrefsManager

@Composable
fun LoginPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val prefsManager = PrefsManager(context)

    LaunchedEffect(authState.value) {
        when (val state = authState.value) {
            is AuthState.Authenticated -> {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val isBrand = state.role.equals("BRAND", ignoreCase = true)
                    val isProfileCompleted =
                        if (state.isProfileCompleted) {
                            prefsManager.saveProfileCompleted(uid, true)
                            true
                        } else {
                            false
                        }


                    if (isProfileCompleted) {
                        val route = if (isBrand) "brand_home" else "influencer_home"
                        navController.navigate(route) {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        val route = if (isBrand) "brand_registration" else "influencer_registration"
                        navController.navigate(route)
                    }
                }
            }
            is AuthState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val imeDp = with(density) { imeBottom.toDp() }

    val offset by animateDpAsState(
        targetValue = if (imeDp > 0.dp) (-imeDp * 0.35f) else 0.dp,
        label = "keyboard-offset"
    )
    val topPadding by animateDpAsState(
        targetValue = if (imeDp > 0.dp) 190.dp else 140.dp,
        label = "text-padding-offset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset(y = offset)
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        LoginPageContent(
            authState = authState.value,
            onLoginClicked = { email, password ->
                authViewModel.login(email, password)
            },
            onSignUpClicked = {
                navController.navigate("signup")
            },
            modifier = Modifier,
            headerTopPadding = topPadding
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginPageContent(
    authState: AuthState?,
    onLoginClicked: (String, String) -> Unit,
    onSignUpClicked: () -> Unit,
    modifier: Modifier = Modifier,
    headerTopPadding: Dp
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    val themeColor = Color(0xFFFF8383)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.vector3),
                contentDescription = "Header background",
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f),
                contentScale = ContentScale.FillBounds
            )
            Text(
                text = "Sign in",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 32.dp, top = headerTopPadding)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                    Text("Remember Me")
                }
                Text("Forgot Password?", color = themeColor, modifier = Modifier.clickable { /*TODO*/ })
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onLoginClicked(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                enabled = authState != AuthState.Loading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (authState == AuthState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Login",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = buildAnnotatedString {
                    append("Don't have an Account? ")
                    withStyle(
                        style = SpanStyle(
                            color = themeColor,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Sign up")
                    }
                },
                fontSize = 14.sp,
                modifier = Modifier.clickable { onSignUpClicked() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPagePreview() {
    LoginPageContent(
        authState = null,
        onLoginClicked = { _, _ -> },
        onSignUpClicked = {},
        headerTopPadding = 140.dp
    )
}
