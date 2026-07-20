package np.com.bimalkafle.firebaseauthdemoapp.utils

import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration

/**
 * Same earnings data as [InfluencerReportPdfGenerator], as a flat CSV table —
 * one row per campaign collaborated on, plus a GRAND TOTAL row, so it opens
 * straight into Excel/Sheets and stays sortable/filterable there.
 */
object InfluencerReportCsvGenerator {

    private const val HEADER = "Campaign,Brand,Status,Amount Received"

    private fun csvEscape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value
    }

    private fun Collaboration.amountReceived(): Int {
        if (paymentStatus != "paid") return 0
        return totalAmount?.toInt() ?: (pricing?.sumOf { it.price } ?: 0)
    }

    fun generate(collaborations: List<Collaboration>): ByteArray {
        val sb = StringBuilder()
        sb.appendLine(HEADER)
        var grandTotal = 0
        for (collab in collaborations) {
            val amount = collab.amountReceived()
            val campaign = csvEscape(collab.campaign.title)
            val brand = csvEscape(collab.brand?.name ?: "Brand")
            sb.appendLine("$campaign,$brand,${collab.status},$amount")
            grandTotal += amount
        }
        sb.appendLine(",,GRAND TOTAL,$grandTotal")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }
}
