package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import np.com.bimalkafle.firebaseauthdemoapp.R
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.imePadding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.network.BackendRepository
import org.json.JSONArray
import org.json.JSONObject
import np.com.bimalkafle.firebaseauthdemoapp.utils.PrefsManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InfluencerRegistrationScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()
    val prefsManager = remember { PrefsManager(context) }

    val deliverables = listOf("Reels/Shorts", "Story", "Post", "Video")
    val selectedDeliverables = remember { mutableStateListOf<String>() }

    val categoryOptions = listOf("Fashion", "Tech", "Lifestyle", "Gaming", "Food", "Beauty", "Travel")
    val selectedCategories = remember { mutableStateListOf<String>() }

    val platforms = listOf("Instagram", "Facebook", "YouTube")
    val selectedPlatforms = remember { mutableStateListOf<String>() }

    var profileUrls by remember { mutableStateOf(mapOf<String, String>()) }
    var pricing by remember { mutableStateOf(mapOf<String, Map<String, String>>()) }

    var isYouTubeConnecting by remember { mutableStateOf(false) }
    var isYouTubeConnected by remember { mutableStateOf(false) }
    var youtubeAuthCode by remember { mutableStateOf<String?>(null) }

    val youtubeAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val authCode = account?.serverAuthCode
                if (authCode != null) {
                    youtubeAuthCode = authCode
                    isYouTubeConnected = true
                    Toast.makeText(context, "YouTube Connected!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Auth code missing", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                val statusCode = e.statusCode
                val errorMsg = GoogleSignInStatusCodes.getStatusCodeString(statusCode)
                Toast.makeText(context, "Error $statusCode: $errorMsg", Toast.LENGTH_LONG).show()
                Log.e("GOOGLE_SIGN_IN", "Status: $statusCode, Msg: $errorMsg", e)
            }
        } else {
            Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
        }
        isYouTubeConnecting = false
    }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val headerHeight = screenHeight * 0.4f
    val formPaddingTop = headerHeight - 80.dp

    Box(modifier = Modifier.fillMaxSize().imePadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(headerHeight).background(Color(0xFFFF8383))) {
            Image(painter = painterResource(id = R.drawable.vector), contentDescription = null, modifier = Modifier.fillMaxSize().alpha(0.2f), contentScale = ContentScale.Crop)
            IconButton(onClick = { FirebaseAuth.getInstance().signOut(); navController.popBackStack() }, modifier = Modifier.statusBarsPadding().padding(16.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painter = painterResource(id = R.drawable.brand_profile), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Creator Profile Setup", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.padding(top = formPaddingTop).fillMaxSize().padding(horizontal = 16.dp).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(Color.White).verticalScroll(rememberScrollState()).padding(24.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Creator Name") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, trailingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = logoUrl, onValueChange = { logoUrl = it }, label = { Text("Logo URL") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Platforms", fontWeight = FontWeight.Bold)
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Instagram", "Facebook", "YouTube").forEach { plat ->
                    FilterChip(selected = selectedPlatforms.contains(plat), onClick = { if (selectedPlatforms.contains(plat)) selectedPlatforms.remove(plat) else selectedPlatforms.add(plat) }, label = { Text(plat) })
                }
            }

            if (selectedPlatforms.contains("YouTube")) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        isYouTubeConnecting = true
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestEmail()
                            .requestServerAuthCode("61884125308-s3uiiss031jvqaje7thsu58027dckp8b.apps.googleusercontent.com")
                            .requestScopes(Scope("https://www.googleapis.com/auth/youtube.readonly"), Scope("https://www.googleapis.com/auth/youtube.analytics.readonly"))
                            .build()
                        val client = GoogleSignIn.getClient(context, gso)
                        client.signOut().addOnCompleteListener { youtubeAuthLauncher.launch(client.signInIntent) }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !isYouTubeConnected,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isYouTubeConnected) Color(0xFF4CAF50) else Color.Red)
                ) {
                    if (isYouTubeConnecting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    else Text(if (isYouTubeConnected) "YouTube Linked ✓" else "Link YouTube Channel")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { /* Submit Logic */ },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8383))
            ) {
                Text("FINISH SETUP", fontWeight = FontWeight.Bold)
            }
        }
    }
}
