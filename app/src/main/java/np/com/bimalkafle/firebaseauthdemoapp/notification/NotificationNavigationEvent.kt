package np.com.bimalkafle.firebaseauthdemoapp.notification

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Fires when a push notification tap (MainActivity.onNewIntent/onCreate) or an in-app
 * notification click resolves to a route, so MyAppNavigation can navigate without any
 * direct reference to the Activity or the click site. Mirrors InstagramAuthResult's
 * singleton-SharedFlow pattern for the same reason: Compose screens can't receive
 * onNewIntent directly.
 */
object NotificationNavigationEvent {
    private val _route = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val route: SharedFlow<String> = _route

    fun notify(route: String) {
        _route.tryEmit(route)
    }
}
