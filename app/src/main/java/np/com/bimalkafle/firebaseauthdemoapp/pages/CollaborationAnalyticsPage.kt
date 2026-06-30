package np.com.bimalkafle.firebaseauthdemoapp.pages

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import np.com.bimalkafle.firebaseauthdemoapp.AuthState
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.components.EmptyState
import np.com.bimalkafle.firebaseauthdemoapp.components.LoadingState
import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration
import np.com.bimalkafle.firebaseauthdemoapp.model.CollaborationAnalytics
import np.com.bimalkafle.firebaseauthdemoapp.model.InstagramPostData
import np.com.bimalkafle.firebaseauthdemoapp.model.PerformanceAchievement
import np.com.bimalkafle.firebaseauthdemoapp.model.PerformanceMilestone
import np.com.bimalkafle.firebaseauthdemoapp.model.PerformanceSnapshot
import np.com.bimalkafle.firebaseauthdemoapp.model.PerformanceTracking
import np.com.bimalkafle.firebaseauthdemoapp.model.YouTubeVideoData
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.InfluencerViewModel

// App theme colors
private val themeColor_campaign: Color
    @Composable get() = MaterialTheme.colorScheme.primary
private val textGray = Color(0xFF8E8E93)
private val softGray = Color(0xFFF8F9FA)

