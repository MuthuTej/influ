package np.com.bimalkafle.firebaseauthdemoapp.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

/**
 * Pull-to-refresh container built on the Material3 1.2.x primitives
 * (`rememberPullToRefreshState` + `PullToRefreshContainer`), since this project is
 * pinned to material3:1.2.1 and the newer single-composable `PullToRefreshBox`
 * (material3 1.3+) isn't available without a much larger Compose/Kotlin upgrade.
 *
 * Same shape as `PullToRefreshBox`: pass the screen's real loading flag as
 * [isRefreshing] and a [onRefresh] that triggers a force-refetch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberPullToRefreshState()

    // User pulled far enough to trigger a refresh.
    if (state.isRefreshing) {
        LaunchedEffect(state) {
            onRefresh()
        }
    }

    // Dismiss the indicator once the real fetch (driven by isRefreshing) completes.
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            state.endRefresh()
        }
    }

    Box(modifier = modifier.nestedScroll(state.nestedScrollConnection)) {
        content()
        // Only place the indicator in the tree while actually being pulled or
        // refreshing — otherwise material3 1.2.x's container still draws a small
        // idle dot at rest even at zero progress.
        if (state.verticalOffset > 0f || state.isRefreshing) {
            PullToRefreshContainer(
                state = state,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
