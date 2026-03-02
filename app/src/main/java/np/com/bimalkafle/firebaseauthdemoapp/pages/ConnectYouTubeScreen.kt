package np.com.bimalkafle.firebaseauthdemoapp.pages

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.YouTubeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectYouTubeScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    youtubeViewModel: YouTubeViewModel = viewModel()
) {
    val context = LocalContext.current
    val channelData by youtubeViewModel.channelData.observeAsState()
    val isLoading by youtubeViewModel.loading.observeAsState(initial = false)
    val error by youtubeViewModel.error.observeAsState()
    val success by youtubeViewModel.success.observeAsState(initial = false)

    var showSuccessDialog by remember { mutableStateOf(false) }

    // Google Sign-In Launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val serverAuthCode = account.serverAuthCode
                if (serverAuthCode != null) {
                    // Get Firebase token and call connectYouTube mutation
                    FirebaseAuth.getInstance().currentUser?.getIdToken(true)
                        ?.addOnSuccessListener { tokenResult ->
                            val firebaseToken = tokenResult.token
                            if (firebaseToken != null) {
                                youtubeViewModel.connectYouTube(
                                    authCode = serverAuthCode,
                                    firebaseToken = firebaseToken
                                )
                            } else {
                                Toast.makeText(
                                    context,
                                    "Failed to get Firebase token",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        ?.addOnFailureListener {
                            Toast.makeText(
                                context,
                                "Error: ${it.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    Toast.makeText(
                        context,
                        "No server auth code received",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: ApiException) {
            Toast.makeText(
                context,
                "Sign-in failed: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(success) {
        if (success && channelData != null) {
            showSuccessDialog = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Connect YouTube", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                LoadingState()
            } else if (error != null) {
                ErrorState(
                    message = error ?: "Unknown error",
                    onRetry = {
                        youtubeViewModel.clearError()
                        val gso = youtubeViewModel.initializeGoogleSignIn(context)
                        val signInIntent = gso.signInIntent
                        signInLauncher.launch(signInIntent)
                    }
                )
            } else if (channelData != null && showSuccessDialog) {
                SuccessState(
                    channelData = channelData!!,
                    onClose = {
                        youtubeViewModel.resetState()
                        navController.popBackStack()
                    }
                )
            } else {
                InitialState(
                    onConnectClick = {
                        val googleSignInClient = youtubeViewModel.initializeGoogleSignIn(context)
                        val signInIntent = googleSignInClient.signInIntent
                        signInLauncher.launch(signInIntent)
                    }
                )
            }
        }
    }
}

@Composable
private fun InitialState(onConnectClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_dialog_info),
            contentDescription = "YouTube",
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFFF0000)
        )

        Text(
            text = "Connect Your YouTube Channel",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = "Allow us to access your YouTube channel data including subscribers, views, and video information. This helps us provide better insights and analytics.",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Button(
            onClick = onConnectClick,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF0000)
            )
        ) {
            Text(
                text = "Connect with Google",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Text(
            text = "You can disconnect at any time from your profile settings.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(color = Color(0xFFFF0000))
        Text(
            text = "Connecting to YouTube...",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_dialog_alert),
            contentDescription = "Error",
            modifier = Modifier.size(80.dp),
            tint = Color.Red
        )

        Text(
            text = "Connection Failed",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Red
        )

        Text(
            text = message,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF0000)
            )
        ) {
            Text(
                text = "Try Again",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SuccessState(
    channelData: np.com.bimalkafle.firebaseauthdemoapp.model.YouTubeChannelData,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_dialog_info),
            contentDescription = "Success",
            modifier = Modifier.size(80.dp),
            tint = Color(0xFF4CAF50)
        )

        Text(
            text = "Connected Successfully!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )

        // Channel Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoRow(label = "Channel", value = channelData.title)
                InfoRow(
                    label = "Subscribers",
                    value = "${channelData.subscriberCount.toFormattedString()}"
                )
                InfoRow(
                    label = "Total Videos",
                    value = "${channelData.videoCount}"
                )
                InfoRow(
                    label = "Total Views",
                    value = "${(channelData.totalVideoViews ?: channelData.viewCount).toFormattedString()}"
                )
                if (!channelData.country.isNullOrEmpty()) {
                    InfoRow(label = "Country", value = channelData.country)
                }
            }
        }

        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            Text(
                text = "Done",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

private fun Int.toFormattedString(): String {
    return when {
        this >= 1_000_000 -> String.format("%.1fM", this / 1_000_000.0)
        this >= 1_000 -> String.format("%.1fK", this / 1_000.0)
        else -> this.toString()
    }
}