@Composable
fun CollaborationAnalyticsPage(
    navController: NavController,
    collaborationId: String,
    brandViewModel: BrandViewModel,
    influencerViewModel: InfluencerViewModel? = null,
    authViewModel: AuthViewModel? = null
) {
    val brandCollaborations by brandViewModel.collaborations.observeAsState(emptyList())
    val influencerCollaborations by (influencerViewModel?.collaborations?.observeAsState(emptyList()) ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList()) })
    val isLoading by brandViewModel.loading.observeAsState(initial = false)

    // Campaign targets are brand-only: the backend never returns
    // performanceTargets/performanceTracking for an influencer-authenticated
    // request, but the UI must also avoid even attempting to render that
    // section when this screen is opened from the influencer side.
    // influencerViewModel is a shared singleton passed to every route regardless
    // of the logged-in role (see MyAppNavigation.kt), so it's always non-null —
    // it can't be used as the role signal. authState.role (the same source
    // HomePage.kt uses to pick Brand/Influencer home screens) is the real one.
    val authState by (authViewModel?.authState?.observeAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(null) })
    val isBrandView = (authState as? AuthState.Authenticated)?.role == "BRAND"
    var showTargetsDialog by remember { mutableStateOf(false) }

    // Prefer the collaboration from whichever ViewModel already has analytics data.
    // The influencer VM may already have it loaded (from ProposalPage) with yt/ig
    // fields populated. Brand VM is the authoritative fallback fetched on open.
    val collaboration = run {
        val fromBrand = brandCollaborations.find { it.id == collaborationId }
        val fromInfluencer = influencerCollaborations.find { it.id == collaborationId }
        when {
            fromBrand != null && (!fromBrand.yt.isNullOrEmpty() || fromBrand.overallAnalytics != null) -> fromBrand
            fromInfluencer != null && (!fromInfluencer.yt.isNullOrEmpty() || fromInfluencer.overallAnalytics != null) -> fromInfluencer
            fromBrand != null -> fromBrand
            else -> fromInfluencer
        }
    }

    val hasAnalyticsData = collaboration != null &&
        (collaboration.overallAnalytics != null ||
         !collaboration.yt.isNullOrEmpty() ||
         !collaboration.ig.isNullOrEmpty() ||
         !collaboration.platformAnalytics.isNullOrEmpty())

    // Tracks how many auto-retries have fired; capped at 3 before giving up.
    var syncRetries by remember(collaborationId) { androidx.compose.runtime.mutableStateOf(0) }
    var syncTimedOut by remember(collaborationId) { androidx.compose.runtime.mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    fun fetchLatest() {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            result.token?.let { token -> brandViewModel.fetchCollaborations(token, force = true) }
        }
    }

    // Calls the backend to read UPLOAD chat messages and retroactively populate yt
    fun refreshAnalytics() {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val token = result.token ?: return@addOnSuccessListener
            scope.launch {
                np.com.bimalkafle.firebaseauthdemoapp.network.BackendRepository
                    .refreshCollaborationAnalytics(collaborationId, token)
                kotlinx.coroutines.delay(2000)
                brandViewModel.fetchCollaborations(token, force = true)
            }
        }
    }

    LaunchedEffect(Unit) { fetchLatest() }

    // If COMPLETED but no analytics data yet, retry twice (every 3 s), then on the
    // third attempt call refreshCollaborationAnalytics which retroactively reads the
    // UPLOAD chat messages and populates yt — handles existing collaborations where
    // the influencer didn't have YouTube OAuth when they originally uploaded.
    LaunchedEffect(collaboration?.status, hasAnalyticsData, syncRetries) {
        if (collaboration?.status == "COMPLETED" && !hasAnalyticsData && !syncTimedOut) {
            when {
                syncRetries < 2 -> {
                    kotlinx.coroutines.delay(3000)
                    fetchLatest()
                    syncRetries++
                }
                syncRetries == 2 -> {
                    kotlinx.coroutines.delay(2000)
                    refreshAnalytics()
                    syncRetries++
                }
                else -> syncTimedOut = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FE))
    ) {
        if (isLoading) {
            LoadingState(modifier = Modifier.align(Alignment.Center), message = "Loading analytics…")
        } else if (collaboration == null) {
            EmptyState(
                modifier = Modifier.align(Alignment.Center),
                icon = Icons.Default.BarChart,
                title = "Collaboration not found",
                subtitle = "This collaboration may have been removed."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AnalyticsHeader(navController, collaboration)
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        InfluencerProfileCard(collaboration)
                    }
                }

                if (isBrandView) {
                    item {
                        BrandTargetsSection(
                            collaboration = collaboration,
                            onSetTargets = { showTargetsDialog = true }
                        )
                    }
                }

                if (collaboration.status != "COMPLETED") {
                    val statusMessage = when (collaboration.status) {
                        "PENDING" -> "Collaboration proposal is pending"
                        "ACCEPTED" -> "Collaboration is accepted, waiting for next steps"
                        "NEGOTIATION" -> "Collaboration is currently under negotiation"
                        "BRIEF_FINALIZED" -> "Campaign brief is finalized, moving to production"
                        "WAITING_FOR_PAYMENT" -> "Waiting for payment to be processed"
                        "IN_PROGRESS" -> "Collaboration is currently in progress"
                        "REVOKED" -> "Collaboration has been revoked"
                        "REJECTED" -> "Collaboration proposal was rejected"
                        else -> "Collaboration is in ${collaboration.status.replace("_", " ").lowercase()} status"
                    }
                    val statusIcon = when (collaboration.status) {
                        "PENDING" -> Icons.Default.HourglassEmpty
                        "ACCEPTED" -> Icons.Default.CheckCircle
                        "NEGOTIATION" -> Icons.Default.Gavel
                        "BRIEF_FINALIZED" -> Icons.Default.Assignment
                        "WAITING_FOR_PAYMENT" -> Icons.Default.Payments
                        "IN_PROGRESS" -> Icons.Default.WorkOutline
                        "REVOKED" -> Icons.Default.Block
                        "REJECTED" -> Icons.Default.Cancel
                        else -> Icons.Default.Info
                    }
                    item { StatusPlaceholder(statusMessage, statusIcon) }
                } else if (!hasAnalyticsData) {
                    // COMPLETED but analytics data not ready yet (or unavailable)
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!syncTimedOut) {
                                CircularProgressIndicator(color = themeColor_campaign)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Syncing analytics…",
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Video data is being saved. This usually takes a few seconds.",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Icon(
                                    Icons.Default.BarChart,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.LightGray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No analytics data available",
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Analytics are collected after the content is verified. Try refreshing after a few minutes.",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    syncRetries = 0
                                    syncTimedOut = false
                                    refreshAnalytics()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor_campaign)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Refresh")
                            }
                        }
                    }
                } else {
                    // Overall Performance Header
                    if (collaboration.overallAnalytics != null) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                SectionTitle("OVERALL PERFORMANCE")
                            }
                        }

                        item {
                            val duration = collaboration.platformAnalytics?.firstOrNull()?.duration ?: 0
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                TotalImpressionsCard(
                                    impressions = String.format("%,d", collaboration.overallAnalytics.impressions ?: 0),
                                    subtext = "Reached across $duration days"
                                )
                            }
                        }

                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                OverallStatsGrid(collaboration)
                            }
                        }

                        item {
                            collaboration.overallAnalytics.let { stats ->
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    EngagementBreakdownCard(
                                        likes = stats.likes ?: 0,
                                        comments = stats.comments ?: 0,
                                        shares = stats.shares ?: 0,
                                        saves = stats.saves ?: 0,
                                        title = "Engagement Breakdown"
                                    )
                                }
                            }
                        }
                    }

                    // YouTube Video Analytics Section
                    if (collaboration.yt != null && collaboration.yt.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                SectionTitle("YOUTUBE VIDEO PERFORMANCE")
                                Spacer(modifier = Modifier.height(8.dp))
                                collaboration.yt.forEach { video ->
                                    YouTubeVideoCard(video)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }

                    // Performance Timeline (milestones)
                    if (!collaboration.performanceMilestones.isNullOrEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                SectionTitle("PERFORMANCE TIMELINE")
                                Spacer(modifier = Modifier.height(8.dp))
                                PerformanceTimelineCard(collaboration.performanceMilestones)
                            }
                        }
                    }

                    // Instagram Post Analytics Section
                    if (collaboration.ig != null && collaboration.ig.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                SectionTitle("INSTAGRAM PERFORMANCE")
                                Spacer(modifier = Modifier.height(8.dp))
                                collaboration.ig.forEach { post ->
                                    InstagramPostCard(post)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }

                    // Platform Breakdown Section
                    if (collaboration.platformAnalytics != null && collaboration.platformAnalytics.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                SectionTitle("PLATFORM BREAKDOWN")
                            }
                        }

                        items(collaboration.platformAnalytics) { analytics ->
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                PlatformMetricCard(analytics, expandedDefault = collaboration.platformAnalytics.size == 1)
                            }
                        }

                        item {
                            val durationValue = collaboration.platformAnalytics.firstOrNull()?.duration ?: 0
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                CampaignDurationCard(durationValue.toString())
                            }
                        }
                    }
                }
            }
        }

        if (isBrandView && showTargetsDialog && collaboration != null) {
            SetTargetsDialog(
                collaboration = collaboration,
                brandViewModel = brandViewModel,
                onDismiss = { showTargetsDialog = false }
            )
        }
    }
}

