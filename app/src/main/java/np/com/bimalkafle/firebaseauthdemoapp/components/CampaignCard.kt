package np.com.bimalkafle.firebaseauthdemoapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import np.com.bimalkafle.firebaseauthdemoapp.model.Campaign
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.Dimens
import np.com.bimalkafle.firebaseauthdemoapp.ui.theme.LocalAppColors

/**
 * Campaign summary card, replacing the campaign card layouts duplicated in
 * BrandHomePage, InfluencerHomePage and AllCampaignPage.
 */
@Composable
fun CampaignCard(
    campaign: Campaign,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AppCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    campaign.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(Dimens.space8))
            CampaignStatusChip(status = campaign.status)
        }

        if (!campaign.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(Dimens.space4))
            Text(
                campaign.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (campaign.budgetMin != null && campaign.budgetMax != null) {
            Spacer(modifier = Modifier.height(Dimens.space12))
            Text(
                "Budget: ₹${campaign.budgetMin} - ₹${campaign.budgetMax}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        val platformNames = campaign.platforms?.mapNotNull { it.platform }
        if (!platformNames.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(Dimens.space8))
            Text(
                platformNames.joinToString(" • "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CampaignStatusChip(status: String?) {
    val appColors = LocalAppColors.current
    val (background, foreground, label) = when (status?.uppercase()) {
        "OPEN" -> Triple(appColors.success.copy(alpha = 0.15f), appColors.success, "Open")
        "CLOSED" -> Triple(appColors.textDisabled.copy(alpha = 0.2f), appColors.textSecondary, "Closed")
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            status ?: "Unknown"
        )
    }
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .padding(horizontal = Dimens.space8, vertical = Dimens.space4)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = foreground)
    }
}
