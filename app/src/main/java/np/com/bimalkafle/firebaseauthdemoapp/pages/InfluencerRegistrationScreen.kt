package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.FirebaseAuthDemoAppTheme
import np.com.bimalkafle.firebaseauthdemoapp.utils.PrefsManager

@Composable
fun InfluencerRegistrationScreen(navController: NavController) {
    var handle by remember { mutableStateOf("") }
    var niche by remember { mutableStateOf("") }
    var followersCount by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val prefsManager = PrefsManager(context)
    val auth = FirebaseAuth.getInstance()
    
    InfluencerRegistrationScreenContent(
        handle = handle,
        niche = niche,
        followersCount = followersCount,
        onHandleChange = { handle = it },
        onNicheChange = { niche = it },
        onFollowersCountChange = { followersCount = it },
        onCompleteProfileClick = {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                // In a real app, you would send this data to the backend here.
                // For this task, we assume local success and save state.
                prefsManager.saveProfileCompleted(uid, true)
                navController.navigate("influencer_home") {
                    popUpTo("influencer_registration") { inclusive = true }
                }
            }
        }
    )
}

@Composable
fun InfluencerRegistrationScreenContent(
    handle: String,
    niche: String,
    followersCount: String,
    onHandleChange: (String) -> Unit,
    onNicheChange: (String) -> Unit,
    onFollowersCountChange: (String) -> Unit,
    onCompleteProfileClick: () -> Unit
) {
    val gradientBg = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF37B6E9),
            Color(0xFF4B4CED)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBg)
            .imePadding()
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
                    text = "Influencer Profile",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tell us about your audience",
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
                        value = handle,
                        onValueChange = onHandleChange,
                        label = { Text("Social Media Handle") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = niche,
                        onValueChange = onNicheChange,
                        label = { Text("Niche (e.g. Travel, Tech)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = followersCount,
                        onValueChange = onFollowersCountChange,
                        label = { Text("Appx. Follower Count") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onCompleteProfileClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(),
                        enabled = handle.isNotEmpty() && niche.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = if (handle.isNotEmpty() && niche.isNotEmpty()) {
                                        Brush.horizontalGradient(listOf(Color(0xFF37B6E9), Color(0xFF4B4CED)))
                                    } else {
                                        Brush.horizontalGradient(listOf(Color.Gray, Color.Gray))
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                             Text(
                               text = "COMPLETE PROFILE",
                               color = Color.White,
                               fontSize = 18.sp,
                               fontWeight = FontWeight.Bold
                           )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InfluencerRegistrationScreenPreview() {
    FirebaseAuthDemoAppTheme {
        InfluencerRegistrationScreenContent(
            handle = "my_handle",
            niche = "tech",
            followersCount = "100k",
            onHandleChange = {},
            onNicheChange = {},
            onFollowersCountChange = {},
            onCompleteProfileClick = {}
        )
    }
}