@Composable
fun InstagramPostCard(post: InstagramPostData) {
    val context = LocalContext.current
    val igColor = Color(0xFFC13584)
    val caption = post.caption ?: ""
    val hashtags = remember(caption) {
        Regex("#\\w+").findAll(caption).map { it.value }.toList()
    }
    val captionWithoutTags = remember(caption) {
        caption.replace(Regex("#\\w+"), "").trim()
    }
    val postDate = post.timestamp?.split("T")?.get(0) ?: "N/A"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row: thumbnail + metadata
            Row(verticalAlignment = Alignment.Top) {
                Box {
                    AsyncImage(
                        model = post.mediaUrl,
                        contentDescription = "Instagram Post",
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.instagram_logo)
                    )
                    Surface(
                        color = igColor,
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(4.dp).size(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Content type badge
                    ContentTypeBadge(
                        label = "Instagram Post",
                        color = igColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = captionWithoutTags.ifBlank { "No caption" },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Posted $postDate",
                        fontSize = 11.sp,
                        color = textGray
                    )
                }
            }

            // Hashtags row
            if (hashtags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    hashtags.take(6).forEach { tag ->
                        HashtagChip(tag, igColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = softGray)
            Spacer(modifier = Modifier.height(12.dp))

            // Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VideoStatItem("Likes", String.format("%,d", post.likeCount ?: 0))
                VideoStatItem("Comments", String.format("%,d", post.commentCount ?: 0))
                VideoStatItem("Views", String.format("%,d", post.viewCount ?: 0))
            }

            // Open on Instagram button
            if (!post.postId.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/p/${post.postId}/"))
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = igColor)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open on Instagram", fontSize = 13.sp)
                }
            }
        }
    }
}

