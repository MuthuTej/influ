package np.com.bimalkafle.firebaseauthdemoapp.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Fires when the Instagram OAuth browser redirect
 * (np.com.bimalkafle.firebaseauthdemoapp://instagram-callback?status=...) lands in
 * MainActivity.onNewIntent, so InfluencerRegistrationScreen can react without any
 * direct reference to the Activity. Mirrors SessionManager's singleton-SharedFlow
 * pattern for the same reason: Compose screens can't receive onNewIntent directly.
 */
object InstagramAuthResult {
    data class Result(val status: String?, val message: String?)

    private val _result = MutableSharedFlow<Result>(extraBufferCapacity = 1)
    val result: SharedFlow<Result> = _result

    fun notifyResult(status: String?, message: String?) {
        _result.tryEmit(Result(status, message))
    }
}
