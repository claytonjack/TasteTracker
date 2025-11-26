package week11.st421007.finalproject.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import week11.st421007.finalproject.data.PlacesRepository
import week11.st421007.finalproject.model.PlaceAutocomplete
import week11.st421007.finalproject.model.PlaceDetails
import week11.st421007.finalproject.util.UiState

@OptIn(FlowPreview::class)
class PlacesViewModel(application: Application) : AndroidViewModel(application) {
    private val placesRepository = PlacesRepository(application.applicationContext)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _predictions = MutableStateFlow<UiState<List<PlaceAutocomplete>>>(UiState.Idle)
    val predictions: StateFlow<UiState<List<PlaceAutocomplete>>> = _predictions.asStateFlow()

    private val _selectedPlace = MutableStateFlow<PlaceDetails?>(null)
    val selectedPlace: StateFlow<PlaceDetails?> = _selectedPlace.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .collect { query ->
                    if (query.isNotBlank()) {
                        searchPlaces(query)
                    } else {
                        _predictions.value = UiState.Idle
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _predictions.value = UiState.Idle
        }
    }

    private fun searchPlaces(query: String) {
        _predictions.value = UiState.Loading
        viewModelScope.launch {
            val result = placesRepository.searchPlaces(query)
            result.fold(
                onSuccess = { predictions ->
                    _predictions.value = UiState.Success(predictions)
                },
                onFailure = { exception ->
                    _predictions.value = UiState.Error(
                        exception.message ?: "Failed to search places"
                    )
                }
            )
        }
    }

    fun selectPlace(placeId: String) {
        viewModelScope.launch {
            val result = placesRepository.getPlaceDetails(placeId)
            result.fold(
                onSuccess = { details ->
                    _selectedPlace.value = details
                },
                onFailure = { exception ->
                    _predictions.value = UiState.Error(
                        exception.message ?: "Failed to fetch place details"
                    )
                }
            )
        }
    }

    fun clearSelection() {
        _selectedPlace.value = null
        _searchQuery.value = ""
        _predictions.value = UiState.Idle
        placesRepository.resetSession()
    }

    fun resetPredictions() {
        _predictions.value = UiState.Idle
    }
}