private fun parseDuration(iso: String?): String? {
    iso ?: return null
    val hours = Regex("(\\d+)H").find(iso)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val mins  = Regex("(\\d+)M").find(iso)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val secs  = Regex("(\\d+)S").find(iso)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    return if (hours > 0) "%d:%02d:%02d".format(hours, mins, secs)
           else "%d:%02d".format(mins, secs)
}

private fun formatCount(raw: String?): String {
    val n = raw?.toLongOrNull() ?: return raw ?: "—"
    return when {
        n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
        n >= 1_000     -> "%.1fK".format(n / 1_000.0)
        else           -> n.toString()
    }
}

private fun formatDate(iso: String?): String {
    iso ?: return "N/A"
    return iso.split("T").firstOrNull() ?: iso
}

@Composable
fun YouTubeVideoCard(video: YouTubeVideoData) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val ytRed = Color(0xFFFF0000)

    val isShort = video.duration?.let {
        val mins = Regex("(\\d+)M").find(it)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val hrs  = Regex("(\\d+)H").find(it)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        hrs == 0 && mins < 1
    } ?: false

    val contentTypeLabel = if (isShort) "YouTube Short" else "YouTube Video"
    val durationStr = parseDuration(video.duration)

    var playerVisible by remember { mutableStateOf(false) }
    var playerReady   by remember { mutableStateOf(false) }
    var playerError   by remember { mutableStateOf(false) }
    var descExpanded  by remember { mutableStateOf(false) }

    val videoUrl = video.videoUrl?.takeIf { it.isNotBlank() }
        ?: "https://www.youtube.com/watch?v=${video.videoId}"

    val playerViewRef = remember { mutableStateOf<YouTubePlayerView?>(null) }
    DisposableEffect(lifecycleOwner) {
        onDispose {
            playerViewRef.value?.let { view ->
                lifecycleOwner.lifecycle.removeObserver(view)
                view.release()
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // ── Video area: thumbnail → WebView player on tap ──
            if (video.videoId.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    when {
                        playerError -> {
                            // Video embedding is disabled by owner — offer YouTube app fallback
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        "Embedding disabled for this video",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = {
                                            val uri = Uri.parse("https://www.youtube.com/watch?v=${video.videoId}")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ytRed),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Open in YouTube", color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        playerVisible -> {
                            // Player + optional loading overlay
                            AndroidView(
                                factory = { ctx ->
                                    YouTubePlayerView(ctx).also { view ->
                                        playerViewRef.value = view
                                        lifecycleOwner.lifecycle.addObserver(view)
                                        view.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                                playerReady = true
                                                youTubePlayer.loadVideo(video.videoId, 0f)
                                            }
                                            override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                                                playerError = true
                                            }
                                        })
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            if (!playerReady) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = ytRed, strokeWidth = 3.dp)
                                }
                            }
                        }
                        else -> {
                            // Thumbnail with play button overlay
                            AsyncImage(
                                model = video.thumbnail
                                    ?: "https://img.youtube.com/vi/${video.videoId}/hqdefault.jpg",
                                contentDescription = video.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .clickable { playerVisible = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ytRed,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Badges: content type + duration
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContentTypeBadge(contentTypeLabel, ytRed)
                    if (durationStr != null) {
                        Surface(color = Color.Black, shape = RoundedCornerShape(4.dp)) {
                            Text(
                                durationStr,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = video.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                // Channel + publish date
                if (!video.authorName.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = textGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(video.authorName, fontSize = 12.sp, color = textGray)
                        if (!video.publishedAt.isNullOrBlank()) {
                            Text(
                                "  ·  ${formatDate(video.publishedAt)}",
                                fontSize = 12.sp,
                                color = textGray
                            )
                        }
                    }
                }

                // Collapsible description
                if (!video.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = video.description,
                        fontSize = 12.sp,
                        color = textGray,
                        fontStyle = FontStyle.Italic,
                        maxLines = if (descExpanded) Int.MAX_VALUE else 2,
                        overflow = if (descExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                        modifier = Modifier
                            .animateContentSize()
                            .clickable { descExpanded = !descExpanded }
                    )
                    Text(
                        text = if (descExpanded) "Show less" else "Show more",
                        fontSize = 11.sp,
                        color = ytRed,
                        modifier = Modifier.clickable { descExpanded = !descExpanded }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = softGray)
                Spacer(modifier = Modifier.height(12.dp))

                // Public metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    VideoStatItem("Views",    formatCount(video.viewCount ?: video.analytics?.views?.toString()))
                    VideoStatItem("Likes",    formatCount(video.likeCount ?: video.analytics?.likes?.toString()))
                    VideoStatItem("Comments", formatCount(video.commentCount ?: video.analytics?.comments?.toString()))
                }

                // Private channel analytics (YouTube Analytics API)
                val analytics = video.analytics
                if (analytics != null && (analytics.watchTimeMinutes != null || analytics.engagementRate != null)) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = softGray)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val watchTime = analytics.watchTimeMinutes ?: 0.0
                        VideoStatItem(
                            "Watch Time",
                            if (watchTime >= 1000) "${(watchTime / 60).toInt()}h" else "${watchTime.toInt()}m"
                        )
                        VideoStatItem("Subs Gained", analytics.subscribersGained?.toString() ?: "—")
                        VideoStatItem("Engagement",  analytics.engagementRate ?: "—")
                    }
                }

                // Watch on YouTube button
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ytRed)
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Watch on YouTube", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun VideoStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.Black)
        Text(text = label, fontSize = 10.sp, color = textGray)
    }
}

