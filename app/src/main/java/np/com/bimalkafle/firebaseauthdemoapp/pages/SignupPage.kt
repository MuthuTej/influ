package np.com.bimalkafle.firebaseauthdemoapp.pages

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.utils.PrefsManager

@Composable
fun SignupPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("BRAND") } // Default role

    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val prefsManager = PrefsManager(context)

    // Google Sign In configuration
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { authViewModel.signInWithGoogle(it) }
        } catch (e: ApiException) {
            Toast.makeText(context, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(authState.value) {
        when (val state = authState.value) {
            is AuthState.Authenticated -> {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val isBrand = state.role.equals("BRAND", ignoreCase = true)
                    if (state.isProfileCompleted) {
                        prefsManager.saveProfileCompleted(uid, true)
                        val route = if (isBrand) "brand_home" else "influencer_home"
                        navController.navigate(route) {
                            popUpTo("login") { inclusive = true }
                            popUpTo("signup") { inclusive = true }
                        }
                    } else {
                        // User exists but profile is incomplete
                        val route = if (isBrand) "brand_registration" else "influencer_registration"
                        navController.navigate(route) {
                            popUpTo("login") { inclusive = true }
                            popUpTo("signup") { inclusive = true }
                        }
                    }
                }
            }
            is AuthState.GoogleNewUser -> {
                email = state.email
                // Do NOT auto-fill name from Google, user must fill it manually
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
        targetValue = if (imeDp > 0.dp) 150.dp else 100.dp,
        label = "text-padding-offset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset(y = offset)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
    ) {
        SignupPageContent(
            modifier = Modifier,
            name = name,
            onNameChange = { name = it },
            email = email,
            onEmailChange = { email = it },
            password = password,
            onPasswordChange = { password = it },
            confirmPassword = confirmPassword,
            onConfirmPasswordChange = { confirmPassword = it },
            role = role,
            onRoleChange = { role = it },
            authState = authState.value,
            onSignupClick = { 
                if (name.isBlank()) {
                    Toast.makeText(context, "${if (role == "BRAND") "Brand Name" else "Name"} is mandatory", Toast.LENGTH_SHORT).show()
                    return@SignupPageContent
                }
                if (authState.value is AuthState.GoogleNewUser) {
                    authViewModel.completeBackendSignup(name, role)
                } else {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Email and password are required", Toast.LENGTH_SHORT).show()
                        return@SignupPageContent
                    }
                    authViewModel.signup(email, password, confirmPassword, name, role)
                }
            },
            onGoogleSignupClick = {
                if (name.isBlank()) {
                    Toast.makeText(context, "Please enter ${if (role == "BRAND") "Brand Name" else "Name"} first", Toast.LENGTH_SHORT).show()
                } else {
                    // Force account picker
                    googleSignInClient.signOut().addOnCompleteListener {
                        launcher.launch(googleSignInClient.signInIntent)
                    }
                }
            },
            onLoginClick = { navController.navigate("login") },
            headerTopPadding = topPadding
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupPageContent(
    modifier: Modifier = Modifier,
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    role: String,
    onRoleChange: (String) -> Unit,
    authState: AuthState?,
    onSignupClick: () -> Unit,
    onGoogleSignupClick: () -> Unit,
    onLoginClick: () -> Unit,
    headerTopPadding: Dp
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val roles = listOf("BRAND", "INFLUENCER")
    val isGoogleUser = authState is AuthState.GoogleNewUser

    val themeColor = MaterialTheme.colorScheme.primary

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
                    .fillMaxHeight(0.35f),
                contentScale = ContentScale.FillBounds
            )
            Text(
                text = "Sign up",
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
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray.copy(alpha = 0.2f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                roles.forEach { selectionOption ->
                    Button(
                        onClick = { onRoleChange(selectionOption) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (role == selectionOption) themeColor else Color.Transparent,
                            contentColor = if (role == selectionOption) Color.White else Color.Black
                        )
                    ) {
                        Text(selectionOption.lowercase().replaceFirstChar { it.titlecase() })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (role == "BRAND") "Brand Name *" else "Name *") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor)
            )

            if (!isGoogleUser) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
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
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Confirm Password") },
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onSignupClick,
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
                        text = if (isGoogleUser) "Complete Account" else "Create Account",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (!isGoogleUser) {
                Spacer(modifier = Modifier.height(16.dp))

                // Google Signup Button
                OutlinedButton(
                    onClick = onGoogleSignupClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = buildAnnotatedString {
                    append("Already have an account? ")
                    withStyle(
                        style = SpanStyle(
                            color = themeColor,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Login")
                    }
                },
                fontSize = 14.sp,
                modifier = Modifier.clickable { onLoginClick() }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignupPagePreview() {
    SignupPageContent(
        name = "",
        onNameChange = {},
        email = "",
        onEmailChange = {},
        password = "",
        onPasswordChange = {},
        confirmPassword = "",
        onConfirmPasswordChange = {},
        role = "BRAND",
        onRoleChange = {},
        authState = null,
        onSignupClick = {},
        onGoogleSignupClick = {},
        onLoginClick = {},
        headerTopPadding = 100.dp
    )
}
