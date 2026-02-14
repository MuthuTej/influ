package np.com.bimalkafle.firebaseauthdemoapp

import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.FirebaseAuthDemoAppTheme
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SplashViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SplashViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val authViewModel : AuthViewModel by viewModels()
        val splashViewModel: SplashViewModel by viewModels { SplashViewModelFactory(this) }
        val brandViewModel: BrandViewModel by viewModels()
        val influencerViewModel: InfluencerViewModel by viewModels()
        val campaignViewModel: CampaignViewModel by viewModels()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf<String>(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )

            }
        }
        setContent {
            FirebaseAuthDemoAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MyAppNavigation(
                        modifier = Modifier, // Remove padding to allow content to draw behind system bars
                        authViewModel = authViewModel,
                        splashViewModel = splashViewModel,
                        brandViewModel = brandViewModel,
                        influencerViewModel = influencerViewModel,
                        campaignViewModel = campaignViewModel
                    )
                }
            }
        }
        
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            Log.d("FCM", token)
        })
    }
}
