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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
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

// App theme colors
private val themeColor_campaign = Color(0xFFFF8383)
private val softGray = Color(0xFFF8F9FA)
private val darkerGray = Color(0xFF6C757D)
private val greenAccent = Color(0xFF28A745)

// Chart colors
private val pinkColor = Color(0xFFE84D8A)
private val tealColor = Color(0xFF4DE8E8)
private val yellowColor = Color(0xFFE8E84D)
private val blueColor = Color(0xFF4D7EE8)
private val orangeColor = Color(0xFFE89D4D)
private val pacificColor = Color(0xFF4DAAE8)
private val southAsiaColor = Color(0xFFE86B4D)

// Data classes for charts
private data class Point(val x: Float, val y: Float)
private data class BarData(val label: String, val before: Float, val after: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampaignAnalyticsPage(navController: NavController) {
    var selectedBottomNavItem by remember { mutableStateOf("Home") }
    Scaffold(
        topBar = { AnalyticsTopBar(navController) },
        containerColor = Color(0xFFF5F5F5),
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedBottomNavItem,
                onItemSelected = { selectedBottomNavItem = it },
                onCreateProposal = { navController.navigate("influencer_create_proposal") },
                navController = navController
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            OverviewHeader()
            Spacer(modifier = Modifier.height(16.dp))
            ViewersCard()
            Spacer(modifier = Modifier.height(16.dp))
            AnalyticsStatGrid()
            Spacer(modifier = Modifier.height(16.dp))
            PerformanceComparisonVisitsCard()
            Spacer(modifier = Modifier.height(16.dp))
            PerformanceComparisonSalesCard()
            Spacer(modifier = Modifier.height(16.dp))
            LocationAnalyticsCard()
            Spacer(modifier = Modifier.height(16.dp))
            OtherInfluencersCard()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsTopBar(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(themeColor_campaign)
    ) {
        Image(
            painter = painterResource(id = R.drawable.vector),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .alpha(0.2f),
            contentScale = ContentScale.Crop
        )
        Column {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text("Campaign Analytics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = Color.Transparent)
            )
            Spacer(modifier = Modifier.height(16.dp))
            AnalyticsProfileHeader()
            Spacer(modifier = Modifier.height(16.dp))

        }
    }
}

@Composable
fun AnalyticsProfileHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.brand_profile),
            contentDescription = "Alexa Rawles",
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("Alexa Rawles", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
            Text("alexarawles@gmail.com", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun OverviewHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Overview", fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("This Year", fontSize = 14.sp, color = darkerGray)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(themeColor_campaign)
            ) {
                Icon(Icons.Default.SaveAlt, contentDescription = "Save", tint = Color.White)
            }
        }
    }
}

@Composable
fun ViewersCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Viewers", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                DonutChart(
                    modifier = Modifier.fillMaxSize(),
                    values = listOf(40f, 30f, 30f),
                    colors = listOf(pinkColor, tealColor, yellowColor),
                    strokeWidth = 15.dp
                )
                Text("8k", fontWeight = FontWeight.Bold, fontSize = 30.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = pinkColor, text = "Instagram")
                LegendItem(color = tealColor, text = "X")
                LegendItem(color = yellowColor, text = "Youtube")
            }
        }
    }
}

@Composable
fun AnalyticsStatGrid() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnalyticsStat(
                "Impact", "180 %", "+2.5%",
                "Compared to (150% last month)",
                modifier = Modifier.weight(1f)
            )
            AnalyticsStat("Total", "150 K", "+0.5%", "(running)", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AnalyticsStat("Views", "7,265", "+2.5%", "+11.01 %", modifier = Modifier.weight(1f))
            AnalyticsStat("New users", "256", "+2.5%", "+15.03 %", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun AnalyticsStat(
    title: String,
    value: String,
    change: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(title, color = darkerGray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                change,
                color = greenAccent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = greenAccent, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtext, color = darkerGray, fontSize = 12.sp)
    }
}

@Composable
fun PerformanceComparisonVisitsCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Performance comparison(visits)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(color = blueColor, text = "Before collab")
                LegendItem(color = tealColor, text = "After collab")
            }
            Spacer(modifier = Modifier.height(16.dp))
            val lineData = listOf(
                Point(0f, 600f),
                Point(1f, 500f),
                Point(2f, 700f),
                Point(3f, 450f),
                Point(4f, 650f),
                Point(5f, 550f)
            )
            val lineData2 = listOf(
                Point(0f, 500f),
                Point(1f, 600f),
                Point(2f, 800f),
                Point(3f, 700f),
                Point(4f, 500f),
                Point(5f, 400f)
            )
            Box(modifier = Modifier.height(150.dp).fillMaxWidth()){
                LineChart(
                    modifier = Modifier.fillMaxSize(),
                    data = lineData,
                    lineColor = blueColor
                )
                LineChart(
                    modifier = Modifier.fillMaxSize(),
                    data = lineData2,
                    lineColor = tealColor,
                    showDashedLines = false
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CircularStat(progress = 0.68f, value = "68%", label = "Hit Rate this year", icon = Icons.Default.GpsFixed)
                CircularStat(progress = 0.76f, value = "76%", label = "Deals this year", icon = Icons.Default.BusinessCenter)
            }
        }
    }
}

