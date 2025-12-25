package se.johannalynn.nexttram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface TimetableUiState {
    data object Loading : TimetableUiState
    data class Error(val message: String) : TimetableUiState
    data class Success(val departures: List<Departure>) : TimetableUiState
}

class TimetableViewModel : ViewModel() {
    private val service = TimetableService()

    private val _uiState = MutableStateFlow<TimetableUiState>(TimetableUiState.Loading)
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    init {
        fetchDepartures()
    }

    fun fetchDepartures() {
        viewModelScope.launch {
            _uiState.value = TimetableUiState.Loading
            try {
                val result = service.getDepartures()
                _uiState.value = TimetableUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = TimetableUiState.Error("Filed to load: ${e.message}")
            }
        }
    }
}