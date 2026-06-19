package np.com.bimalkafle.firebaseauthdemoapp.viewmodel

/**
 * Stale-while-revalidate guard for ViewModel fetch functions.
 *
 * Screens call their `fetchX()` functions from `LaunchedEffect(Unit)` every time
 * they're navigated to (e.g. switching bottom-nav tabs), which previously meant a
 * fresh network round-trip — and a loading flash — on every single visit, even
 * though the ViewModel already held perfectly good data from moments ago.
 *
 * [shouldFetch] lets a fetch function skip the network call (and therefore the
 * loading-state flip) if the same key was fetched within [ttlMs]. Pass
 * `force = true` (e.g. from a pull-to-refresh gesture) to bypass the cache.
 */
class FetchThrottle(private val ttlMs: Long = 60_000L) {
    private val lastFetchAt = mutableMapOf<String, Long>()

    fun shouldFetch(key: String, force: Boolean = false): Boolean {
        val now = System.currentTimeMillis()
        val last = lastFetchAt[key]
        if (!force && last != null && now - last < ttlMs) {
            return false
        }
        lastFetchAt[key] = now
        return true
    }
}
