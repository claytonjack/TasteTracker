package week11.st421007.finalproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import week11.st421007.finalproject.ui.theme.TasteTrackerTheme
import week11.st421007.finalproject.util.NavigationGraph
import week11.st421007.finalproject.viewmodel.AuthViewModel
import week11.st421007.finalproject.viewmodel.JournalViewModel
import week11.st421007.finalproject.viewmodel.PlacesViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }

        enableEdgeToEdge()

        setContent {
            TasteTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel()
                    val journalViewModel: JournalViewModel = viewModel()
                    val placesViewModel: PlacesViewModel = viewModel()

                    NavigationGraph(
                        navController = navController,
                        authViewModel = authViewModel,
                        journalViewModel = journalViewModel,
                        placesViewModel = placesViewModel
                    )
                }
            }
        }
    }
}
