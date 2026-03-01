package np.com.bimalkafle.firebaseauthdemoapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.network.BackendRepository
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.FirebaseAuthDemoAppTheme
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SplashViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.SplashViewModelFactory
import org.json.JSONObject

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    private val brandViewModel: BrandViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Checkout.preload(applicationContext)

        val authViewModel: AuthViewModel by viewModels()
        val splashViewModel: SplashViewModel by viewModels { SplashViewModelFactory(this) }
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
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        setContent {
            FirebaseAuthDemoAppTheme {
                MyAppNavigation(
                    modifier = Modifier.fillMaxSize(),
                    authViewModel = authViewModel,
                    splashViewModel = splashViewModel,
                    brandViewModel = brandViewModel,
                    influencerViewModel = influencerViewModel,
                    campaignViewModel = campaignViewModel
                )
            }
        }

        setupFcm()
    }

    private fun setupFcm() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            val token = task.result
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                user.getIdToken(true).addOnCompleteListener { taskToken ->
                    if (taskToken.isSuccessful) {
                        val idToken = taskToken.result?.token
                        if (idToken != null) {
                            lifecycleScope.launch {
                                BackendRepository.updateFcmToken(idToken, token)
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val notes = paymentData?.data?.optJSONObject("notes")
        val collaborationId = notes?.optString("collaborationId")
        val paymentType = notes?.optString("paymentType")
        val signature = paymentData?.signature

        if (!collaborationId.isNullOrEmpty() && !razorpayPaymentId.isNullOrEmpty() && !signature.isNullOrEmpty() && !paymentType.isNullOrEmpty()) {
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
                brandViewModel.verifyPayment(
                    token = result.token!!,
                    collaborationId = collaborationId,
                    razorpayPaymentId = razorpayPaymentId,
                    razorpaySignature = signature,
                    paymentType = paymentType
                ) { success ->
                    if (success) {
                        Toast.makeText(this, "Payment Successful & Verified", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Payment verification failed", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        Log.e("Razorpay", "Payment Error $code: $response")
        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_LONG).show()
    }

    fun startPayment(orderId: String, amount: Int, collaborationId: String, paymentType: String, brandName: String) {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_SDi8IlcjLgcYQE")
        
        // Auto-detect if amount is in Rupees or Paise
        val finalAmount = if (amount < 1000) amount * 100 else amount
        
        Log.d("Razorpay", "Opening Checkout - Order: $orderId, Final Amount: $finalAmount")

        try {
            val options = JSONObject()
            options.put("name", brandName)
            options.put("description", "Payment for Collaboration")
            options.put("order_id", orderId)
            options.put("theme.color", "#FF8383")
            options.put("currency", "INR")
            options.put("amount", finalAmount)

            val notes = JSONObject()
            notes.put("collaborationId", collaborationId)
            notes.put("paymentType", paymentType)
            options.put("notes", notes)

            checkout.open(this, options)
        } catch (e: Exception) {
            Log.e("Razorpay", "Error starting checkout", e)
        }
    }
}
