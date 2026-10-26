package com.arif.cvcheck.ats

/** Vocabulary used for keyword detection. Lower-case; matched on word boundaries. */
object SkillBank {

    val skills: List<String> = listOf(
        // Software & data
        "python", "java", "javascript", "typescript", "kotlin", "swift", "c++", "c#", "go", "rust", "php", "ruby",
        "sql", "mysql", "postgresql", "mongodb", "redis", "firebase", "graphql", "rest api", "restful api", "api",
        "react", "next.js", "vue.js", "vue", "nuxt.js", "angular", "node.js", "express", "django", "flask", "spring",
        "html", "css", "sass", "tailwind", "bootstrap", "gsap", "webpack", "git", "github", "docker", "kubernetes",
        "aws", "azure", "gcp", "ci/cd", "linux", "android", "ios", "flutter", "react native", "jira", "confluence",
        "machine learning", "data analysis", "data visualization", "excel", "power bi", "tableau", "pandas", "numpy",
        "tensorflow", "pytorch", "nlp", "etl", "statistics", "a/b testing", "unit testing", "automation",
        // Design & product
        "ux", "ui", "ui/ux", "user experience", "user interface", "figma", "sketch", "adobe xd", "photoshop",
        "illustrator", "after effects", "design system", "design systems", "wireframing", "wireframes", "prototyping",
        "prototype", "user research", "usability testing", "information architecture", "interaction design",
        "visual design", "responsive design", "accessibility", "wcag", "design thinking", "user flows", "personas",
        "product design", "saas", "dashboard", "enterprise", "erp", "branding", "typography", "motion design",
        // Product, project & business
        "agile", "scrum", "kanban", "roadmap", "stakeholder management", "product management", "project management",
        "okr", "kpi", "market research", "business analysis", "requirements gathering", "budgeting", "forecasting",
        "strategy", "negotiation", "risk management", "process improvement", "change management", "crm", "salesforce",
        "sap", "erp systems",
        // Marketing & content
        "seo", "sem", "google analytics", "content marketing", "social media", "email marketing", "copywriting",
        "campaign management", "lead generation", "brand strategy", "conversion optimization", "google ads",
        // Finance, ops, HR
        "accounting", "financial analysis", "auditing", "payroll", "recruitment", "talent acquisition",
        "supply chain", "logistics", "procurement", "quality assurance", "compliance",
        // Working style (kept short on purpose)
        "leadership", "communication", "collaboration", "cross-functional", "mentoring", "problem solving",
        "presentation", "teamwork", "time management"
    )

    val actionVerbs: Set<String> = setOf(
        "achieved", "analyzed", "analysed", "architected", "automated", "built", "collaborated", "completed",
        "conceived", "conducted", "configured", "consolidated", "coordinated", "created", "cut", "decreased",
        "defined", "delivered", "demonstrated", "deployed", "designed", "developed", "directed", "doubled",
        "drove", "enabled", "engineered", "established", "evaluated", "executed", "expanded", "facilitated",
        "generated", "grew", "guided", "headed", "identified", "implemented", "improved", "increased",
        "initiated", "integrated", "introduced", "launched", "led", "maintained", "managed", "mentored",
        "migrated", "modernized", "negotiated", "optimized", "optimised", "orchestrated", "organized",
        "organised", "oversaw", "owned", "partnered", "pioneered", "planned", "presented", "prioritized",
        "produced", "programmed", "proposed", "prototyped", "published", "raised", "rebuilt", "redesigned",
        "reduced", "refactored", "refined", "released", "replaced", "researched", "resolved", "restructured",
        "revamped", "saved", "scaled", "secured", "shipped", "simplified", "solved", "spearheaded",
        "standardized", "streamlined", "strengthened", "supervised", "surpassed", "taught", "tested",
        "trained", "transformed", "translated", "unified", "upgraded", "validated", "won", "wrote",
        // present tense for current roles
        "lead", "manage", "design", "develop", "build", "create", "drive", "own", "deliver", "maintain"
    )

    val weakPhrases: List<String> = listOf(
        "responsible for", "duties included", "worked on", "helped with", "helped to", "assisted with",
        "was involved in", "tasked with", "various", "etc.", "hard working", "hardworking", "team player",
        "go-getter", "results-driven", "detail-oriented", "references available", "references upon request"
    )

    val stopWords: Set<String> = setOf(
        "the", "and", "for", "with", "you", "your", "our", "are", "will", "have", "has", "this", "that", "from",
        "they", "their", "who", "what", "when", "where", "which", "while", "such", "into", "about", "also",
        "can", "able", "must", "should", "would", "could", "may", "might", "all", "any", "more", "most", "other",
        "than", "then", "its", "his", "her", "per", "via", "using", "use", "used", "work", "working", "works",
        "role", "job", "team", "teams", "company", "position", "candidate", "candidates", "looking", "join",
        "experience", "years", "year", "ability", "strong", "skills", "skill", "knowledge", "understanding",
        "including", "etc", "preferred", "required", "requirements", "responsibilities", "qualifications",
        "plus", "good", "great", "excellent", "across", "within", "being", "well", "new", "high", "make",
        "help", "ensure", "provide", "support", "based", "related", "relevant", "equal", "opportunity",
        "employer", "benefits", "salary", "apply", "application", "we", "us", "is", "of", "to", "in", "on",
        "or", "as", "an", "be", "by", "at", "it", "a"
    )
}
