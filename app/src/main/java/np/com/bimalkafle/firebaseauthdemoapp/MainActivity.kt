package np.com.bimalkafle.firebaseauthdemoapp

import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.network.BackendRepository
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.FirebaseAuthDemoAppTheme
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.NotificationViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SplashViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SplashViewModelFactory
import com.razorpay.PaymentResultWithDataListener
import com.razorpay.PaymentData
import com.razorpay.Checkout
import android.widget.Toast

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    private val authViewModel : AuthViewModel by viewModels()
    private val brandViewModel: BrandViewModel by viewModels()
    private val influencerViewModel: InfluencerViewModel by viewModels()
    private val campaignViewModel: CampaignViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()
    private val splashViewModel: SplashViewModel by viewModels { SplashViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Checkout.preload(applicationContext)

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
                // Removed outer Scaffold to prevent double-inset handling
                MyAppNavigation(
                    modifier = Modifier.fillMaxSize(),
                    authViewModel = authViewModel,
                    splashViewModel = splashViewModel,
                    brandViewModel = brandViewModel,
                    influencerViewModel = influencerViewModel,
                    campaignViewModel = campaignViewModel,
                    notificationViewModel = notificationViewModel
                )
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
            
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                user.getIdToken(true).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val idToken = task.result?.token
                        if (idToken != null) {
                            lifecycleScope.launch {
                                val result = BackendRepository.updateFcmToken(idToken, token)
                                result.onSuccess {
                                    Log.d("FCM", "FCM Token updated successfully")
                                }.onFailure {
                                    // Silence NOT_FOUND error as it means user is not yet in the DB
                                    if (it.message?.contains("NOT_FOUND", ignoreCase = true) == false) {
                                        Log.e("FCM", "Failed to update FCM Token", it)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, data: PaymentData?) {
        val paymentId = razorpayPaymentId ?: data?.paymentId
        val signature = data?.signature

        Log.d("Razorpay", "Payment Success: $paymentId, Order: ${data?.orderId}")

        if (paymentId.isNullOrBlank() || signature.isNullOrBlank()) {
            Log.e("Razorpay", "Payment succeeded but paymentId/signature missing from callback data")
            Toast.makeText(this, "Payment completed, but verification data was incomplete. Please contact support with order ${data?.orderId}.", Toast.LENGTH_LONG).show()
            return
        }

        // collaborationId/paymentType travel in the Razorpay order notes (set in RazorpayService).
        val notes = data?.data?.optJSONObject("notes")
        val collaborationId = notes?.optString("collaborationId")
        val paymentType = notes?.optString("paymentType", "FULL")

        if (collaborationId.isNullOrBlank()) {
            Log.e("Razorpay", "Payment succeeded but collaborationId missing from order notes")
            Toast.makeText(this, "Payment completed, but we couldn't link it to a collaboration automatically. Please contact support with payment ID $paymentId.", Toast.LENGTH_LONG).show()
            return
        }

        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val token = result.token
            if (token != null) {
                brandViewModel.verifyPayment(token, collaborationId, paymentId, signature, paymentType ?: "FULL") { success ->
                    if (success) {
                        Toast.makeText(this, "Payment Verified Successfully", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Payment received but verification failed. Please contact support with payment ID $paymentId.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Payment received but we couldn't verify your session. Please reopen the app and contact support with payment ID $paymentId.", Toast.LENGTH_LONG).show()
            }
        }?.addOnFailureListener {
            Toast.makeText(this, "Payment received but verification couldn't start. Please contact support with payment ID $paymentId.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPaymentError(code: Int, description: String?, data: PaymentData?) {
        Log.e("Razorpay", "Payment Error $code: $description")
        Toast.makeText(this, "Payment Failed: $description", Toast.LENGTH_LONG).show()
    }
}
