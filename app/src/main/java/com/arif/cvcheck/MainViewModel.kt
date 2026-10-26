package com.arif.cvcheck

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arif.cvcheck.ats.AtsAnalyzer
import com.arif.cvcheck.ats.AtsReport
import com.arif.cvcheck.extract.CvReadException
import com.arif.cvcheck.extract.FileKind
import com.arif.cvcheck.extract.TextExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UiState {
    data object Idle : UiState
    data class Analyzing(val stage: String) : UiState
    data class Done(val fileName: String, val kind: FileKind, val report: AtsReport) : UiState
    data class Failed(val message: String) : UiState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val extractor = TextExtractor(app)
    private var job: Job? = null

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _jobDescription = MutableStateFlow("")
    val jobDescription: StateFlow<String> = _jobDescription.asStateFlow()

    fun setJobDescription(value: String) { _jobDescription.value = value }

    fun analyze(uri: Uri) {
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = UiState.Analyzing("Opening file…")
            try {
                val extracted = extractor.extract(uri) { stage -> _state.value = UiState.Analyzing(stage) }
                _state.value = UiState.Analyzing("Scoring your CV…")
                val report = withContext(Dispatchers.Default) {
                    AtsAnalyzer.analyze(extracted.input, _jobDescription.value)
                }
                _state.value = UiState.Done(extracted.fileName, extracted.kind, report)
            } catch (e: CancellationException) {
                throw e
            } catch (e: CvReadException) {
                _state.value = UiState.Failed(e.message ?: "Could not read this file.")
            } catch (e: Exception) {
                _state.value = UiState.Failed("Something went wrong while analysing this file.")
            }
        }
    }

    fun reset() {
        job?.cancel()
        _state.value = UiState.Idle
    }
}
