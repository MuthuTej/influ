package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import np.com.bimalkafle.firebaseauthdemoapp.AuthViewModel
import np.com.bimalkafle.firebaseauthdemoapp.viewmodel.BrandViewModel

@Composable
fun BrandProfilePage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    brandViewModel: BrandViewModel
) {
    val brandProfile by brandViewModel.brandProfile.observeAsState()
    val isLoading by brandViewModel.loading.observeAsState(initial = false)

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnSuccessListener { result ->
            val firebaseToken = result.token
            if (firebaseToken != null) {
                brandViewModel.fetchBrandDetails(firebaseToken)
            }
        }
    }

    Scaffold(
        bottomBar = {
            BrandBottomNavigationBar(
                selectedItem = "Profile",
                onItemSelected = { /* Handled in the component */ },
                onCreateCampaign = { navController.navigate("create_campaign") },
                navController = navController
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFF8383))
            }
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
            ) {
                // Profile Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFFFF8383)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            color = Color.White
                        ) {
                            if (!brandProfile?.logoUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = brandProfile?.logoUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Business,
                                    contentDescription = null,
                                    modifier = Modifier.padding(24.dp),
                                    tint = Color(0xFFFF8383)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = brandProfile?.name ?: "Brand Name",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Profile Details
                Column(modifier = Modifier.padding(16.dp)) {
                    ProfileSectionTitle("About")
                    Text(
                        text = brandProfile?.about ?: "No information available.",
                        color = Color.DarkGray,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    ProfileSectionTitle("Primary Objective")
                    Text(
                        text = brandProfile?.primaryObjective ?: "No objective set.",
                        color = Color.DarkGray,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionTitle("Category")
                    brandProfile?.brandCategory?.let { category ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFF8383).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${category.category} • ${category.subCategory}",
                                color = Color(0xFFFF8383),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } ?: Text("Not specified", color = Color.Gray, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = { authViewModel.signout() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = Color.Red)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out", color = Color.Red)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
