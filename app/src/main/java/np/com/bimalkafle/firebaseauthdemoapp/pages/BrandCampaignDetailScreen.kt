package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.components.ErrorState
import np.com.bimalkafle.firebaseauthdemoapp.components.LoadingState
import np.com.bimalkafle.firebaseauthdemoapp.model.CampaignCollaborationSummary
import np.com.bimalkafle.firebaseauthdemoapp.model.CampaignDetail
import np.com.bimalkafle.firebaseauthdemoapp.model.CampaignOverallAnalytics
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.Dimens
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.LocalAppColors
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel

private fun formatBudget(amount: Int?): String {
    if (amount == null) return "—"
    return when {
        amount >= 100_000 -> "${"%.1f".format(amount / 100_000.0)}L"
        amount >= 1_000 -> "${"%.1f".format(amount / 1_000.0)}K"
        else -> "$amount"
    }
}

/**
 * Shows one existing campaign by ID, fetched fresh from the backend —
 * unlike CampaignDetailsPage, which only renders the in-memory state left
 * over from the create-campaign flow. Used both from AllCampaignPage and
 * from "View campaign" buttons the AI chat attaches to its replies.
 */
@Composable
fun BrandCampaignDetailScreen(
    campaignId: String,
    navController: NavHostController,
    campaignViewModel: CampaignViewModel
) {
    val appColors = LocalAppColors.current
    val campaign by campaignViewModel.campaign.observeAsState(initial = null)
    val isLoading by campaignViewModel.loading.observeAsState(initial = false)
    val error by campaignViewModel.error.observeAsState(initial = null)

    LaunchedEffect(campaignId) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            result.token?.let { token -> campaignViewModel.fetchCampaignById(campaignId, token) }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(appColors.brandPrimary)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.space16, vertical = Dimens.space16),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .size(Dimens.minTouchTarget)
                        .background(Color.White.copy(alpha = 0.18f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(Dimens.space12))
                Column {
                    Text("Campaign", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    Text(
                        campaign?.title ?: "Loading…",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(appColors.surfaceElevated)
            ) {
                when {
                    isLoading && campaign == null -> LoadingState(message = "Loading campaign…")
                    error != null && campaign == null -> ErrorState(message = error ?: "Something went wrong")
                    campaign != null -> CampaignDetailBody(campaign!!, navController)
                }
            }
        }
    }
}

@Composable
private fun CampaignDetailBody(campaign: CampaignDetail, navController: NavHostController) {
    val appColors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.space20)
    ) {
        StatusBadge(campaign.status)
        Spacer(modifier = Modifier.height(Dimens.space12))

        Text(campaign.description, color = appColors.textSecondary, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
        Spacer(modifier = Modifier.height(Dimens.space24))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.space12)) {
            val budgetValue = if (campaign.budgetMin != null || campaign.budgetMax != null) {
                "${formatBudget(campaign.budgetMin)} – ${formatBudget(campaign.budgetMax)}"
            } else "—"

            DetailInfoCard(
                icon = Icons.Default.CurrencyRupee,
                label = "Budget",
                value = budgetValue,
                modifier = Modifier.weight(1f)
            )
            DetailInfoCard(
                icon = Icons.Default.CalendarToday,
                label = "Duration",
                value = "${formatDateShort(campaign.startDate)} – ${formatDateShort(campaign.endDate)}",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(Dimens.space12))

        campaign.targetAudience?.let { audience ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.space12)) {
                DetailInfoCard(
                    icon = Icons.Default.People,
                    label = "Target age",
                    value = "${audience.ageMin ?: "—"}–${audience.ageMax ?: "—"} yrs",
                    modifier = Modifier.weight(1f)
                )
                DetailInfoCard(
                    icon = Icons.Default.LocationOn,
                    label = "Locations",
                    value = audience.locations?.joinToString(", ") ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(Dimens.space24))
        }

        campaign.categories?.takeIf { it.isNotEmpty() }?.let {
            ChipSection(title = "Categories", values = it.map { c -> c.category })
        }
        campaign.platforms?.takeIf { it.isNotEmpty() }?.let {
            ChipSection(title = "Platforms", values = it.map { p -> p.platform })
        }

        CombinedCollaborationsSection(
            collaborations = campaign.collaborations ?: emptyList(),
            overallAnalytics = campaign.overallAnalytics,
            onCollaborationClick = { id -> navController.navigate("collaboration_analytics/$id") }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CombinedCollaborationsSection(
    collaborations: List<CampaignCollaborationSummary>,
    overallAnalytics: CampaignOverallAnalytics?,
    onCollaborationClick: (String) -> Unit
) {
    val appColors = LocalAppColors.current

    Spacer(modifier = Modifier.height(Dimens.space8))
    HorizontalDivider(color = appColors.divider)
    Spacer(modifier = Modifier.height(Dimens.space20))

    Text("Collaborations overview", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
    Spacer(modifier = Modifier.height(Dimens.space12))

    if (collaborations.isEmpty()) {
        Text(
            "No collaborations yet for this campaign.",
            color = appColors.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = Dimens.space20)
        )
        return
    }

    val totalSpend = collaborations.sumOf { it.totalPrice }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.space12)) {
        DetailInfoCard(
            icon = Icons.Default.CurrencyRupee,
            label = "Total spend",
            value = formatBudget(totalSpend),
            modifier = Modifier.weight(1f)
        )
        DetailInfoCard(
            icon = Icons.Default.Groups,
            label = "Collaborations",
            value = collaborations.size.toString(),
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(Dimens.space16))

    val statusCounts = collaborations.groupingBy { it.status }.eachCount()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.space8), verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
        statusCounts.forEach { (status, count) ->
            val proposalStatus = runCatching { ProposalStatus.valueOf(status) }.getOrNull()
            val color = proposalStatus?.color ?: appColors.textSecondary
            Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = Dimens.space12, vertical = Dimens.space8)
                ) {
                    proposalStatus?.let {
                        Icon(it.icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(Dimens.space4))
                    }
                    Text("$count ${proposalStatus?.displayName ?: status}", color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(Dimens.space20))

    Text("Accumulated performance", fontSize = 14.sp, color = appColors.textSecondary, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(Dimens.space8))

    val metrics = overallAnalytics?.let {
        listOfNotNull(
            it.views?.takeIf { v -> v > 0 }?.let { v -> "Views" to v },
            it.likes?.takeIf { v -> v > 0 }?.let { v -> "Likes" to v },
            it.comments?.takeIf { v -> v > 0 }?.let { v -> "Comments" to v },
            it.shares?.takeIf { v -> v > 0 }?.let { v -> "Shares" to v },
            it.retweets?.takeIf { v -> v > 0 }?.let { v -> "Retweets" to v },
            it.replies?.takeIf { v -> v > 0 }?.let { v -> "Replies" to v },
            it.impressions?.takeIf { v -> v > 0 }?.let { v -> "Impressions" to v },
            it.clicks?.takeIf { v -> v > 0 }?.let { v -> "Clicks" to v },
            it.saves?.takeIf { v -> v > 0 }?.let { v -> "Saves" to v }
        )
    } ?: emptyList()

    if (metrics.isEmpty()) {
        Text(
            "No analytics yet — these accumulate once collaborations are completed and post data comes in.",
            color = appColors.textSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = Dimens.space20)
        )
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.space8), verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
            metrics.forEach { (label, value) -> StatChip(label, value) }
        }
        Spacer(modifier = Modifier.height(Dimens.space20))
    }

    Text("All collaborations", fontSize = 14.sp, color = appColors.textSecondary, fontWeight = FontWeight.Medium)
    Spacer(modifier = Modifier.height(Dimens.space8))
    collaborations.forEach { collab ->
        CollaborationRow(collab, onClick = { onCollaborationClick(collab.id) })
        Spacer(modifier = Modifier.height(Dimens.space8))
    }
}

