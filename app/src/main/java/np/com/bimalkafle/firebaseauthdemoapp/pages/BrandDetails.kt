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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel
import np.com.bimalkafle.firebaseauthdemoapp.R
import np.com.bimalkafle.firebaseauthdemoapp.model.Brand
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BrandDetailsScreen(
    onBack: () -> Unit,
    onCreateCampaign: () -> Unit,
    onGoToHome: () -> Unit,
    brandViewModel: BrandViewModel
) {
    val brandProfile by brandViewModel.brandProfile.observeAsState()
    val isLoading by brandViewModel.loading.observeAsState(initial = false)
    val error by brandViewModel.error.observeAsState()

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val token = result.token
            if (token != null) {
                brandViewModel.fetchBrandDetails(token)
            }
        }
    }

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
                modifier = Modifier.fillMaxSize()
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
                if (!brandProfile?.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = brandProfile?.logoUrl,
                        contentDescription = "Brand Logo",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.brand_profile), // Placeholder
                        contentDescription = "Brand Logo",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = brandProfile?.name ?: "You're all set",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (brandProfile != null) "Your brand has been registered, create your campaign" else "Loading...",
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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFF8383))
                }
            } else {
                Image(
                    painter = painterResource(id = R.drawable.brand2),
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
                    DetailRow(label = "Brand Name", value = brandProfile?.name ?: "N/A")
                    DetailRow(label = "Category", value = brandProfile?.brandCategory?.category ?: "N/A")
                    DetailRow(label = "Objective", value = brandProfile?.primaryObjective ?: "N/A")
                }

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = onGoToHome,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF8383)),
                ) {
                    Text("GO TO HOME SCREEN", color = Color(0xFFFF8383), fontWeight = FontWeight.Bold)
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
    // We can pass a dummy or null ViewModel for preview if needed, 
    // but usually we create a mock version. 
    // For now, I'll just fix the call structure if possible, 
    // or comment it out if it's too complex to mock here.
    // BrandDetailsScreen(onBack = {}, onCreateCampaign = {}, onGoToHome = {}, brandViewModel = ...) 
}
