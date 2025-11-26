package week11.st421007.finalproject.ui.screens.main

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import week11.st421007.finalproject.util.UiState
import week11.st421007.finalproject.viewmodel.JournalViewModel

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    journalViewModel: JournalViewModel,
    onMarkerClick: (String) -> Unit,
    onLogout: () -> Unit
) {
    val entriesState by journalViewModel.entriesState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val entriesWithCoordinates = remember(entriesState) {
        when (val state = entriesState) {
            is UiState.Success -> state.data.filter { it.hasValidCoordinates() }
            else -> emptyList()
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        if (entriesWithCoordinates.isNotEmpty()) {
            val firstEntry = entriesWithCoordinates.first()
            position = CameraPosition.fromLatLngZoom(
                LatLng(firstEntry.latitude!!, firstEntry.longitude!!),
                12f
            )
        } else {
            position = CameraPosition.fromLatLngZoom(LatLng(40.7128, -74.0060), 10f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food Map") },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                entriesWithCoordinates.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No restaurants with locations yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add entries using autocomplete to see them on the map",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                else -> {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = locationPermissions.allPermissionsGranted
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            myLocationButtonEnabled = locationPermissions.allPermissionsGranted
                        )
                    ) {
                        entriesWithCoordinates.forEach { entry ->
                            Marker(
                                state = MarkerState(
                                    position = LatLng(entry.latitude!!, entry.longitude!!)
                                ),
                                title = entry.restaurantName,
                                snippet = "${entry.getPriceLevelDisplay()} • ${String.format("%.1f", entry.foodQualityRating)} ⭐"
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
