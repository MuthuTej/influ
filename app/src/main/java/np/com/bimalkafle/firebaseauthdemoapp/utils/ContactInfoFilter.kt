package np.com.bimalkafle.firebaseauthdemoapp.utils

/**
 * Best-effort detector for phone numbers, emails, and off-platform contact links
 * in chat messages. This exists to deter brands/influencers from exchanging direct
 * contact info to bypass the platform — it's a client-side UX deterrent, not a
 * security boundary (chat writes directly to Firestore, so a modified client or a
 * direct Firestore write can still bypass this; real enforcement needs a
 * server-side check, e.g. a Cloud Function trigger on message creation).
 */
object ContactInfoFilter {

    private val EMAIL_REGEX = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")

    // Deliberately narrow to common phone-number shapes (10 bare digits, +country
    // code, or dash/dot/space-grouped digits) rather than "7+ digits anywhere",
    // since this app's chat regularly contains prices like "₹100000" that would
    // otherwise false-positive as a phone number.
    private val PHONE_REGEX = Regex(
        """(\+\d{7,15})|(\b\d{10}\b)|(\b\d{3,4}[-.\s]\d{3,4}[-.\s]\d{3,4}\b)"""
    )

    private val CONTACT_LINK_REGEX = Regex(
        """(wa\.me/|whatsapp\.com/|t\.me/|telegram\.me/|instagram\.com/|facebook\.com/|m\.me/)""",
        RegexOption.IGNORE_CASE
    )

    private val CONTACT_PHRASE_REGEX = Regex(
        """\b(whatsapp\s*me|call\s*me|text\s*me|my\s*number|reach\s*me\s*at|contact\s*me\s*(at|on))\b""",
        RegexOption.IGNORE_CASE
    )

    /** Returns a user-facing reason if [text] looks like it contains contact info, else null. */
    fun detect(text: String): String? {
        return when {
            EMAIL_REGEX.containsMatchIn(text) -> "email addresses"
            PHONE_REGEX.containsMatchIn(text) -> "phone numbers"
            CONTACT_LINK_REGEX.containsMatchIn(text) -> "external contact links"
            CONTACT_PHRASE_REGEX.containsMatchIn(text) -> "off-platform contact requests"
            else -> null
        }
    }

    fun containsContactInfo(text: String): Boolean = detect(text) != null
}
