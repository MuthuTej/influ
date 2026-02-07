package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import np.com.bimalkafle.firebaseauthdemoapp.R

@Composable
fun BrandDetailsScreen(
    onBack: () -> Unit,
    onCreateCampaign: () -> Unit,
    onGoToHome: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val headerHeight = screenHeight * 0.4f
    val formPaddingTop = headerHeight - 40.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
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
                modifier = Modifier.fillMaxSize(),
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
                Image(
                    painter = painterResource(id = R.drawable.splash1), // Placeholder
                    contentDescription = "Brand Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "You're all set",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Your brand has been registered, create your campaign",
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Content
        Column(
            modifier = Modifier
                .padding(top = formPaddingTop)
                // This outer padding creates the margin around the white card
                .padding(horizontal = 24.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color.White)
                // This inner padding is for the content (image, buttons) inside the card
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash1),
                contentDescription = null,
                modifier = Modifier.height(screenHeight * 0.2f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Brand Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE8EAF6))
                    .padding(16.dp)
            ) {
                DetailRow(label = "Brand Name", value = "Myntra")
                DetailRow(label = "Category", value = "E-commerce")
                DetailRow(label = "Objective", value = "Brand Awareness")
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = onGoToHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF4B4CED)),
            ) {
                Text("GO TO HOME SCREEN", color = Color(0xFF4B4CED), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCreateCampaign,
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
                    Text("CREATE CAMPAIGN", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold)
        Text(text = value)
    }
}

@Preview(showBackground = true)
@Composable
fun BrandDetailsScreenPreview() {
    BrandDetailsScreen(onBack = {}, onCreateCampaign = {}, onGoToHome = {})
}
