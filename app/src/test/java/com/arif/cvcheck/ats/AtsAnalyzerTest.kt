package com.arif.cvcheck.ats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AtsAnalyzerTest {

    private val strongCv = """
        Jane Doe
        jane.doe@example.com | +1 415 555 0134 | linkedin.com/in/janedoe | San Francisco, CA

        PROFESSIONAL SUMMARY
        Senior product designer with 7 years of experience building SaaS dashboards and design systems.

        WORK EXPERIENCE
        Senior Product Designer — Acme Cloud                                  Jan 2021 – Present
        • Led redesign of the analytics dashboard, increasing weekly active users by 32%
        • Built a design system in Figma used by 14 designers and 40 engineers
        • Reduced onboarding drop-off by 18% through user research and usability testing
        • Launched 5 enterprise features with agile squads using Jira and Confluence
        • Mentored 3 junior designers and ran 12 design thinking workshops
        Product Designer — Beta Labs                                          Mar 2018 – Dec 2020
        • Designed responsive web UI for an ERP product serving 2,000 customers
        • Improved accessibility to WCAG standards, cutting support tickets by 25%
        • Created wireframes and prototypes for 20+ features
        • Partnered with product managers on roadmap and stakeholder management
        • Delivered a typography and branding refresh across 6 products

        EDUCATION
        BSc in Computer Science, State University, 2018

        SKILLS
        Figma, UX, UI, design systems, prototyping, user research, usability testing, HTML, CSS, JavaScript, agile, accessibility

        CERTIFICATIONS
        Google UX Design Certificate, 2019
    """.trimIndent()

    private val weakCv = """
        John Smith
        I am a hard working person. I am responsible for various tasks. I worked on many things and helped with stuff.
        My team player attitude is great. References available upon request.
        Worked at a company for a while doing things. I did my best and my manager liked me.
        This is a long paragraph of text without any bullets or structure, repeated to reach a reasonable length so extraction is considered clean enough to evaluate sections and so on.
    """.trimIndent()

    @Test
    fun strongCvScoresHigh() {
        val r = AtsAnalyzer.analyze(CvInput(strongCv, pageCount = 2))
        assertTrue("expected >= 75 but was ${r.total}", r.total >= 75)
        assertEquals(7, r.categories.size)
        assertEquals(100, r.categories.sumOf { it.max })
    }

    @Test
    fun weakCvScoresLowAndGetsHighPrioritySuggestions() {
        val r = AtsAnalyzer.analyze(CvInput(weakCv, pageCount = 1))
        assertTrue("expected < 40 but was ${r.total}", r.total < 40)
        assertTrue(r.suggestions.any { it.priority == Priority.HIGH })
        assertTrue(r.suggestions.first().priority == Priority.HIGH)
    }

    @Test
    fun imageBasedCvIsPenalised() {
        val native = AtsAnalyzer.analyze(CvInput(strongCv, pageCount = 2))
        val scan = AtsAnalyzer.analyze(CvInput(strongCv, pageCount = 2, fromImageOrScan = true))
        assertTrue(scan.total < native.total)
        assertTrue(scan.suggestions.any { it.category == "Readability" })
    }

    @Test
    fun tablesAndGraphicsAreFlagged() {
        val r = AtsAnalyzer.analyze(CvInput(strongCv, pageCount = 2, hasTables = true, hasImages = true))
        assertTrue(r.suggestions.any { it.title.startsWith("Remove") })
    }

    @Test
    fun jobDescriptionDrivesKeywordMatch() {
        val jd = """
            We are hiring a product designer to own our design system and dashboard work in Figma.
            You will run user research, usability testing and prototyping, and partner on the roadmap.
            Experience with Kubernetes and GraphQL is a plus. Design system ownership is key.
        """.trimIndent()
        val r = AtsAnalyzer.analyze(CvInput(strongCv, pageCount = 2), jd)
        assertNotNull(r.jobMatchPercent)
        assertTrue(r.matchedKeywords.contains("figma"))
        assertTrue(r.missingKeywords.contains("kubernetes"))
        assertTrue(r.missingKeywords.contains("graphql"))
    }

    @Test
    fun emptyTextDoesNotCrash() {
        val r = AtsAnalyzer.analyze(CvInput("", fromImageOrScan = true))
        assertTrue(r.total in 0..100)
        assertTrue(r.suggestions.any { it.title.contains("Could not read") })
    }
}
