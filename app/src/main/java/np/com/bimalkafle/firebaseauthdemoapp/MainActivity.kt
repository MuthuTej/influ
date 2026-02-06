package np.com.bimalkafle.firebaseauthdemoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.FirebaseAuthDemoAppTheme
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SplashViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SplashViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val authViewModel : AuthViewModel by viewModels()
        val splashViewModel: SplashViewModel by viewModels { SplashViewModelFactory(this) }
        
        setContent {
            FirebaseAuthDemoAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MyAppNavigation(
                        modifier = Modifier, // Remove padding to allow content to draw behind system bars
                        authViewModel = authViewModel,
                        splashViewModel = splashViewModel
                    )
                }
            }
        }
    }
}