@Composable
fun PerformanceComparisonSalesCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Performance comparison(sales)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(color = tealColor, text = "Before collab")
                LegendItem(color = blueColor, text = "After collab")
            }
            Spacer(modifier = Modifier.height(16.dp))
            val barData = listOf(
                BarData("Jan", 500f, 750f),
                BarData("Feb", 800f, 900f),
                BarData("Mar", 500f, 700f),
                BarData("Apr", 800f, 850f),
                BarData("May", 600f, 1000f),
                BarData("Jun", 400f, 950f)
            )
            BarChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                data = barData,
                barColors = Pair(tealColor, blueColor)
            )
        }
    }
}

@Composable
fun LocationAnalyticsCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                DonutChart(
                    modifier = Modifier.fillMaxSize(),
                    values = listOf(40f, 25f, 20f, 15f),
                    colors = listOf(blueColor, pacificColor, orangeColor, southAsiaColor),
                    strokeWidth = 20.dp
                )
                Text("22,870", fontWeight = FontWeight.Bold, fontSize = 30.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegendItem(color = blueColor, text = "United States")
                    LegendItem(color = pacificColor, text = "Pacific")
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LegendItem(color = orangeColor, text = "Morocco")
                    LegendItem(color = southAsiaColor, text = "South Asia")
                }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Top Views Locations", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("19,870", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🇺🇸", fontSize = 24.sp)
                }
                Text("Our most customers in US", color = darkerGray)
                Spacer(modifier = Modifier.height(16.dp))
                LocationStatItem(color = blueColor, label = "Massive", value = "15.7k")
                LocationStatItem(color = orangeColor, label = "Large", value = "4.9k")
                LocationStatItem(color = yellowColor, label = "Medium", value = "2.4k")
                LocationStatItem(color = Color.LightGray, label = "Small", value = "980")
            }
        }
    }
}

@Composable
fun OtherInfluencersCard() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Other influencers", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Icon(Icons.Default.MoreHoriz, contentDescription = "More options", tint = darkerGray)
        }
        Spacer(modifier = Modifier.height(16.dp))
        InfluencerItem("Francis Holzworth")
        InfluencerItem("Kaylyn Yokel")
        InfluencerItem("Kimberly Muro")
        InfluencerItem("Jack Sause")
        InfluencerItem("Rebekkah Lafantano")
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = { /* TODO */ }) {
            Text("VIEW MORE INFLUENCERS", color = themeColor_campaign, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InfluencerItem(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.brand_profile),
                contentDescription = name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
        Icon(Icons.Default.Email, contentDescription = "Contact", tint = darkerGray)
    }
}


// Embedded Charting Components

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = darkerGray)
    }
}

@Composable
fun CircularStat(progress: Float, value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = themeColor_campaign,
                trackColor = themeColor_campaign.copy(alpha = 0.2f),
                strokeWidth = 4.dp
            )
            Icon(icon, contentDescription = null, tint = themeColor_campaign, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(label, fontSize = 12.sp, color = darkerGray)
        }
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
private fun LineChart(
    modifier: Modifier = Modifier,
    data: List<Point>,
    lineColor: Color,
    showDashedLines: Boolean = true
) {
    Canvas(modifier = modifier) {
        val xMin = 0f
        val xMax = data.maxOf { it.x }
        val yMin = 0f
        val yMax = 1000f // Hardcoded for consistency with design

        val xRange = xMax - xMin
        val yRange = yMax - yMin

        val xScale = size.width / xRange
        val yScale = size.height / yRange

        if (showDashedLines) {
            (1..4).forEach { i ->
                val y = size.height - i * (size.height / 5)
                drawDashedLine(start = Offset(0f, y), end = Offset(size.width, y))
            }
        }

        val path = Path().apply {
            moveTo((data.first().x - xMin) * xScale, size.height - (data.first().y - yMin) * yScale)
            data.forEach { point ->
                lineTo((point.x - xMin) * xScale, size.height - (point.y - yMin) * yScale)
            }
        }

        drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx()))
    }
}

private fun DrawScope.drawDashedLine(
    start: Offset,
    end: Offset,
    color: Color = Color.LightGray,
    strokeWidth: Float = 1.dp.toPx(),
    pathEffect: PathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
) {
    drawPath(
        path = Path().apply { moveTo(start.x, start.y); lineTo(end.x, end.y) },
        color = color,
        style = Stroke(width = strokeWidth, pathEffect = pathEffect)
    )
}

@Composable
private fun BarChart(
    modifier: Modifier = Modifier,
    data: List<BarData>,
    barColors: Pair<Color, Color>
) {
    val yMax = 1000f // Hardcoded for consistency with design

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        data.forEach { barData ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(
                    modifier = Modifier.height(120.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                ) {
                    Bar(fraction = barData.before / yMax, color = barColors.first)
                    Bar(fraction = barData.after / yMax, color = barColors.second)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = barData.label, color = darkerGray, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun RowScope.Bar(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(fraction)
            .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
    )
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
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, color = darkerGray, fontSize = 14.sp)
        }
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}


@Preview(showBackground = true)
@Composable
fun CampaignAnalyticsPagePreview() {
    CampaignAnalyticsPage(rememberNavController())
}
