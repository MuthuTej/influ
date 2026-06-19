package np.com.bimalkafle.firebaseauthdemoapp.viewmodel

import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration

/**
 * Real, computed hero-section numbers for the influencer home screen — derived
 * from the same `collaborations` list the screen already fetches, no extra
 * network calls. Replaces the previous hardcoded "₹18.4K / 6.1% / 1.2K / 2.4M".
 */
data class InfluencerHeroStats(
    val totalEarnings: Double,
    val awaitingResponseCount: Int,
    val activeCollaborationsCount: Int,
    val totalViewsGenerated: Long
)

/** Same idea for the brand home screen. */
data class BrandHeroStats(
    val totalReachDelivered: Long,
    val reachGrowth: Long,
    val pendingApplicationsCount: Int,
    val activeCollaborationsCount: Int,
    val totalSpent: Double
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

fun computeInfluencerHeroStats(collaborations: List<Collaboration>): InfluencerHeroStats {
    val totalEarnings = collaborations
        .filter { it.paymentStatus == "paid" }
        .sumOf { it.bestKnownAmount() }

    val awaitingResponseCount = collaborations.count { it.status == "PENDING" || it.status == "NEGOTIATION" }
    val activeCollaborationsCount = collaborations.count { it.status == "IN_PROGRESS" }
    val totalViewsGenerated = collaborations.sumOf { it.bestKnownViews() }

    return InfluencerHeroStats(
        totalEarnings = totalEarnings,
        awaitingResponseCount = awaitingResponseCount,
        activeCollaborationsCount = activeCollaborationsCount,
        totalViewsGenerated = totalViewsGenerated
    )
}

fun computeBrandHeroStats(collaborations: List<Collaboration>): BrandHeroStats {
    val totalReachDelivered = collaborations.sumOf { it.bestKnownViews() }
    val reachGrowth = collaborations.sumOf { (it.viewsGrowthSincePosting ?: 0).toLong() }

    val pendingApplicationsCount = collaborations.count { it.status == "PENDING" && it.initiatedBy == "INFLUENCER" }
    val activeCollaborationsCount = collaborations.count { it.status == "IN_PROGRESS" }
    val totalSpent = collaborations
        .filter { it.paymentStatus == "paid" }
        .sumOf { it.bestKnownAmount() }

    return BrandHeroStats(
        totalReachDelivered = totalReachDelivered,
        reachGrowth = reachGrowth,
        pendingApplicationsCount = pendingApplicationsCount,
        activeCollaborationsCount = activeCollaborationsCount,
        totalSpent = totalSpent
    )
}
