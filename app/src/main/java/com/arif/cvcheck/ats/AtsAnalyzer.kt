package com.arif.cvcheck.ats

import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Offline, rule-based ATS check. It estimates how well an applicant tracking system could
 * parse the CV and how well it reads to a recruiter. It is a heuristic, not a replica of any
 * specific vendor's parser.
 *
 * Score budget (100): Contact 10 · Sections 20 · Keywords 20 · Impact 20 · Formatting 15 ·
 * Content 10 · Readability 5.
 */
object AtsAnalyzer {

    // ---------- regexes ----------
    private val EMAIL = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val PHONE = Regex("(\\+?\\d[\\d\\s().-]{6,}\\d)")
    private val LINK = Regex(
        "(linkedin\\.com|github\\.com|behance\\.net|dribbble\\.com|gitlab\\.com|medium\\.com|https?://|www\\.)",
        RegexOption.IGNORE_CASE
    )
    private val LINKEDIN = Regex("linkedin\\.com", RegexOption.IGNORE_CASE)
    private const val MONTH = "(?:jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*\\.?"
    private val DATE_RANGE = Regex(
        "(?:$MONTH\\s+)?(?:19|20)\\d{2}\\s*(?:-|–|—|to)\\s*(?:(?:$MONTH\\s+)?(?:19|20)\\d{2}|present|current|now|ongoing)",
        RegexOption.IGNORE_CASE
    )
    private val YEAR = Regex("\\b(?:19|20)\\d{2}\\b")
    private val FIRST_PERSON_I = Regex("\\bI\\b(?!/)")
    private val FIRST_PERSON_OTHER = Regex("\\b(my|me|myself)\\b", RegexOption.IGNORE_CASE)
    private val BULLET_START = Regex("^\\s*[•·▪●◦■□➢►✓✔*\\-–—]\\s*")

    private val SECTION_ALIASES: Map<String, Set<String>> = mapOf(
        "experience" to setOf(
            "experience", "work experience", "professional experience", "employment history",
            "work history", "career history", "employment", "relevant experience", "internships"
        ),
        "education" to setOf("education", "academic background", "academic qualifications", "qualifications", "education & training"),
        "skills" to setOf(
            "skills", "technical skills", "core competencies", "key skills", "skills & tools",
            "tools & technologies", "technologies", "expertise", "core skills", "skills & expertise"
        ),
        "summary" to setOf(
            "summary", "professional summary", "profile", "professional profile", "objective",
            "career objective", "about me", "about", "executive summary"
        ),
        "extras" to setOf(
            "projects", "key projects", "selected projects", "certifications", "certificates", "awards",
            "achievements", "publications", "volunteer", "volunteering", "languages", "licenses & certifications"
        )
    )

    // ---------- public API ----------
    fun analyze(input: CvInput, jobDescription: String? = null): AtsReport {
        val text = input.text.replace(' ', ' ').replace("\r", "")
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val words = Regex("\\S+").findAll(text).count()
        val pages = input.pageCount ?: min(5, maxOf(1, ceil(words / 500.0).toInt()))
        val suggestions = mutableListOf<Suggestion>()

        val headings = detectSections(lines)
        val categories = listOf(
            contact(text, suggestions),
            sections(headings, suggestions),
            keywords(text, jobDescription, suggestions),
            impact(lines, headings, suggestions),
            formatting(input, lines, words, pages, suggestions),
            content(text, suggestions),
            readability(input, text, suggestions)
        )

        val kw = categories[2] as KeywordResult
        val scored = categories.map { it.category }
        val total = scored.sumOf { it.score }.coerceIn(0, 100)

        return AtsReport(
            total = total,
            verdict = verdict(total),
            categories = scored,
            suggestions = suggestions.sortedBy { it.priority.ordinal },
            matchedKeywords = kw.matched,
            missingKeywords = kw.missing,
            jobMatchPercent = kw.jobMatchPercent,
            wordCount = words,
            pageCount = pages
        )
    }

    fun verdict(score: Int): String = when {
        score >= 85 -> "Excellent — ATS-ready"
        score >= 70 -> "Good — a few fixes left"
        score >= 50 -> "Needs work"
        else -> "High risk of being filtered out"
    }

