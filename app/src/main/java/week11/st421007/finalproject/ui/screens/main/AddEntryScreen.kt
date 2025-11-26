package week11.st421007.finalproject.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import week11.st421007.finalproject.ui.components.RestaurantAutocompleteField
import week11.st421007.finalproject.util.UiState
import week11.st421007.finalproject.viewmodel.AuthViewModel
import week11.st421007.finalproject.viewmodel.JournalViewModel
import week11.st421007.finalproject.viewmodel.PlacesViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    authViewModel: AuthViewModel,
    journalViewModel: JournalViewModel,
    placesViewModel: PlacesViewModel,
    onNavigateBack: () -> Unit
) {
    val userId = authViewModel.currentUserId ?: return

    var restaurantName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(3f) }
    var priceLevel by remember { mutableStateOf(2) }
    var selectedDate by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedLatitude by remember { mutableStateOf<Double?>(null) }
    var selectedLongitude by remember { mutableStateOf<Double?>(null) }

    val operationState by journalViewModel.operationState.collectAsState()
    val predictions by placesViewModel.predictions.collectAsState()
    val selectedPlace by placesViewModel.selectedPlace.collectAsState()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    LaunchedEffect(operationState) {
        if (operationState is UiState.Success) {
            journalViewModel.resetOperationState()
            onNavigateBack()
        }
    }

    LaunchedEffect(selectedPlace) {
        selectedPlace?.let { place ->
            restaurantName = place.name
            location = place.address
            selectedLatitude = place.latitude
            selectedLongitude = place.longitude
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            placesViewModel.clearSelection()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Entry") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            RestaurantAutocompleteField(
                value = restaurantName,
                onValueChange = {
                    restaurantName = it
                    placesViewModel.updateSearchQuery(it)
                },
                predictions = predictions,
                onPlaceSelected = { place ->
                    placesViewModel.selectPlace(place.placeId)
                },
                onClearClick = {
                    restaurantName = ""
                    placesViewModel.clearSelection()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Visit Date: ${
                        java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(selectedDate)
                    }"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Food Quality",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(5) { index ->
                            IconButton(
                                onClick = { rating = (index + 1).toFloat() }
                            ) {
                                Icon(
                                    imageVector = if (index < rating.toInt())
                                        Icons.Default.Star
                                    else
                                        Icons.Default.StarBorder,
                                    contentDescription = "${index + 1} stars",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Price",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (level in 1..4) {
                            FilterChip(
                                selected = priceLevel == level,
                                onClick = { priceLevel = level },
                                label = {
                                    Text(
                                        text = "$".repeat(level),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                minLines = 4,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (operationState is UiState.Error) {
                Text(
                    text = (operationState as UiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = {
                    focusManager.clearFocus()
                    journalViewModel.addEntry(
                        userId = userId,
                        restaurantName = restaurantName,
                        visitDate = Timestamp(selectedDate),
                        foodQualityRating = rating,
                        priceLevel = priceLevel,
                        location = location,
                        notes = notes,
                        latitude = selectedLatitude,
                        longitude = selectedLongitude
                    )
                },
                enabled = operationState !is UiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (operationState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Entry")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.time
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = Date(it)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
