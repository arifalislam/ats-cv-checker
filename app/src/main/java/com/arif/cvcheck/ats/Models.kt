package com.arif.cvcheck.ats

/** What the extractor hands to the analyzer. No Android types so it can be unit tested on the JVM. */
data class CvInput(
    val text: String,
    val pageCount: Int? = null,
    val fromImageOrScan: Boolean = false,
    val hasTables: Boolean = false,
    val hasTextBoxes: Boolean = false,
    val hasImages: Boolean = false
)

enum class Priority { HIGH, MEDIUM, LOW }

data class Check(val label: String, val passed: Boolean, val detail: String)

data class Suggestion(
    val priority: Priority,
    val category: String,
    val title: String,
    val detail: String
)

data class CategoryScore(
    val name: String,
    val score: Int,
    val max: Int,
    val checks: List<Check>
) {
    val fraction: Float get() = if (max == 0) 0f else score.toFloat() / max
}

data class AtsReport(
    val total: Int,
    val verdict: String,
    val categories: List<CategoryScore>,
    val suggestions: List<Suggestion>,
    val matchedKeywords: List<String>,
    val missingKeywords: List<String>,
    /** Percentage of job-description keywords found, or null if no job description was supplied. */
    val jobMatchPercent: Int?,
    val wordCount: Int,
    val pageCount: Int
)