@Composable
fun AnalyticsHeader(navController: NavController, collaboration: Collaboration?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(themeColor_campaign, themeColor_campaign.copy(alpha = 0.9f))
                )
            )
            .padding(bottom = 24.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.vector),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .alpha(0.25f),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "ANALYTICS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (collaboration != null) {
                Text(
                    collaboration.campaign.title,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 36.sp
                )
                
                Text(
                    "with ${collaboration.influencer.name}",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun InfluencerProfileCard(collaboration: Collaboration) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = Color.LightGray.copy(alpha = 0.1f)
            ) {
                AsyncImage(
                    model = collaboration.influencer.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.brand_profile)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(collaboration.influencer.name, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Influencer", color = textGray, fontSize = 12.sp)
            }
            if (collaboration.status == "COMPLETED") {
                Surface(
                    color = Color(0xFF2E7D32).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DONE", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPlaceholder(message: String, icon: ImageVector) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = themeColor_campaign.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.CenterStart) {
        Text(
            title,
            color = themeColor_campaign.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun TotalImpressionsCard(impressions: String, subtext: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColor_campaign)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "TOTAL IMPRESSIONS",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        impressions,
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                Text(
                    subtext,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun OverallStatsGrid(collaboration: Collaboration) {
    val stats = collaboration.overallAnalytics ?: return
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(Icons.Default.PlayArrow, Color(0xFF4285F4), String.format("%,d", stats.views ?: 0), "Views", Color(0xFF4285F4), Modifier.weight(1f))
            StatCard(Icons.Default.AdsClick, Color(0xFF9E9E9E), String.format("%,d", stats.clicks ?: 0), "Clicks", Color(0xFF9E9E9E), Modifier.weight(1f))
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(Icons.Default.Favorite, Color(0xFFFF5252), String.format("%,d", stats.likes ?: 0), "Likes", Color(0xFFFF5252), Modifier.weight(1f))
            StatCard(Icons.Default.ChatBubble, Color(0xFFFFD740), String.format("%,d", stats.comments ?: 0), "Comments", Color(0xFFFFD740), Modifier.weight(1f))
        }
    }
}

@Composable
fun EngagementBreakdownCard(likes: Int, comments: Int, shares: Int, saves: Int, title: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(title, color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(20.dp))
            val total = (likes + comments + shares + saves).toFloat()
            EngagementBarItem("Likes", likes, total, Color(0xFFFF5252), Icons.Default.Favorite)
            EngagementBarItem("Comments", comments, total, Color(0xFFFFD740), Icons.Default.ChatBubble)
            EngagementBarItem("Shares", shares, total, Color(0xFF448AFF), Icons.Default.IosShare)
            EngagementBarItem("Saves", saves, total, Color(0xFF1DE9B6), Icons.Default.Label)
        }
    }
}

@Composable
fun EngagementBarItem(label: String, value: Int, total: Float, color: Color, icon: ImageVector) {
    val progress = if (total > 0) value / total else 0f
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = textGray, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Text(String.format("%,d", value), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun StatCard(icon: ImageVector, iconColor: Color, value: String, label: String, topAccentColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(95.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(topAccentColor))
            Column(modifier = Modifier.padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Surface(shape = RoundedCornerShape(8.dp), color = iconColor.copy(alpha = 0.1f), modifier = Modifier.size(28.dp)) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(6.dp))
                }
                Column {
                    Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    Text(label.uppercase(), fontSize = 9.sp, color = textGray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CampaignDurationCard(duration: String) {
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = themeColor_campaign)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("CAMPAIGN DURATION", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("$duration days", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun PlatformMetricCard(analytics: CollaborationAnalytics, expandedDefault: Boolean = false) {
    var expanded by remember { mutableStateOf(expandedDefault) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(12.dp), color = Color.Transparent) {
                    val platformType = analytics.platform?.lowercase()
                    if (platformType == "instagram") {
                        Image(painter = painterResource(id = R.drawable.instagram_logo), contentDescription = null, modifier = Modifier.padding(8.dp))
                    } else if (platformType == "youtube") {
                        Image(painter = painterResource(id = R.drawable.youtube_logo), contentDescription = null, modifier = Modifier.padding(8.dp))
                    } else {
                        Icon(imageVector = getPlatformIcon(analytics.platform), contentDescription = null, tint = themeColor_campaign, modifier = Modifier.padding(8.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = analytics.platform?.uppercase() ?: "UNKNOWN", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Black)
                    Text(text = "${String.format("%,d", analytics.impressions ?: 0)} impressions", fontSize = 12.sp, color = textGray)
                }
                Surface(color = Color(0xFFFFF4F4), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFDEDE))) {
                    Text(text = "₹${analytics.cost ?: 0f}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB35A5A))
                }
                Icon(imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = textGray, modifier = Modifier.padding(start = 8.dp))
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    PlatformStatsDetails(analytics)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    EngagementBreakdownCard(
                        likes = analytics.likes ?: 0,
                        comments = analytics.comments ?: 0,
                        shares = analytics.shares ?: 0,
                        saves = analytics.saves ?: 0,
                        title = "Platform Engagement"
                    )
                }
            }
        }
    }
}

@Composable
fun PlatformStatsDetails(analytics: CollaborationAnalytics) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniStatCard("Views", String.format("%,d", analytics.views ?: 0), Icons.Default.PlayArrow, Color(0xFF4285F4), Modifier.weight(1f))
            MiniStatCard("Clicks", String.format("%,d", analytics.clicks ?: 0), Icons.Default.AdsClick, Color(0xFF9E9E9E), Modifier.weight(1f))
        }
    }
}

@Composable
fun MiniStatCard(label: String, value: String, icon: ImageVector, iconColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F9FE))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = iconColor.copy(alpha = 0.1f),
            modifier = Modifier.size(28.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.padding(6.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, color = textGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

fun getPlatformIcon(platform: String?): ImageVector {
    return when (platform?.lowercase()) {
        "instagram" -> Icons.Default.CameraAlt
        "youtube" -> Icons.Default.PlayCircle
        "facebook" -> Icons.Default.Facebook
        else -> Icons.Default.Language
    }
}

// ---------------------------------------------------------------------------
// Campaign Target Tracking — brand-only. Shown independent of the COMPLETED-
// gated analytics sections above, since brands should be able to set and
// monitor targets throughout the live collaboration, not just afterwards.
// ---------------------------------------------------------------------------

@Composable
fun BrandTargetsSection(collaboration: Collaboration, onSetTargets: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        SectionTitle("CAMPAIGN TARGETS")
        Spacer(modifier = Modifier.height(8.dp))

        val targets = collaboration.performanceTargets
        val tracking = collaboration.performanceTracking

        if (targets == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = themeColor_campaign.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No targets set for this collaboration yet",
                        color = textGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onSetTargets,
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor_campaign)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Set Targets")
                    }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onSetTargets) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = themeColor_campaign)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Targets", color = themeColor_campaign, fontSize = 13.sp)
                }
            }

            if (tracking != null) {
                PerformanceScoreCard(tracking)
                Spacer(modifier = Modifier.height(16.dp))
                TargetAchievementCard(tracking)
                if (tracking.history.size >= 2) {
                    Spacer(modifier = Modifier.height(16.dp))
                    PerformanceTrendCard(tracking.history)
                }
            }
        }
    }
}

