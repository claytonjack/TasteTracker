package week11.st421007.finalproject.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import week11.st421007.finalproject.model.PlaceAutocomplete
import week11.st421007.finalproject.util.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    predictions: UiState<List<PlaceAutocomplete>>,
    onPlaceSelected: (PlaceAutocomplete) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Restaurant Name *"
) {
    var showDropdown by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                showDropdown = it.isNotEmpty()
            },
            label = { Text(label) },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = {
                        onClearClick()
                        showDropdown = false
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (showDropdown && value.isNotEmpty()) {
            when (predictions) {
                is UiState.Loading -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
                is UiState.Success -> {
                    if (predictions.data.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                            ) {
                                items(predictions.data) { place ->
                                    ListItem(
                                        headlineContent = { Text(place.primaryText) },
                                        supportingContent = { Text(place.secondaryText) },
                                        leadingContent = {
                                            Icon(
                                                Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        modifier = Modifier.clickable {
                                            onPlaceSelected(place)
                                            showDropdown = false
                                        }
                                    )
                                    if (place != predictions.data.last()) {
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = predictions.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                else -> {}
            }
        }
    }
}