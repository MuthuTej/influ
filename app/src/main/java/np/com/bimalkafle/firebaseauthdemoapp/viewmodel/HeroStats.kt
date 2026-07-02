package np.com.bimalkafle.firebaseauthdemoapp.viewmodel

import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Real, computed hero-section numbers for the influencer home screen — derived
 * from the same `collaborations` list the screen already fetches, no extra
 * network calls. Replaces the previous hardcoded "₹18.4K / 6.1% / 1.2K / 2.4M".
 */
data class InfluencerHeroStats(
    val totalEarnings: Double,
    /** % change vs the prior 30-day window. Null when there's no prior-window
     * earnings to compare against (would otherwise be a meaningless +∞%). */
    val earningsTrendPercent: Double?,
    val awaitingResponseCount: Int,
    val activeCollaborationsCount: Int,
    val completedCount: Int
)

/** Same idea for the brand home screen. */
data class BrandHeroStats(
    val totalSpent: Double,
    val spendTrendPercent: Double?,
    val pendingApplicationsCount: Int,
    val activeCollaborationsCount: Int,
    val completedCount: Int
)

/** Shared "Performance · last 30 days" numbers — same shape for both roles. */
data class PerformanceStats(
    val views: Long,
    val engagementRatePercent: Double,
    val impressions: Long
)

/**
 * Falls back to the one-time overallAnalytics snapshot for collaborations the
 * daily content-performance sync hasn't reached yet (e.g. just submitted,
 * before the next cron run) — totalViewsDelivered is the up-to-date number
 * once it exists.
 */
private fun Collaboration.bestKnownViews(): Long {
    totalViewsDelivered?.let { return it.toLong() }
    val analytics = overallAnalytics
    return (analytics?.views ?: analytics?.impressions ?: 0).toLong()
}

/**
 * `totalAmount` is only persisted once a payment has actually been initiated
 * through the real Pay Now flow (createCollaborationPaymentOrder). Older/manually
 * progressed collaborations can reach paymentStatus "paid" without it ever being
 * written, which would otherwise sum to a misleading ₹0 — fall back to the same
 * pricing-sum the backend itself uses to compute totalAmount in the first place.
 */
private fun Collaboration.bestKnownAmount(): Double {
    totalAmount?.let { return it }
    return pricing?.sumOf { it.price.toDouble() } ?: 0.0
}

/** Days between this collaboration's last known activity and `now`, or null if
 * neither updatedAt nor createdAt parses as a real timestamp. */
private fun Collaboration.daysAgo(now: Instant): Long? {
    val ts = updatedAt.ifBlank { createdAt }
    return try {
        ChronoUnit.DAYS.between(Instant.parse(ts), now)
    } catch (e: Exception) {
        null
    }
}

/** Null (no trend shown) rather than a misleading +∞% when there's nothing in
 * the prior window to compare against. */
private fun trendPercent(current: Double, previous: Double): Double? {
    if (previous <= 0.0) return null
    return ((current - previous) / previous) * 100.0
}

fun computeInfluencerHeroStats(collaborations: List<Collaboration>): InfluencerHeroStats {
    val now = Instant.now()
    val paidWithAge = collaborations
        .filter { it.paymentStatus == "paid" }
        .map { it to (it.daysAgo(now) ?: Long.MAX_VALUE) }

    val totalEarnings = paidWithAge.sumOf { it.first.bestKnownAmount() }
    val currentWindowEarnings = paidWithAge.filter { it.second in 0..30 }.sumOf { it.first.bestKnownAmount() }
    val previousWindowEarnings = paidWithAge.filter { it.second in 31..60 }.sumOf { it.first.bestKnownAmount() }

    val awaitingResponseCount = collaborations.count { it.status == "PENDING" || it.status == "NEGOTIATION" }
    val activeCollaborationsCount = collaborations.count { it.status == "IN_PROGRESS" }
    val completedCount = collaborations.count { it.status == "COMPLETED" }

    return InfluencerHeroStats(
        totalEarnings = totalEarnings,
        earningsTrendPercent = trendPercent(currentWindowEarnings, previousWindowEarnings),
        awaitingResponseCount = awaitingResponseCount,
        activeCollaborationsCount = activeCollaborationsCount,
        completedCount = completedCount
    )
}

fun computeBrandHeroStats(collaborations: List<Collaboration>): BrandHeroStats {
    val now = Instant.now()
    val paidWithAge = collaborations
        .filter { it.paymentStatus == "paid" }
        .map { it to (it.daysAgo(now) ?: Long.MAX_VALUE) }

    val totalSpent = paidWithAge.sumOf { it.first.bestKnownAmount() }
    val currentWindowSpend = paidWithAge.filter { it.second in 0..30 }.sumOf { it.first.bestKnownAmount() }
    val previousWindowSpend = paidWithAge.filter { it.second in 31..60 }.sumOf { it.first.bestKnownAmount() }

    val pendingApplicationsCount = collaborations.count { it.status == "PENDING" && it.initiatedBy == "INFLUENCER" }
    val activeCollaborationsCount = collaborations.count { it.status == "IN_PROGRESS" }
    val completedCount = collaborations.count { it.status == "COMPLETED" }

    return BrandHeroStats(
        totalSpent = totalSpent,
        spendTrendPercent = trendPercent(currentWindowSpend, previousWindowSpend),
        pendingApplicationsCount = pendingApplicationsCount,
        activeCollaborationsCount = activeCollaborationsCount,
        completedCount = completedCount
    )
}

/**
 * Aggregates real content performance (views/engagement/impressions) across
 * whichever collaborations had activity in the last [windowDays] days — same
 * underlying fields (overallAnalytics, totalViewsDelivered) the analytics
 * dashboard already shows per-collaboration, just summed across the period.
 */
fun computePerformanceStats(collaborations: List<Collaboration>, windowDays: Long = 30): PerformanceStats {
    val now = Instant.now()
    val recent = collaborations.filter { (it.daysAgo(now) ?: Long.MAX_VALUE) <= windowDays }

    val views = recent.sumOf { it.bestKnownViews() }
    val impressions = recent.sumOf { (it.overallAnalytics?.impressions ?: it.overallAnalytics?.views ?: 0).toLong() }
    val likes = recent.sumOf { (it.overallAnalytics?.likes ?: 0).toLong() }
    val comments = recent.sumOf { (it.overallAnalytics?.comments ?: 0).toLong() }
    val shares = recent.sumOf { (it.overallAnalytics?.shares ?: 0).toLong() }
    val saves = recent.sumOf { (it.overallAnalytics?.saves ?: 0).toLong() }
    val engagementRate = if (views > 0) ((likes + comments + shares + saves).toDouble() / views) * 100.0 else 0.0

    return PerformanceStats(
        views = views,
        engagementRatePercent = engagementRate,
        impressions = impressions
    )
}
