package com.arif.cvcheck

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arif.cvcheck.ui.AnalyzingScreen
import com.arif.cvcheck.ui.CvCheckTheme
import com.arif.cvcheck.ui.HomeScreen
import com.arif.cvcheck.ui.ResultScreen
import com.arif.cvcheck.ui.reportAsText

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val supportedTypes = arrayOf(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "image/*"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CvCheckTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val jobDescription by viewModel.jobDescription.collectAsStateWithLifecycle()

                val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) viewModel.analyze(uri)
                }

                BackHandler(enabled = state is UiState.Done || state is UiState.Analyzing) { viewModel.reset() }

                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val content = Modifier.systemBarsPadding()
                    when (val s = state) {
                        is UiState.Idle, is UiState.Failed -> HomeScreen(
                            jobDescription = jobDescription,
                            onJobDescriptionChange = viewModel::setJobDescription,
                            onPickFile = { picker.launch(supportedTypes) },
                            errorMessage = (s as? UiState.Failed)?.message,
                            modifier = content.imePadding()
                        )
                        is UiState.Analyzing -> AnalyzingScreen(s.stage, content)
                        is UiState.Done -> ResultScreen(
                            done = s,
                            onCheckAnother = viewModel::reset,
                            onShare = {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "ATS report: ${s.fileName}")
                                    putExtra(Intent.EXTRA_TEXT, reportAsText(s.fileName, s.report))
                                }
                                startActivity(Intent.createChooser(send, "Share report"))
                            },
                            modifier = content
                        )
                    }
                }
            }
        }
    }
}