    // ---------- category plumbing ----------
    private interface Scored { val category: CategoryScore }
    private class Plain(override val category: CategoryScore) : Scored
    private class KeywordResult(
        override val category: CategoryScore,
        val matched: List<String>,
        val missing: List<String>,
        val jobMatchPercent: Int?
    ) : Scored

    private fun cat(name: String, max: Int, checks: List<Pair<Check, Int>>): CategoryScore {
        // each check carries its own point value; a passed check earns it
        val earned = checks.filter { it.first.passed }.sumOf { it.second }
        return CategoryScore(name, earned.coerceIn(0, max), max, checks.map { it.first })
    }

    // ---------- 1. Contact (10) ----------
    private fun contact(text: String, out: MutableList<Suggestion>): Scored {
        val hasEmail = EMAIL.containsMatchIn(text)
        val hasPhone = PHONE.findAll(text).any { m ->
            val digits = m.value.count { it.isDigit() }
            digits in 8..15 && !YEAR.matches(m.value.trim())
        }
        val hasLink = LINK.containsMatchIn(text)
        val hasLinkedIn = LINKEDIN.containsMatchIn(text)

        if (!hasEmail) out += Suggestion(Priority.HIGH, "Contact", "Add an email address",
            "Recruiters and ATS forms key off your email. Put it in the body of the page, not only in a header or footer.")
        if (!hasPhone) out += Suggestion(Priority.HIGH, "Contact", "Add a phone number",
            "Include a phone number with country code, e.g. +880 1XXX-XXXXXX.")
        if (!hasLink) out += Suggestion(Priority.MEDIUM, "Contact", "Add a LinkedIn or portfolio link",
            "A profile or portfolio URL lets recruiters verify your work quickly. Write the full URL as plain text.")

        return Plain(cat("Contact details", 10, listOf(
            Check("Email address found", hasEmail, if (hasEmail) "Found" else "Not found") to 4,
            Check("Phone number found", hasPhone, if (hasPhone) "Found" else "Not found") to 3,
            Check("LinkedIn / portfolio link", hasLink,
                if (hasLinkedIn) "LinkedIn found" else if (hasLink) "Link found" else "No link found") to 3
        )))
    }

