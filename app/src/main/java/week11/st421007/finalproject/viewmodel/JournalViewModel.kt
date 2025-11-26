package week11.st421007.finalproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import week11.st421007.finalproject.data.JournalRepository
import week11.st421007.finalproject.model.JournalEntry
import week11.st421007.finalproject.util.UiState

class JournalViewModel : ViewModel() {
    private val journalRepository = JournalRepository()

    private val _entriesState = MutableStateFlow<UiState<List<JournalEntry>>>(UiState.Idle)
    val entriesState: StateFlow<UiState<List<JournalEntry>>> = _entriesState.asStateFlow()

    private val _operationState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val operationState: StateFlow<UiState<String>> = _operationState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPriceLevel = MutableStateFlow<Int?>(null)
    val selectedPriceLevel: StateFlow<Int?> = _selectedPriceLevel.asStateFlow()

    private val _minRating = MutableStateFlow<Float?>(null)
    val minRating: StateFlow<Float?> = _minRating.asStateFlow()

    private var allEntries: List<JournalEntry> = emptyList()

    fun loadEntries(userId: String) {
        _entriesState.value = UiState.Loading
        viewModelScope.launch {
            journalRepository.getUserEntries(userId).collect { result ->
                result.fold(
                    onSuccess = { entries ->
                        allEntries = entries
                        applyFilters()
                    },
                    onFailure = { exception ->
                        _entriesState.value = UiState.Error(
                            exception.message ?: "Failed to load entries"
                        )
                    }
                )
            }
        }
    }

    fun addEntry(
        userId: String,
        restaurantName: String,
        visitDate: Timestamp,
        foodQualityRating: Float,
        priceLevel: Int,
        location: String,
        notes: String,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        if (restaurantName.isBlank()) {
            _operationState.value = UiState.Error("Restaurant name is required")
            return
        }

        if (location.isBlank()) {
            _operationState.value = UiState.Error("Location is required")
            return
        }

        val entry = JournalEntry(
            restaurantName = restaurantName.trim(),
            visitDate = visitDate,
            foodQualityRating = foodQualityRating,
            priceLevel = priceLevel,
            location = location.trim(),
            notes = notes.trim(),
            userId = userId,
            latitude = latitude,
            longitude = longitude
        )

        _operationState.value = UiState.Loading
        viewModelScope.launch {
            val result = journalRepository.addEntry(entry)
            result.fold(
                onSuccess = { entryId ->
                    _operationState.value = UiState.Success("Entry added successfully")
                },
                onFailure = { exception ->
                    _operationState.value = UiState.Error(
                        exception.message ?: "Failed to add entry"
                    )
                }
            )
        }
    }

    fun updateEntry(
        entryId: String,
        userId: String,
        restaurantName: String,
        visitDate: Timestamp,
        foodQualityRating: Float,
        priceLevel: Int,
        location: String,
        notes: String,
        createdAt: Timestamp,
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        if (restaurantName.isBlank()) {
            _operationState.value = UiState.Error("Restaurant name is required")
            return
        }

        if (location.isBlank()) {
            _operationState.value = UiState.Error("Location is required")
            return
        }

        val entry = JournalEntry(
            id = entryId,
            restaurantName = restaurantName.trim(),
            visitDate = visitDate,
            foodQualityRating = foodQualityRating,
            priceLevel = priceLevel,
            location = location.trim(),
            notes = notes.trim(),
            userId = userId,
            createdAt = createdAt,
            latitude = latitude,
            longitude = longitude
        )

        _operationState.value = UiState.Loading
        viewModelScope.launch {
            val result = journalRepository.updateEntry(entry)
            result.fold(
                onSuccess = {
                    _operationState.value = UiState.Success("Entry updated successfully")
                },
                onFailure = { exception ->
                    _operationState.value = UiState.Error(
                        exception.message ?: "Failed to update entry"
                    )
                }
            )
        }
    }

    fun deleteEntry(userId: String, entryId: String) {
        _operationState.value = UiState.Loading
        viewModelScope.launch {
            val result = journalRepository.deleteEntry(userId, entryId)
            result.fold(
                onSuccess = {
                    _operationState.value = UiState.Success("Entry deleted successfully")
                },
                onFailure = { exception ->
                    _operationState.value = UiState.Error(
                        exception.message ?: "Failed to delete entry"
                    )
                }
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun setPriceLevelFilter(priceLevel: Int?) {
        _selectedPriceLevel.value = priceLevel
        applyFilters()
    }

    fun setMinRatingFilter(rating: Float?) {
        _minRating.value = rating
        applyFilters()
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedPriceLevel.value = null
        _minRating.value = null
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = allEntries

        if (_searchQuery.value.isNotBlank()) {
            val query = _searchQuery.value.lowercase()
            filtered = filtered.filter {
                it.restaurantName.lowercase().contains(query) ||
                        it.location.lowercase().contains(query) ||
                        it.notes.lowercase().contains(query)
            }
        }

        _selectedPriceLevel.value?.let { priceLevel ->
            filtered = filtered.filter { it.priceLevel == priceLevel }
        }

        _minRating.value?.let { minRating ->
            filtered = filtered.filter { it.foodQualityRating >= minRating }
        }

        _entriesState.value = UiState.Success(filtered)
    }

    fun getEntriesWithCoordinates(): List<JournalEntry> {
        return allEntries.filter { it.hasValidCoordinates() }
    }

    fun resetOperationState() {
        _operationState.value = UiState.Idle
    }
}