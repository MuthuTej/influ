package np.com.bimalkafle.firebaseauthdemoapp.pages

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var email by remember { mutableStateOf("") }
    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val themeColor = Color(0xFFFF8383)

    LaunchedEffect(authState.value) {
        if (authState.value is AuthState.PasswordResetSent) {
            Toast.makeText(context, "Reset link sent to your email", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        } else if (authState.value is AuthState.Error) {
            Toast.makeText(context, (authState.value as AuthState.Error).message, Toast.LENGTH_SHORT).show()
        }
    }

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
            Row(
                modifier = Modifier
                    .padding(start = 16.dp, top = 48.dp)
                    .clickable { navController.popBackStack() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "Forgot Password",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(start = 32.dp, top = 120.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Enter your email address and we will send you a link to reset your password.",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email Address") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { authViewModel.forgotPassword(email) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                enabled = authState.value != AuthState.Loading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (authState.value == AuthState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Send Reset Link",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Back to Login",
                color = themeColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { navController.popBackStack() }
            )
        }
    }
}