@Composable
private fun StatChip(label: String, value: Int) {
    val appColors = LocalAppColors.current
    Surface(color = appColors.surfaceSubtle, shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(horizontal = Dimens.space12, vertical = Dimens.space8)) {
            Text("%,d".format(value), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
            Text(label, fontSize = 11.sp, color = appColors.textSecondary)
        }
    }
}

@Composable
private fun CollaborationRow(collab: CampaignCollaborationSummary, onClick: () -> Unit) {
    val appColors = LocalAppColors.current
    val proposalStatus = runCatching { ProposalStatus.valueOf(collab.status) }.getOrNull()
    val statusColor = proposalStatus?.color ?: appColors.textSecondary

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = appColors.surfaceSubtle,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.space12, vertical = Dimens.space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    collab.influencerName ?: collab.influencerHandle ?: "Influencer",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = appColors.textPrimary,
                    maxLines = 1
                )
                collab.influencerHandle?.let {
                    Text("@$it", fontSize = 12.sp, color = appColors.textSecondary)
                }
            }
            if (collab.totalPrice > 0) {
                Text(formatBudget(collab.totalPrice), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = appColors.textPrimary)
                Spacer(modifier = Modifier.width(Dimens.space8))
            }
            Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                Text(
                    proposalStatus?.displayName ?: collab.status,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = Dimens.space8, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(Dimens.space4))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = appColors.textSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DetailInfoCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    val appColors = LocalAppColors.current
    Surface(
        modifier = modifier,
        color = appColors.surfaceSubtle,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(Dimens.space12)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(appColors.brandPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = appColors.brandPrimary, modifier = Modifier.size(14.dp))
                }
                Spacer(modifier = Modifier.width(Dimens.space8))
                Text(
                    text = value,
                    fontSize = 13.sp,
                    color = appColors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(Dimens.space4))
            Text(label, fontSize = 10.sp, color = appColors.textSecondary, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipSection(title: String, values: List<String>) {
    val appColors = LocalAppColors.current
    Column(modifier = Modifier.padding(bottom = Dimens.space20)) {
        Text(title, fontSize = 13.sp, color = appColors.textSecondary, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(Dimens.space8))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.space8), verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
            values.forEach { value ->
                Surface(color = appColors.brandPrimary.copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp)) {
                    Text(
                        value,
                        color = appColors.brandPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = Dimens.space12, vertical = Dimens.space8)
                    )
                }
            }
        }
    }
}
