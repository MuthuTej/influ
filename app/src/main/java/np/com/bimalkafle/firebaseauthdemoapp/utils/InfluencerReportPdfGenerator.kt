package np.com.bimalkafle.firebaseauthdemoapp.utils

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import np.com.bimalkafle.firebaseauthdemoapp.model.Collaboration
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders the influencer's earnings — one row per campaign collaborated on,
 * with the amount received from that campaign's brand, plus a grand total.
 * Mirrors [CampaignReportPdfGenerator] but inverted: one influencer, many
 * campaigns (vs. one campaign, many influencers), so it isn't reused as-is.
 */
object InfluencerReportPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val ROW_HEIGHT = 26f

    private fun rupees(amount: Int): String =
        "₹" + String.format(Locale.US, "%,d", amount)

    /** Amount actually paid for this collaboration, 0 until payment is confirmed.
     * `totalAmount` is only persisted once the real Pay Now flow runs — older/manual
     * collaborations can reach paymentStatus "paid" without it, so fall back to the
     * negotiated price sum, same convention as CampaignCollaborationSummary.amountPaid. */
    private fun Collaboration.amountReceived(): Int {
        if (paymentStatus != "paid") return 0
        return totalAmount?.toInt() ?: (pricing?.sumOf { it.price } ?: 0)
    }

    fun generate(collaborations: List<Collaboration>): ByteArray {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val metaPaint = Paint().apply { textSize = 10f; color = Color.DKGRAY }
        val headerPaint = Paint().apply { textSize = 12f; isFakeBoldText = true }
        val rowPaint = Paint().apply { textSize = 12f }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        val footerPaint = Paint().apply { textSize = 14f; isFakeBoldText = true }

        val campaignX = MARGIN
        val brandX = 230f
        val statusX = 380f
        val amountX = PAGE_WIDTH - MARGIN

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN + 10f

        fun rightAlignedText(text: String, rightEdge: Float, yPos: Float, paint: Paint) {
            canvas.drawText(text, rightEdge - paint.measureText(text), yPos, paint)
        }

        canvas.drawText("Earnings Report", MARGIN, y, titlePaint)
        y += 26f
        val generatedOn = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on $generatedOn", MARGIN, y, metaPaint)
        y += 20f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 20f

        fun drawTableHeader() {
            canvas.drawText("Campaign", campaignX, y, headerPaint)
            canvas.drawText("Brand", brandX, y, headerPaint)
            canvas.drawText("Status", statusX, y, headerPaint)
            canvas.drawText("Amount", amountX - headerPaint.measureText("Amount"), y, headerPaint)
            y += 10f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += ROW_HEIGHT - 10f
        }

        drawTableHeader()

        fun finishPageAndStartNew() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN + 10f
            drawTableHeader()
        }

        var grandTotal = 0

        if (collaborations.isEmpty()) {
            canvas.drawText("No campaigns yet.", campaignX, y, rowPaint)
            y += ROW_HEIGHT
        } else {
            for (collab in collaborations) {
                if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN - 30f) {
                    finishPageAndStartNew()
                }
                val amount = collab.amountReceived()
                canvas.drawText(collab.campaign.title, campaignX, y, rowPaint)
                canvas.drawText(collab.brand?.name ?: "Brand", brandX, y, rowPaint)
                canvas.drawText(collab.status, statusX, y, rowPaint)
                rightAlignedText(rupees(amount), amountX, y, rowPaint)
                grandTotal += amount
                y += ROW_HEIGHT
            }
        }

        if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) {
            finishPageAndStartNew()
        }
        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 26f
        canvas.drawText("Grand Total", campaignX, y, footerPaint)
        rightAlignedText(rupees(grandTotal), amountX, y, footerPaint)

        document.finishPage(page)

        val output = ByteArrayOutputStream()
        document.writeTo(output)
        document.close()
        return output.toByteArray()
    }
}
