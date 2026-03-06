package np.com.bimalkafle.firebaseauthdemoapp.utils

import android.app.Activity
import com.razorpay.Checkout
import org.json.JSONObject

object RazorpayService {
    fun startPayment(
        activity: Activity,
        orderData: JSONObject,
        userEmail: String?,
        userContact: String?
    ) {
        val checkout = Checkout()
        // [FIXME] Hardcoded test key. This MUST match RAZORPAY_KEY_ID in backend .env
        checkout.setKeyID("rzp_test_SDi8IlcjLgcYQE") 

        try {
            val options = JSONObject()
            options.put("name", "Connect Platform")
            options.put("description", "Collaboration Payment")
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
            options.put("order_id", orderData.getString("razorpayOrderId"))
            options.put("theme.color", "#FF8383")
            options.put("currency", "INR")
            options.put("amount", (orderData.getDouble("totalAmount") * 100).toInt().toString())
            
            val prefill = JSONObject()
            prefill.put("email", userEmail ?: "")
            prefill.put("contact", userContact ?: "")
            options.put("prefill", prefill)

            val notes = JSONObject()
            notes.put("collaborationId", orderData.getString("collaborationId"))
            notes.put("paymentType", orderData.optString("paymentType", "FULL"))
            options.put("notes", notes)

            val retryObj = JSONObject()
            retryObj.put("enabled", true)
            retryObj.put("max_count", 4)
            options.put("retry", retryObj)

            checkout.open(activity, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
