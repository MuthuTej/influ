package np.com.bimalkafle.firebaseauthdemoapp.pages

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import np.com.bimalkafle.firebaseauthdemoapp.R

val themeColor = Color(0xFFFF8383)
val orangeColor = Color(0xFFFFA500)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignAnalyticsPage(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campaign Analytics", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColor
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.brand_profile),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Alexa Rawles", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("alexarawles@gmail.com", fontSize = 14.sp, color = Color.Gray)
                }
            }

            Text("Overview", style = MaterialTheme.typography.headlineMedium, modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp))

            // Viewers Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Viewers", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(150.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                         CircularProgressIndicator(
                            progress = { 0.6f },
                            modifier = Modifier.fillMaxSize(),
                            color = themeColor,
                            strokeWidth = 12.dp,
                             trackColor = themeColor.copy(alpha = 0.2f)
                        )
                        Text("8K", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    }
                }
            }

            // Stat Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                 StatCard("Impact", "180%", "+5.0%")
                 StatCard("Total", "150K", "+5.0%")
            }
             Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                 StatCard("Views", "7,265", "+11.01%")
                 StatCard("New Users", "256", "+12.01%")
            }

            // Performance Comparison (Visits)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Performance comparison(visits)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendItem(color = themeColor, text = "Before collab")
                        LegendItem(color = Color.Green, text = "After collab")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Image(
                        painter = painterResource(id = R.drawable.vector),
                        contentDescription = "Visits chart placeholder",
                        modifier = Modifier
                            .height(150.dp)
                            .fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HitRateItem(progress = 0.68f, text = "68%", description = "Hit Rate this year", icon = Icons.Default.GpsFixed)
                        HitRateItem(progress = 0.76f, text = "76%", description = "Deals this year", icon = Icons.Default.BusinessCenter)
                    }
                }
            }

            // Performance Comparison (Sales)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Performance comparison(sales)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendItem(color = Color.Green, text = "Before collab")
                        LegendItem(color = themeColor, text = "After collab")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Image(
                        painter = painterResource(id = R.drawable.vector),
                        contentDescription = "Sales chart placeholder",
                        modifier = Modifier
                            .height(150.dp)
                            .fillMaxWidth()
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GenderSplitCard(modifier = Modifier.weight(1f))
                AgeGroupCard(modifier = Modifier.weight(1f))
            }

            TopLocationsCard()
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, change: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = change, style = MaterialTheme.typography.bodySmall, color = Color.Green)
        }
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier
            .size(10.dp)
            .background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
private fun HitRateItem(progress: Float, text: String, description: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = themeColor,
                trackColor = themeColor.copy(alpha = 0.2f),
                strokeWidth = 5.dp
            )
            Icon(imageVector = icon, contentDescription = null, tint = themeColor, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(description, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun TopLocationsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Year Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.ArrowBackIos, contentDescription = "Previous Year")
                }
                Text("2026", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Year")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Donut chart
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(150.dp)
            ) {
                DonutChart(
                    values = listOf(45f, 25f, 15f, 15f),
                    colors = listOf(Color.Blue, orangeColor, Color.Green, Color.Yellow),
                    modifier = Modifier.fillMaxSize()
                )
                Text("22,870", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = Color.Blue, text = "United States")
                LegendItem(color = orangeColor, text = "Morocco")
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = Color.Green, text = "Pacific")
                LegendItem(color = Color.Yellow, text = "South Asia")
            }
            Spacer(modifier = Modifier.height(16.dp))

            // World Map
            Image(
                painter = painterResource(id = R.drawable.vector), // Placeholder
                contentDescription = "World Map",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Top Views Locations
            Text("Top Views Locations", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.Start)) {
                 Text("19,870", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                 Spacer(modifier = Modifier.width(8.dp))
                 Text("🇺🇸", fontSize = 24.sp) // US Flag emoji
            }
            Text("Our most customers in US", color = Color.Gray, modifier = Modifier.align(Alignment.Start))

            Spacer(modifier = Modifier.height(16.dp))
            // Customer size breakdown
            Column(modifier = Modifier.fillMaxWidth()) {
                LocationStatItem(color = Color.Blue, label = "Massive", value = "15.7k")
                LocationStatItem(color = orangeColor, label = "Large", value = "4.9k")
                LocationStatItem(color = Color.Yellow, label = "Medium", value = "2.4k")
                LocationStatItem(color = Color.LightGray, label = "Small", value = "980")
            }
        }
    }
}

@Composable
private fun GenderSplitCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Gender Split", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                        .background(themeColor)
                )
                Box(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .background(themeColor.copy(alpha = 0.4f))
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Male", color = Color.Gray)
                Text("Female", color = Color.Gray)
            }
        }
    }
}

@Composable
private fun AgeGroupCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Age Group", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Bar(0.9f, "18-24")
                Bar(0.7f, "25-34")
                Bar(0.8f, "35-44")
                Bar(0.4f, "45-60")
            }
        }
    }
}

@Composable
private fun RowScope.Bar(fraction: Float, label: String) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(fraction)
                .background(themeColor, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DonutChart(
    values: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 12.dp
) {
    val total = values.sum()
    var startAngle = -90f

    Canvas(modifier = modifier) {
        values.forEachIndexed { index, value ->
            val sweepAngle = (value / total) * 360f
            drawArc(
                color = colors[index],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun LocationStatItem(color: Color, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = Color.Gray)
        }
        Text(value, fontWeight = FontWeight.Bold)
    }
}


@Preview(showBackground = true)
@Composable
fun CampaignAnalyticsPagePreview() {
    CampaignAnalyticsPage(rememberNavController())
}
