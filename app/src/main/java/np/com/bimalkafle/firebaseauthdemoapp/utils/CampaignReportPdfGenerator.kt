package np.com.bimalkafle.firebaseauthdemoapp.utils

import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import np.com.bimalkafle.firebaseauthdemoapp.model.Campaign
import np.com.bimalkafle.firebaseauthdemoapp.model.CampaignCollaborationSummary
import np.com.bimalkafle.firebaseauthdemoapp.model.CampaignDetail
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Renders campaign payment summaries (campaign name, budget, and each
 * influencer's amount paid) as PDF bytes using the platform's built-in
 * [PdfDocument] — no third-party library needed for a table this simple.
 * Callers (see [ReportFileSaver]) decide whether the bytes get saved to
 * Downloads or handed off through the share sheet.
 */
object CampaignReportPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 at 72dpi
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val ROW_HEIGHT = 26f

    private fun rupees(amount: Int): String =
        "₹" + String.format(Locale.US, "%,d", amount)

    /** Sanitized file name (no extension) for a single-campaign report. */
    fun fileBaseName(campaign: CampaignDetail): String {
        val name = campaign.title
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "campaign" }
        return "${name}_report"
    }

    /** Builds a one-campaign report PDF and returns its raw bytes. */
    fun generate(campaign: CampaignDetail): ByteArray {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val subPaint = Paint().apply { textSize = 12f; color = Color.DKGRAY }
        val headerPaint = Paint().apply { textSize = 12f; isFakeBoldText = true }
        val rowPaint = Paint().apply { textSize = 12f }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        val footerPaint = Paint().apply { textSize = 12f; isFakeBoldText = true }

        val influencerX = MARGIN
        val statusX = 300f
        val amountX = PAGE_WIDTH - MARGIN

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN + 10f

        canvas.drawText("Campaign Report", MARGIN, y, titlePaint)
        y += 26f
        canvas.drawText(campaign.title, MARGIN, y, subPaint.apply { isFakeBoldText = true; textSize = 14f })
        y += 20f

        val budgetText = if (campaign.budgetMin != null || campaign.budgetMax != null) {
            "Budget: ${rupees(campaign.budgetMin ?: 0)} - ${rupees(campaign.budgetMax ?: 0)}"
        } else "Budget: —"
        canvas.drawText(budgetText, MARGIN, y, subPaint.apply { isFakeBoldText = false; textSize = 12f })
        y += 18f

        val generatedOn = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on $generatedOn", MARGIN, y, subPaint.apply { textSize = 10f })
        y += 20f

        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 20f

        fun drawTableHeader() {
            canvas.drawText("Influencer", influencerX, y, headerPaint)
            canvas.drawText("Status", statusX, y, headerPaint)
            canvas.drawText("Amount Paid", amountX - headerPaint.measureText("Amount Paid"), y, headerPaint)
            y += 10f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += ROW_HEIGHT - 10f
        }

        drawTableHeader()

        val collaborations = campaign.collaborations ?: emptyList()
        var totalPaid = 0

        fun finishPageAndStartNew() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN + 10f
            drawTableHeader()
        }

        if (collaborations.isEmpty()) {
            canvas.drawText("No collaborations yet for this campaign.", influencerX, y, rowPaint)
            y += ROW_HEIGHT
        } else {
            for (collab: CampaignCollaborationSummary in collaborations) {
                if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN - 30f) {
                    finishPageAndStartNew()
                }
                val name = collab.influencerName ?: collab.influencerHandle ?: "Influencer"
                val amountText = rupees(collab.amountPaid)
                canvas.drawText(name, influencerX, y, rowPaint)
                canvas.drawText(collab.status, statusX, y, rowPaint)
                canvas.drawText(amountText, amountX - rowPaint.measureText(amountText), y, rowPaint)
                totalPaid += collab.amountPaid
                y += ROW_HEIGHT
            }
        }

        if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) {
            finishPageAndStartNew()
        }
        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 22f
        val totalText = rupees(totalPaid)
        canvas.drawText("Total paid", influencerX, y, footerPaint)
        canvas.drawText(totalText, amountX - footerPaint.measureText(totalText), y, footerPaint)

        document.finishPage(page)

        val output = ByteArrayOutputStream()
        document.writeTo(output)
        document.close()
        return output.toByteArray()
    }

    /** Builds one combined PDF covering every campaign: each campaign's budget,
     * every influencer's amount paid under it, a per-campaign subtotal, and a
     * grand total across all campaigns at the end. Returns raw PDF bytes. */
    fun generateOverall(campaigns: List<Campaign>): ByteArray {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val metaPaint = Paint().apply { textSize = 10f; color = Color.DKGRAY }
        val campaignPaint = Paint().apply { textSize = 13f; isFakeBoldText = true }
        val budgetPaint = Paint().apply { textSize = 12f; color = Color.DKGRAY }
        val rowPaint = Paint().apply { textSize = 12f }
        val subtotalPaint = Paint().apply { textSize = 12f; isFakeBoldText = true }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        val grandTotalPaint = Paint().apply { textSize = 14f; isFakeBoldText = true }

        val labelX = MARGIN
        val indentedX = MARGIN + 16f
        val amountX = PAGE_WIDTH - MARGIN

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN + 10f

        fun newPage() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN + 10f
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > PAGE_HEIGHT - MARGIN) newPage()
        }

        fun rightAlignedText(text: String, rightEdge: Float, yPos: Float, paint: Paint) {
            canvas.drawText(text, rightEdge - paint.measureText(text), yPos, paint)
        }

        canvas.drawText("All Campaigns Report", MARGIN, y, titlePaint)
        y += 22f
        val generatedOn = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on $generatedOn", MARGIN, y, metaPaint)
        y += 20f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 24f

        var grandTotal = 0

        if (campaigns.isEmpty()) {
            canvas.drawText("No campaigns yet.", labelX, y, rowPaint)
        }

        for (campaign in campaigns) {
            val collaborations = campaign.collaborations ?: emptyList()
            // Campaign header + its rows + subtotal, kept together where possible.
            ensureSpace(ROW_HEIGHT * (collaborations.size + 2) + 10f)

            canvas.drawText(campaign.title, labelX, y, campaignPaint)
            val budgetText = if (campaign.budgetMin != null || campaign.budgetMax != null) {
                "Budget: ${rupees(campaign.budgetMin ?: 0)} - ${rupees(campaign.budgetMax ?: 0)}"
            } else "Budget: —"
            rightAlignedText(budgetText, amountX, y, budgetPaint)
            y += ROW_HEIGHT

            var campaignTotal = 0
            if (collaborations.isEmpty()) {
                canvas.drawText("No collaborations yet.", indentedX, y, rowPaint.apply { color = Color.GRAY })
                rowPaint.color = Color.BLACK
                y += ROW_HEIGHT
            } else {
                for (collab in collaborations) {
                    ensureSpace(ROW_HEIGHT)
                    val name = collab.influencerName ?: collab.influencerHandle ?: "Influencer"
                    canvas.drawText(name, indentedX, y, rowPaint)
                    rightAlignedText(rupees(collab.amountPaid), amountX, y, rowPaint)
                    campaignTotal += collab.amountPaid
                    y += ROW_HEIGHT
                }
            }
            grandTotal += campaignTotal

            ensureSpace(ROW_HEIGHT)
            canvas.drawText("Total", indentedX, y, subtotalPaint)
            rightAlignedText(rupees(campaignTotal), amountX, y, subtotalPaint)
            y += ROW_HEIGHT + 10f
        }

        ensureSpace(ROW_HEIGHT + 20f)
        y += 6f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 26f
        canvas.drawText("Grand Total", labelX, y, grandTotalPaint)
        rightAlignedText(rupees(grandTotal), amountX, y, grandTotalPaint)

        document.finishPage(page)

        val output = ByteArrayOutputStream()
        document.writeTo(output)
        document.close()
        return output.toByteArray()
    }
}
