package np.com.bimalkafle.firebaseauthdemoapp.utils

/** Compact "1.2K" / "2.4M" style formatting, shared by Brand and Influencer hero sections. */
fun formatCompactCount(count: Long): String {
    return when {
        count >= 1_000_000 -> "${String.format("%.1f", count / 1_000_000.0)}M"
        count >= 1_000 -> "${String.format("%.1f", count / 1_000.0)}K"
        else -> count.toString()
    }
}

fun formatCompactCount(count: Int): String = formatCompactCount(count.toLong())

/** Same compact formatting, with a ₹ prefix for currency amounts. */
fun formatCompactCurrency(amount: Double): String = "₹${formatCompactCount(amount.toLong())}"
