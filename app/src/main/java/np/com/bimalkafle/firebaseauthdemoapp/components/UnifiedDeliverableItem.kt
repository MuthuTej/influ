package np.com.bimalkafle.firebaseauthdemoapp.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UnifiedDeliverableItem(
    deliverable: String,
    count: String,
    onCountChange: (String) -> Unit,
    price: String,
    onPriceChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = deliverable,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )

            CompactInput(
                label = "QTY",
                value = count,
                onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) onCountChange(it) },
                modifier = Modifier.width(60.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            CompactInput(
                label = "PRICE",
                value = price,
                onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) onPriceChange(it) },
                prefix = "₹",
                modifier = Modifier.width(85.dp)
            )
        }
    }
}

@Composable
fun CompactInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefix: String? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFB0B0C0),
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier
                .height(36.dp)
                .background(Color.White, RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFFF0F0F5), RoundedCornerShape(10.dp)),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                color = Color.DarkGray
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (prefix != null) {
                        Text(prefix, fontSize = 14.sp, color = Color(0xFFB0B0C0))
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    Box(contentAlignment = Alignment.Center) {
                        if (value.isEmpty()) {
                            Text("0", fontSize = 14.sp, color = Color(0xFFB0B0C0))
                        }
                        innerTextField()
                    }
                }
            }
        )
    }
}
