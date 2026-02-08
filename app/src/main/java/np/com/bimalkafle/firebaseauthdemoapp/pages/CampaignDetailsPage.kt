package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.CampaignViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CampaignDetailsPage(
    onBack: () -> Unit = {},
    onSearchInfluencer: () -> Unit = {},
    campaignViewModel: CampaignViewModel = CampaignViewModel()
) {
    val createdCampaign by campaignViewModel.createdCampaign.observeAsState()
    
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val headerHeight = screenHeight * 0.4f
    val formPaddingTop = headerHeight - 40.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .background(Color(0xFFFF8383))
        ) {
            Image(
                painter = painterResource(id = R.drawable.vector),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.2f),
                contentScale = ContentScale.Crop
            )
            IconButton(onClick = onBack, modifier = Modifier.padding(16.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (createdCampaign?.brand?.logoUrl != null) {
                    AsyncImage(
                        model = createdCampaign?.brand?.logoUrl,
                        contentDescription = "Brand Logo",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.brand_profile)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.brand_profile),
                        contentDescription = "Brand Logo",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You're all set",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your Campaign has been created successfully",
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Form Content
        Column(
            modifier = Modifier
                .padding(top = formPaddingTop)
                .padding(horizontal = 24.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.brand2),
                contentDescription = "Rocket Launch",
                modifier = Modifier.size(150.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
                    .padding(16.dp)
            ) {
                CampaignDetailRow("Brand Name", createdCampaign?.brand?.name ?: "N/A")
                Divider(color = Color.LightGray)
                CampaignDetailRow("Category", createdCampaign?.brand?.brandCategory?.category ?: "N/A")
                Divider(color = Color.LightGray)
                
                val budgetRange = if (createdCampaign?.budgetMin != null && createdCampaign?.budgetMax != null) {
                    "₹${formatBudgetValue(createdCampaign?.budgetMin?.toFloat() ?: 0f)} - ${formatBudgetValue(createdCampaign?.budgetMax?.toFloat() ?: 0f)}"
                } else "N/A"
                CampaignDetailRow("Budget Range", budgetRange)
                Divider(color = Color.LightGray)

                val duration = if (createdCampaign?.startDate != null && createdCampaign?.endDate != null) {
                    "${formatDate(createdCampaign?.startDate!!)} - ${formatDate(createdCampaign?.endDate!!)}"
                } else "N/A"
                CampaignDetailRow("Duration", duration)
                Divider(color = Color.LightGray)

                val ageRange = if (createdCampaign?.brand?.targetAudience?.ageMin != null && createdCampaign?.brand?.targetAudience?.ageMax != null) {
                    "${createdCampaign?.brand?.targetAudience?.ageMin} - ${createdCampaign?.brand?.targetAudience?.ageMax}"
                } else "N/A"
                CampaignDetailRow("Age group", ageRange)
                Divider(color = Color.LightGray)

                val platforms = createdCampaign?.brand?.preferredPlatforms?.joinToString(", ") { it.platform } ?: "N/A"
                CampaignDetailRow("Platform", platforms)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onSearchInfluencer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFF8383)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SEARCH INFLUENCER", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun formatBudgetValue(value: Float): String {
    return if (value >= 100) {
        val lakhs = value / 100f
        if (lakhs.rem(1) == 0f) {
            "${lakhs.toInt()}L"
        } else {
            String.format("%.1f", lakhs) + "L"
        }
    } else {
        "${value.toInt()}K"
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date!!)
    } catch (e: Exception) {
        dateString
    }
}

@Composable
fun CampaignDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text(value, color = Color.Gray)
    }
}

@Preview(showBackground = true)
@Composable
fun CampaignDetailsPagePreview() {
    CampaignDetailsPage()
}