    // ---------- 2. Sections (20) ----------
    private fun detectSections(lines: List<String>): List<Pair<Int, String>> {
        val found = mutableListOf<Pair<Int, String>>()
        lines.forEachIndexed { i, raw ->
            if (raw.length > 45) return@forEachIndexed
            val norm = raw.lowercase()
                .replace(Regex("[^a-z& ]"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (norm.isEmpty()) return@forEachIndexed
            for ((key, aliases) in SECTION_ALIASES) {
                if (norm in aliases) { found += i to key; break }
            }
        }
        return found
    }

    private fun sections(headings: List<Pair<Int, String>>, out: MutableList<Suggestion>): Scored {
        val keys = headings.map { it.second }.toSet()
        fun has(k: String) = k in keys

        if (!has("experience")) out += Suggestion(Priority.HIGH, "Sections", "Add a clear \"Work Experience\" heading",
            "ATS parsers look for standard headings. Avoid creative titles like \"My Journey\"; use \"Work Experience\".")
        if (!has("education")) out += Suggestion(Priority.HIGH, "Sections", "Add an \"Education\" section",
            "Even if you're experienced, list your degree(s) under a standard \"Education\" heading.")
        if (!has("skills")) out += Suggestion(Priority.HIGH, "Sections", "Add a dedicated \"Skills\" section",
            "A plain-text skills list is the easiest place for an ATS to pick up keywords.")
        if (!has("summary")) out += Suggestion(Priority.MEDIUM, "Sections", "Add a 2–3 line professional summary",
            "Lead with your title, years of experience and 2–3 headline strengths.")
        if (!has("extras")) out += Suggestion(Priority.LOW, "Sections", "Consider a Projects or Certifications section",
            "Extra proof of skills helps, especially for early-career or career-change CVs.")

        return Plain(cat("Section structure", 20, listOf(
            Check("Experience heading", has("experience"), if (has("experience")) "Found" else "Missing") to 6,
            Check("Education heading", has("education"), if (has("education")) "Found" else "Missing") to 5,
            Check("Skills heading", has("skills"), if (has("skills")) "Found" else "Missing") to 5,
            Check("Summary / profile", has("summary"), if (has("summary")) "Found" else "Missing") to 2,
            Check("Projects / certifications", has("extras"), if (has("extras")) "Found" else "Missing") to 2
        )))
    }

    // ---------- 3. Keywords (20) ----------
    private fun containsTerm(haystackLower: String, term: String): Boolean {
        val pattern = Regex("(?<![a-z0-9+#])" + Regex.escape(term) + "(?![a-z0-9+#])")
        return pattern.containsMatchIn(haystackLower)
    }

    private fun extractJobTerms(jd: String): List<String> {
        val lower = jd.lowercase()
        val terms = LinkedHashSet<String>()
        // known skills first (keeps multi-word terms such as "design system")
        SkillBank.skills.filter { containsTerm(lower, it) }.forEach { terms += it }
        // then frequent single tokens the skill bank doesn't know
        val freq = HashMap<String, Int>()
        Regex("[a-z][a-z0-9+#.]{2,}").findAll(lower).forEach { m ->
            val t = m.value.trimEnd('.')
            if (t.length >= 4 && t !in SkillBank.stopWords) freq[t] = (freq[t] ?: 0) + 1
        }
        freq.entries.filter { it.value >= 2 }
            .sortedByDescending { it.value }
            .map { it.key }
            .forEach { t -> if (terms.none { it == t || it.contains(t) }) terms += t }
        return terms.take(25)
    }

    private fun keywords(text: String, jd: String?, out: MutableList<Suggestion>): Scored {
        val lower = text.lowercase()
        val jobText = jd?.trim().orEmpty()

        if (jobText.length >= 40) {
            val terms = extractJobTerms(jobText)
            if (terms.isNotEmpty()) {
                val matched = terms.filter { containsTerm(lower, it) }
                val missing = terms - matched.toSet()
                val pct = (100.0 * matched.size / terms.size).roundToInt()
                val points = (20.0 * matched.size / terms.size).roundToInt()
                if (pct < 70 && missing.isNotEmpty()) {
                    out += Suggestion(
                        if (pct < 40) Priority.HIGH else Priority.MEDIUM, "Keywords",
                        "Work in the missing job keywords",
                        "Your CV matches $pct% of the job's key terms. Where it is truthful, add: " +
                            missing.take(8).joinToString(", ") + "."
                    )
                }
                return KeywordResult(
                    CategoryScore("Keyword match", points, 20, listOf(
                        Check("Job keywords covered", pct >= 70, "${matched.size} of ${terms.size} ($pct%)")
                    )),
                    matched, missing, pct
                )
            }
        }

        val matched = SkillBank.skills.filter { containsTerm(lower, it) }
        val n = matched.size
        val points = min(20, (20.0 * n / 12.0).roundToInt())
        if (n < 8) out += Suggestion(Priority.HIGH, "Keywords", "List more role-relevant skills",
            "Only $n recognised skills were found. Add the tools, methods and technologies you actually use, spelled the way job ads spell them.")
        else if (n < 12) out += Suggestion(Priority.MEDIUM, "Keywords", "Broaden your skills list",
            "You have $n recognised skills; 12+ gives an ATS more to match on.")
        out += Suggestion(Priority.LOW, "Keywords", "Tailor to a specific job",
            "Paste a job description on the start screen to see which keywords you are missing.")
        return KeywordResult(
            CategoryScore("Keywords", points, 20, listOf(
                Check("Recognised skills (12+ is ideal)", n >= 12, "$n found")
            )),
            matched, emptyList(), null
        )
    }

    // ---------- 4. Impact (20) ----------
    private fun stripBullet(s: String) = s.replace(BULLET_START, "").trim()

    private fun impact(
        lines: List<String>,
        headings: List<Pair<Int, String>>,
        out: MutableList<Suggestion>
    ): Scored {
        val headingIdx = headings.map { it.first }.toSet()
        val candidates = lines.withIndex()
            .filter { (i, l) -> i !in headingIdx }
            .map { (_, l) -> l to BULLET_START.containsMatchIn(l) }
            .map { (l, isBullet) -> stripBullet(l) to isBullet }
            .filter { (l, isBullet) ->
                val n = l.split(Regex("\\s+")).size
                (isBullet && n >= 4) || n >= 8
            }
            .map { it.first }

        if (candidates.size < 3) {
            out += Suggestion(Priority.HIGH, "Impact", "Describe your work in bullet points",
                "Few achievement lines were detected. Under each role, add 3–5 bullets that start with a verb and show a result.")
            return Plain(CategoryScore("Impact & achievements", 0, 20, listOf(
                Check("Quantified achievements", false, "Not enough bullet content to measure"),
                Check("Strong action verbs", false, "Not enough bullet content to measure")
            )))
        }

        val quantified = candidates.count { l -> l.replace(YEAR, "").any { it.isDigit() } }
        val verbs = candidates.count { l ->
            val first = l.split(Regex("\\s+")).first().lowercase().trim(',', '.', ':', ';')
            first in SkillBank.actionVerbs
        }
        val qRatio = quantified.toDouble() / candidates.size
        val vRatio = verbs.toDouble() / candidates.size

        val qPoints = (10.0 * min(1.0, qRatio / 0.30)).roundToInt()
        val vPoints = (10.0 * min(1.0, vRatio / 0.50)).roundToInt()
        val qPct = (qRatio * 100).roundToInt()
        val vPct = (vRatio * 100).roundToInt()

        if (qRatio < 0.30) out += Suggestion(
            if (qRatio < 0.12) Priority.HIGH else Priority.MEDIUM, "Impact", "Add numbers to your achievements",
            "Only $qPct% of your bullets include a number. Aim for 30%+: \"Cut load time by 40%\", \"Led a team of 6\", \"Managed a \$120k budget\".")
        if (vRatio < 0.50) out += Suggestion(
            if (vRatio < 0.20) Priority.HIGH else Priority.MEDIUM, "Impact", "Start bullets with strong action verbs",
            "$vPct% of your bullets open with a strong verb. Swap openings like \"Responsible for…\" for \"Led\", \"Built\", \"Launched\", \"Reduced\".")

        return Plain(CategoryScore("Impact & achievements", qPoints + vPoints, 20, listOf(
            Check("Bullets with numbers (30%+ ideal)", qRatio >= 0.30, "$quantified of ${candidates.size} ($qPct%)"),
            Check("Bullets starting with action verbs (50%+ ideal)", vRatio >= 0.50, "$verbs of ${candidates.size} ($vPct%)")
        )))
    }

    // ---------- 5. Formatting (15) ----------
    private fun formatting(
        input: CvInput,
        lines: List<String>,
        words: Int,
        pages: Int,
        out: MutableList<Suggestion>
    ): Scored {
        val lenPts = when (words) { in 350..900 -> 5; in 250..349, in 901..1200 -> 3; else -> 1 }
        if (words < 350) out += Suggestion(Priority.MEDIUM, "Formatting", "Add more detail",
            "At $words words your CV is thin. 400–800 words usually fits one to two pages and gives an ATS enough to work with.")
        if (words > 900) out += Suggestion(Priority.MEDIUM, "Formatting", "Trim your CV",
            "At $words words it is long. Cut roles older than 10–15 years and keep each role to 3–6 bullets.")

        val pagePts = when { pages <= 2 -> 3; pages == 3 -> 1; else -> 0 }
        if (pages > 2) out += Suggestion(Priority.MEDIUM, "Formatting", "Aim for two pages or fewer",
            "Your CV runs about $pages pages. Most recruiters scan the first page only.")

        val bulletLines = lines.count { BULLET_START.containsMatchIn(it) }
        val bulletPts = when { bulletLines >= 6 -> 3; bulletLines >= 2 -> 1; else -> 0 }
        if (bulletLines < 6) out += Suggestion(Priority.MEDIUM, "Formatting", "Use standard bullet points",
            "Only $bulletLines bulleted lines were detected. Use simple round bullets so achievements are parsed as separate items.")

        var layoutPts = 4
        val layoutIssues = mutableListOf<String>()
        if (input.hasTables) { layoutPts -= 2; layoutIssues += "tables" }
        if (input.hasTextBoxes) { layoutPts -= 1; layoutIssues += "text boxes" }
        if (input.hasImages) { layoutPts -= 1; layoutIssues += "images/graphics" }
        layoutPts = layoutPts.coerceAtLeast(0)
        if (layoutIssues.isNotEmpty()) out += Suggestion(Priority.HIGH, "Formatting", "Remove ${layoutIssues.joinToString(" and ")}",
            "ATS parsers often skip or scramble content inside tables, text boxes and graphics. Use a single-column layout with plain text; skip photos, icons and skill-rating bars.")

        return Plain(CategoryScore("Formatting & length", lenPts + pagePts + bulletPts + layoutPts, 15, listOf(
            Check("Length 350–900 words", words in 350..900, "$words words"),
            Check("Two pages or fewer", pages <= 2, "About $pages page(s)"),
            Check("Bullet points used", bulletLines >= 6, "$bulletLines bulleted lines"),
            Check("ATS-safe layout", layoutIssues.isEmpty(),
                if (layoutIssues.isEmpty()) "No tables, text boxes or graphics detected"
                else "Found ${layoutIssues.joinToString(", ")}")
        )))
    }

    // ---------- 6. Content quality (10) ----------
    private fun content(text: String, out: MutableList<Suggestion>): Scored {
        val lower = text.lowercase()
        val weakFound = SkillBank.weakPhrases.filter { lower.contains(it) }
        val weakCount = SkillBank.weakPhrases.sumOf { p -> Regex(Regex.escape(p)).findAll(lower).count() }
        val weakPts = when { weakCount <= 1 -> 4; weakCount <= 3 -> 2; else -> 0 }
        if (weakCount > 1) out += Suggestion(Priority.MEDIUM, "Content", "Replace vague phrases",
            "Found: ${weakFound.take(5).joinToString(", ") { "\"$it\"" }}. Describe what you did and the outcome instead.")

        val pronouns = FIRST_PERSON_I.findAll(text).count() + FIRST_PERSON_OTHER.findAll(text).count()
        val pronounPts = when { pronouns <= 2 -> 3; pronouns <= 5 -> 1; else -> 0 }
        if (pronouns > 2) out += Suggestion(Priority.LOW, "Content", "Drop first-person pronouns",
            "Found $pronouns uses of I / my / me. CVs read stronger in implied first person: \"Led…\" not \"I led…\".")

        val dateRanges = DATE_RANGE.findAll(text).count()
        val datePts = when { dateRanges >= 2 -> 3; dateRanges == 1 -> 1; else -> 0 }
        if (dateRanges < 2) out += Suggestion(Priority.HIGH, "Content", "Show dates for each role",
            "Found $dateRanges date range(s). Use a consistent format such as \"Jan 2022 – Present\" so an ATS can calculate your experience.")

        return Plain(CategoryScore("Content quality", weakPts + pronounPts + datePts, 10, listOf(
            Check("Few vague phrases", weakCount <= 1, "$weakCount found"),
            Check("No first-person pronouns", pronouns <= 2, "$pronouns found"),
            Check("Dated roles (2+ date ranges)", dateRanges >= 2, "$dateRanges found")
        )))
    }

    // ---------- 7. Readability / parseability (5) ----------
    private fun readability(input: CvInput, text: String, out: MutableList<Suggestion>): Scored {
        val garbled = text.count { it == '�' || (it.isISOControl() && it != '\n' && it != '\t') }
        val garbledRatio = if (text.isEmpty()) 1.0 else garbled.toDouble() / text.length
        val textOk = text.length >= 200 && garbledRatio < 0.02

        val pts = when {
            !textOk -> 0
            input.fromImageOrScan -> 2
            else -> 5
        }
        if (text.length < 200) out += Suggestion(Priority.HIGH, "Readability", "Could not read enough text",
            "Almost no text could be extracted. Export your CV as a text-based PDF or DOCX rather than a scan or screenshot.")
        else if (garbledRatio >= 0.02) out += Suggestion(Priority.HIGH, "Readability", "Text extracts with errors",
            "Unreadable characters were found, often caused by decorative fonts or outlined text. Use a standard font and re-export.")
        if (input.fromImageOrScan && textOk) out += Suggestion(Priority.HIGH, "Readability", "Upload a text-based file to employers",
            "This CV was read with OCR because it is an image or scan. Most ATS cannot read images. Submit a text-based PDF or DOCX.")

        return Plain(CategoryScore("Machine readability", pts, 5, listOf(
            Check("Text can be extracted cleanly", textOk, if (textOk) "Clean" else "Poor extraction"),
            Check("Text-based file (not a scan or image)", !input.fromImageOrScan,
                if (input.fromImageOrScan) "Read via OCR" else "Native text")
        )))
    }
}
