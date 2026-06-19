package np.com.bimalkafle.firebaseauthdemoapp.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Fires when the backend rejects the current Firebase token (401/403) so the UI layer
 * can sign the user out and route back to login instead of every screen silently
 * failing its own request with no way to recover.
 */
object SessionManager {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired

    fun notifySessionExpired() {
        _sessionExpired.tryEmit(Unit)
    }
}
