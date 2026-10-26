package com.arif.cvcheck.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arif.cvcheck.UiState
import com.arif.cvcheck.ats.AtsReport
import com.arif.cvcheck.ats.CategoryScore
import com.arif.cvcheck.ats.Priority
import com.arif.cvcheck.ats.Suggestion
import com.arif.cvcheck.extract.FileKind
import kotlin.math.roundToInt

// ============================================================ Home

@Composable
fun HomeScreen(
    jobDescription: String,
    onJobDescriptionChange: (String) -> Unit,
    onPickFile: () -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    var showJd by rememberSaveable { mutableStateOf(jobDescription.isNotBlank()) }
    val outline = MaterialTheme.colorScheme.outline

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("CV Check", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(
                "See how an applicant tracking system reads your CV, get a score out of 100, and fix what is holding it back.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (errorMessage != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Upload target
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable(onClick = onPickFile)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    // drawn inside the Surface so the dashes sit on top of its background
                    .drawBehind {
                        val w = 2.dp.toPx()
                        drawRoundRect(
                            color = outline,
                            topLeft = Offset(w / 2, w / 2),
                            size = Size(size.width - w, size.height - w),
                            cornerRadius = CornerRadius(22.dp.toPx()),
                            style = Stroke(
                                width = w,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 16f))
                            )
                        )
                    }
                    .padding(horizontal = 20.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .then(Modifier.drawBehind { drawCircle(Color(0x1F4F46E5)) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text("↑", fontSize = 30.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Text("Upload your CV", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "PDF  ·  DOCX  ·  JPG  ·  PNG",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Button(onClick = onPickFile, shape = RoundedCornerShape(14.dp)) { Text("Choose file") }
                Text(
                    "Analysed on your phone. Your CV is never uploaded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Optional job description
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showJd = !showJd },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Match to a job (optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Paste a job description to see which keywords your CV is missing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(if (showJd) "▴" else "▾", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp)
                }
                if (showJd) {
                    OutlinedTextField(
                        value = jobDescription,
                        onValueChange = onJobDescriptionChange,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5,
                        maxLines = 10,
                        placeholder = { Text("Paste the job description here") }
                    )
                    if (jobDescription.isNotEmpty()) {
                        TextButton(onClick = { onJobDescriptionChange("") }) { Text("Clear") }
                    }
                }
            }
        }

        // What we check
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("What gets checked", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val items = listOf(
                "Contact details" to "10", "Section structure" to "20", "Keywords" to "20", "Impact & achievements" to "20",
                "Formatting & length" to "15", "Content quality" to "10", "Machine readability" to "5"
            )
            items.forEach { (name, pts) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text("$pts pts", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ============================================================ Analyzing

@Composable
fun AnalyzingScreen(stage: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(20.dp))
        Text(stage, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "This stays on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================ Results

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(
    done: UiState.Done,
    onCheckAnother: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val report = done.report
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Your ATS score", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${done.fileName}  ·  ${report.wordCount} words  ·  ~${report.pageCount} page${if (report.pageCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScoreGauge(report.total)
                    Text(
                        report.verdict,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Signal.forFraction(report.total / 100f),
                        textAlign = TextAlign.Center
                    )
                    if (report.jobMatchPercent != null) {
                        Text(
                            "${report.jobMatchPercent}% of the job's keywords found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (done.kind == FileKind.IMAGE) {
                        Text(
                            "Read from an image with OCR. Most ATS cannot read images, so submit a text-based PDF or DOCX.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item { SectionTitle("Score breakdown") }
        items(report.categories) { CategoryCard(it) }

        if (report.matchedKeywords.isNotEmpty() || report.missingKeywords.isNotEmpty()) {
            item { SectionTitle(if (report.jobMatchPercent != null) "Job keywords" else "Skills found") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (report.matchedKeywords.isNotEmpty()) {
                        if (report.jobMatchPercent != null) Label("In your CV")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) { report.matchedKeywords.forEach { Chip(it, Signal.good()) } }
                    }
                    if (report.missingKeywords.isNotEmpty()) {
                        Label("Missing from your CV")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) { report.missingKeywords.forEach { Chip(it, Signal.bad()) } }
                    }
                }
            }
        }

        item { SectionTitle("How to improve (${report.suggestions.size})") }
        if (report.suggestions.isEmpty()) {
            item {
                Text(
                    "Nothing to fix. Your CV passes every check.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Signal.good()
                )
            }
        } else {
            items(report.suggestions) { SuggestionCard(it) }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(
                    onClick = onCheckAnother,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Check another CV") }
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Share report") }
                Text(
                    "Scores are an estimate from common ATS rules. Each employer's system differs, so use this as a guide.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Chip(text: String, tint: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f))
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ScoreGauge(score: Int, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(score) { progress.animateTo(score / 100f, tween(900)) }
    val color = Signal.forFraction(score / 100f)
    val track = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track, startAngle = 135f, sweepAngle = 270f, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = color, startAngle = 135f, sweepAngle = 270f * progress.value, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                (progress.value * 100).roundToInt().toString(),
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text("out of 100", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CategoryCard(c: CategoryScore) {
    var open by rememberSaveable(c.name) { mutableStateOf(false) }
    val tint = Signal.forFraction(c.fraction)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { open = !open }
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${c.score}/${c.max}", style = MaterialTheme.typography.titleMedium, color = tint, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(if (open) "▴" else "▾", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { c.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = tint,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            if (open) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    c.checks.forEach { check ->
                        Row(verticalAlignment = Alignment.Top) {
                            val mark = if (check.passed) Signal.good() else Signal.bad()
                            Box(
                                Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .drawBehind { drawCircle(mark.copy(alpha = 0.18f)) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (check.passed) "✓" else "✕", color = mark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(check.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    check.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(s: Suggestion) {
    val tint = when (s.priority) {
        Priority.HIGH -> Signal.bad()
        Priority.MEDIUM -> Signal.warn()
        Priority.LOW -> Signal.info()
    }
    val label = when (s.priority) {
        Priority.HIGH -> "HIGH"
        Priority.MEDIUM -> "MEDIUM"
        Priority.LOW -> "LOW"
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(50), color = tint.copy(alpha = 0.15f)) {
                    Text(
                        label,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tint
                    )
                }
                Text(s.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(s.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(s.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Plain-text version of the report, used by the Share button. */
fun reportAsText(fileName: String, r: AtsReport): String = buildString {
    appendLine("ATS report for $fileName")
    appendLine("Score: ${r.total}/100 — ${r.verdict}")
    if (r.jobMatchPercent != null) appendLine("Job keyword match: ${r.jobMatchPercent}%")
    appendLine()
    appendLine("Breakdown")
    r.categories.forEach { appendLine("• ${it.name}: ${it.score}/${it.max}") }
    if (r.missingKeywords.isNotEmpty()) {
        appendLine()
        appendLine("Missing keywords: ${r.missingKeywords.joinToString(", ")}")
    }
    if (r.suggestions.isNotEmpty()) {
        appendLine()
        appendLine("How to improve")
        r.suggestions.forEachIndexed { i, s -> appendLine("${i + 1}. [${s.priority}] ${s.title} — ${s.detail}") }
    }
}
