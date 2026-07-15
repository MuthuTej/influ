package np.com.bimalkafle.firebaseauthdemoapp.utils

import np.com.bimalkafle.firebaseauthdemoapp.model.Campaign
import np.com.bimalkafle.firebaseauthdemoapp.model.CampaignDetail

/**
 * Same payment-summary data as [CampaignReportPdfGenerator], as a flat CSV
 * table instead — one row per influencer, plus a TOTAL/GRAND TOTAL row, so it
 * opens straight into Excel/Sheets and stays sortable/filterable there.
 */
object CampaignReportCsvGenerator {

    private const val HEADER = "Campaign,Budget Min,Budget Max,Influencer,Status,Amount Paid"

    private fun csvEscape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value
    }

    fun generate(campaign: CampaignDetail): ByteArray {
        val sb = StringBuilder()
        sb.appendLine(HEADER)
        val collaborations = campaign.collaborations ?: emptyList()
        var total = 0
        val title = csvEscape(campaign.title)
        val budgetMin = campaign.budgetMin?.toString() ?: ""
        val budgetMax = campaign.budgetMax?.toString() ?: ""
        if (collaborations.isEmpty()) {
            sb.appendLine("$title,$budgetMin,$budgetMax,,,")
        } else {
            for (collab in collaborations) {
                val name = csvEscape(collab.influencerName ?: collab.influencerHandle ?: "Influencer")
                sb.appendLine("$title,$budgetMin,$budgetMax,$name,${collab.status},${collab.amountPaid}")
                total += collab.amountPaid
            }
        }
        sb.appendLine(",,,,TOTAL,$total")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    fun generateOverall(campaigns: List<Campaign>): ByteArray {
        val sb = StringBuilder()
        sb.appendLine(HEADER)
        var grandTotal = 0
        for (campaign in campaigns) {
            val collaborations = campaign.collaborations ?: emptyList()
            val title = csvEscape(campaign.title)
            val budgetMin = campaign.budgetMin?.toString() ?: ""
            val budgetMax = campaign.budgetMax?.toString() ?: ""
            var campaignTotal = 0
            if (collaborations.isEmpty()) {
                sb.appendLine("$title,$budgetMin,$budgetMax,,,")
            } else {
                for (collab in collaborations) {
                    val name = csvEscape(collab.influencerName ?: collab.influencerHandle ?: "Influencer")
                    sb.appendLine("$title,$budgetMin,$budgetMax,$name,${collab.status},${collab.amountPaid}")
                    campaignTotal += collab.amountPaid
                }
            }
            sb.appendLine("$title,,,,TOTAL,$campaignTotal")
            grandTotal += campaignTotal
        }
        sb.appendLine(",,,,GRAND TOTAL,$grandTotal")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }
}