@Composable
fun PerformanceScoreCard(tracking: PerformanceTracking) {
    val score = tracking.performanceScore
    val outcomeColor = when (tracking.campaignOutcome) {
        "EXCEEDED" -> Color(0xFF2E7D32)
        "MET" -> Color(0xFF388E3C)
        "PARTIAL" -> Color(0xFFFFA000)
        "MISSED" -> Color(0xFFD32F2F)
        else -> themeColor_campaign
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(outcomeColor)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "PERFORMANCE SCORE",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    score?.let { "${it.toInt()}/100" } ?: "—",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black
                )
                if (tracking.campaignOutcome != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        tracking.campaignOutcome.replace("_", " "),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            tracking.overallAchievedPercent?.let { pct ->
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.2f), modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${pct.toInt()}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TargetAchievementCard(tracking: PerformanceTracking) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Target vs Actual", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            tracking.achievements.forEachIndexed { index, achievement ->
                AchievementRow(achievement)
                if (index != tracking.achievements.lastIndex) {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
fun AchievementRow(achievement: PerformanceAchievement) {
    val statusColor = when (achievement.status) {
        "ON_TRACK" -> Color(0xFF4CAF50)
        "AT_RISK" -> Color(0xFFFFA000)
        "BEHIND" -> Color(0xFFD32F2F)
        else -> Color.LightGray
    }
    val label = achievement.metric.replaceFirstChar { it.uppercase() }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = textGray, fontSize = 13.sp)
            if (!achievement.tracked) {
                Text("Not tracked yet", color = Color.LightGray, fontSize = 12.sp, fontStyle = FontStyle.Italic)
            } else {
                Text(
                    "${formatMetricValue(achievement.actual)} / ${formatMetricValue(achievement.target)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.Black
                )
            }
        }
        if (achievement.tracked && achievement.achievedPercent != null) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (achievement.achievedPercent / 100f).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.15f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "${achievement.achievedPercent.toInt()}% achieved",
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

fun formatMetricValue(value: Double?): String {
    if (value == null) return "—"
    return if (value % 1.0 == 0.0) String.format("%,d", value.toLong()) else String.format("%.1f", value)
}

private fun formatTargetForInput(value: Double?): String {
    if (value == null) return ""
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

@Composable
fun PerformanceTrendCard(history: List<PerformanceSnapshot>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Performance Score Trend", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            val points = history.mapIndexedNotNull { index, snapshot ->
                snapshot.performanceScore?.let { Point(index.toFloat(), it.toFloat()) }
            }

            if (points.size >= 2) {
                Box(modifier = Modifier.height(120.dp).fillMaxWidth()) {
                    LineChart(
                        modifier = Modifier.fillMaxSize(),
                        data = points,
                        lineColor = themeColor_campaign,
                        yMaxOverride = 100f
                    )
                }
            } else {
                Text("Not enough history yet to show a trend", color = textGray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SetTargetsDialog(
    collaboration: Collaboration,
    brandViewModel: BrandViewModel,
    onDismiss: () -> Unit
) {
    val existing = collaboration.performanceTargets
    var views by remember { mutableStateOf(formatTargetForInput(existing?.targetViews)) }
    var reach by remember { mutableStateOf(formatTargetForInput(existing?.targetReach)) }
    var engagementRate by remember { mutableStateOf(formatTargetForInput(existing?.targetEngagementRate)) }
    var likes by remember { mutableStateOf(formatTargetForInput(existing?.targetLikes)) }
    var comments by remember { mutableStateOf(formatTargetForInput(existing?.targetComments)) }
    var shares by remember { mutableStateOf(formatTargetForInput(existing?.targetShares)) }
    var saves by remember { mutableStateOf(formatTargetForInput(existing?.targetSaves)) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Set Performance Targets", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Visible only to you — never shown to the influencer.",
                    color = textGray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                TargetField("Target Views", views) { views = it }
                TargetField("Target Reach", reach) { reach = it }
                TargetField("Target Engagement Rate (%)", engagementRate) { engagementRate = it }
                TargetField("Target Likes", likes) { likes = it }
                TargetField("Target Comments", comments) { comments = it }
                TargetField("Target Shares", shares) { shares = it }
                TargetField("Target Saves", saves) { saves = it }
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(error ?: "", color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    val fields = listOf(views, reach, engagementRate, likes, comments, shares, saves)
                    if (fields.all { it.isBlank() }) {
                        error = "Set at least one target"
                        return@Button
                    }
                    error = null
                    saving = true
                    FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { tokenResult ->
                        val token = tokenResult.token
                        if (token == null) {
                            saving = false
                            error = "Could not authenticate. Please try again."
                            return@addOnSuccessListener
                        }
                        scope.launch {
                            val result = brandViewModel.setCollaborationTargets(
                                collaborationId = collaboration.id,
                                targetViews = views.toDoubleOrNull(),
                                targetReach = reach.toDoubleOrNull(),
                                targetEngagementRate = engagementRate.toDoubleOrNull(),
                                targetLikes = likes.toDoubleOrNull(),
                                targetComments = comments.toDoubleOrNull(),
                                targetShares = shares.toDoubleOrNull(),
                                targetSaves = saves.toDoubleOrNull(),
                                token = token
                            )
                            saving = false
                            if (result.isSuccess) {
                                onDismiss()
                            } else {
                                error = "Failed to save targets. Please try again."
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = themeColor_campaign)
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!saving) onDismiss() }) { Text("Cancel") }
        }
    )
}

@Composable
fun TargetField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> if (new.isEmpty() || new.matches(Regex("^\\d*\\.?\\d*$"))) onValueChange(new) },
        label = { Text(label, fontSize = 13.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = true
    )
}

@Composable
fun ContentTypeBadge(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = label.uppercase(),
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun HashtagChip(tag: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = tag,
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PerformanceTimelineCard(milestones: List<PerformanceMilestone>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Growth over time", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))

            milestones.forEachIndexed { idx, m ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Timeline spine
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(32.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(10.dp)
                        ) {}
                        if (idx < milestones.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(48.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            m.label.ifBlank { "${m.hoursAfterPost}h after post" },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (m.views != null) {
                                Text(
                                    "${String.format("%,d", m.views)} views",
                                    fontSize = 11.sp,
                                    color = Color(0xFF4285F4)
                                )
                            }
                            if (m.likes != null) {
                                Text(
                                    "${String.format("%,d", m.likes)} likes",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF5252)
                                )
                            }
                            if (m.comments != null) {
                                Text(
                                    "${String.format("%,d", m.comments)} comments",
                                    fontSize = 11.sp,
                                    color = textGray
                                )
                            }
                        }
                        if (!m.capturedAt.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                m.capturedAt.split("T").firstOrNull() ?: m.capturedAt,
                                fontSize = 10.sp,
                                color = textGray
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
