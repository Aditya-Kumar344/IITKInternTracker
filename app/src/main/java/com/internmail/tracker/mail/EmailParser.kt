package com.internmail.tracker.mail

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

data class ParsedInternshipMail(
    val companyName: String,
    val deadlineEpochMillis: Long?,
    val link: String?
)

/**
 * Rule-based (no network / no AI-call needed) detector + extractor for
 * "internship opportunity" emails. Tuned for typical placement-cell /
 * recruiter / careers-page style emails. Not perfect — but easy to extend:
 * add more keywords / patterns below as you see emails it misses.
 */
object EmailParser {

    // Keywords that suggest the mail is about an internship / off-campus-intern opportunity.
    private val INTERNSHIP_KEYWORDS = listOf(
        "internship", "intern role", "summer intern", "winter intern",
        "intern opportunity", "intern hiring", "hiring interns",
        "internship opportunity", "internship program", "intern position",
        "apply for internship", "intern opening"
    )

    // Words that strongly suggest a call-for-application (helps avoid false positives
    // like newsletters that merely mention the word "intern" in passing).
    private val ACTION_KEYWORDS = listOf(
        "apply by", "apply before", "last date", "deadline", "closes on",
        "closing date", "apply now", "register by", "submit by", "due by",
        "application deadline", "last date to apply"
    )

    private val SUBJECT_REJECT_KEYWORDS = listOf(
        "unsubscribe digest", "newsletter roundup"
    )

    // e.g. "Apply by 12 August 2026", "Deadline: 12-08-2026", "Last date: August 12, 2026"
    private val DATE_PATTERNS: List<Pair<Pattern, SimpleDateFormat>> = listOf(
        Pattern.compile("""\b(\d{1,2})(st|nd|rd|th)?\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[a-z]*\s+(\d{4})\b""", Pattern.CASE_INSENSITIVE)
            to SimpleDateFormat("d MMM yyyy", Locale.ENGLISH),
        Pattern.compile("""\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[a-z]*\s+(\d{1,2}),?\s+(\d{4})\b""", Pattern.CASE_INSENSITIVE)
            to SimpleDateFormat("MMM d yyyy", Locale.ENGLISH),
        Pattern.compile("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{4})\b""")
            to SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
        Pattern.compile("""\b(\d{4})-(\d{1,2})-(\d{1,2})\b""")
            to SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    )

    private val URL_PATTERN = Pattern.compile("""https?://[^\s"'<>)]+""")

    /**
     * Returns a ParsedInternshipMail if [subject] + [body] look like a genuine
     * internship opportunity email with a call to action, otherwise null.
     */
    fun tryParse(subject: String, body: String, fromAddress: String, fromName: String?): ParsedInternshipMail? {
        val haystack = (subject + " \n" + body).lowercase(Locale.ENGLISH)

        val subjectLower = subject.lowercase(Locale.ENGLISH)
        if (SUBJECT_REJECT_KEYWORDS.any { subjectLower.contains(it) }) return null

        val hasInternshipKeyword = INTERNSHIP_KEYWORDS.any { haystack.contains(it) }
        if (!hasInternshipKeyword) return null

        val hasActionKeyword = ACTION_KEYWORDS.any { haystack.contains(it) }
        // Require SOME call-to-action / deadline language to reduce false positives
        // from generic internship-related discussion emails.
        if (!hasActionKeyword) return null

        val company = extractCompanyName(subject, body, fromAddress, fromName)
        val deadline = extractDeadline(subject + "\n" + body)
        val link = extractLink(body)

        return ParsedInternshipMail(
            companyName = company,
            deadlineEpochMillis = deadline,
            link = link
        )
    }

    private fun extractCompanyName(subject: String, body: String, fromAddress: String, fromName: String?): String {
        // 1. Try "at <Company>" or "from <Company>" or "- <Company>" patterns in subject.
        val atPattern = Pattern.compile("""(?:internship\s*(?:at|@|-)\s*)([A-Z][A-Za-z0-9&.,\- ]{1,40})""", Pattern.CASE_INSENSITIVE)
        val m = atPattern.matcher(subject)
        if (m.find()) {
            return m.group(1)?.trim()?.trimEnd('.', ',') ?: fallbackCompanyFromEmail(fromAddress, fromName)
        }

        // 2. Try sender display name (often "Careers @ Company" or "Company Recruitment").
        if (!fromName.isNullOrBlank()) {
            val cleaned = fromName
                .replace(Regex("(?i)careers?|recruit(ment|ing|er)?|hr team|talent (acquisition|team)|no-?reply"), "")
                .trim(' ', '-', '|', '@')
            if (cleaned.length in 2..40) return cleaned
        }

        return fallbackCompanyFromEmail(fromAddress, fromName)
    }

    private fun fallbackCompanyFromEmail(fromAddress: String, fromName: String?): String {
        val domain = fromAddress.substringAfter('@', "").substringBefore('.')
        return if (domain.isNotBlank()) domain.replaceFirstChar { it.uppercase() }
        else fromName ?: "Unknown Company"
    }

    private fun extractDeadline(text: String): Long? {
        for ((pattern, format) in DATE_PATTERNS) {
            val m = pattern.matcher(text)
            if (m.find()) {
                val matched = m.group()
                try {
                    val normalized = normalizeForFormat(matched)
                    val date = format.parse(normalized) ?: continue
                    val cal = Calendar.getInstance()
                    cal.time = date
                    // Snap to end of that day (23:59:59) so "deadline day" logic includes the whole day.
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    return cal.timeInMillis
                } catch (e: Exception) {
                    // try next pattern
                }
            }
        }
        return null
    }

    private fun normalizeForFormat(raw: String): String {
        // Strip ordinal suffixes (1st, 2nd, 3rd, 4th) and normalize "Sept" -> "Sep"
        return raw
            .replace(Regex("(?i)(\\d+)(st|nd|rd|th)"), "$1")
            .replace(Regex("(?i)sept\\b"), "Sep")
    }

    private fun extractLink(body: String): String? {
        val m = URL_PATTERN.matcher(body)
        // Prefer a link that looks like an application/careers/job link.
        var fallback: String? = null
        while (m.find()) {
            val url = m.group()
            if (fallback == null) fallback = url
            val lower = url.lowercase(Locale.ENGLISH)
            if (lower.contains("apply") || lower.contains("career") || lower.contains("job") || lower.contains("intern")) {
                return url
            }
        }
        return fallback
    }
}
