package week11.st421007.finalproject.data

import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.tasks.await
import week11.st421007.finalproject.model.PlaceAutocomplete
import week11.st421007.finalproject.model.PlaceDetails
import android.content.Context

class PlacesRepository(context: Context) {
    private val placesClient: PlacesClient = Places.createClient(context)
    private var sessionToken: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()

    suspend fun searchPlaces(query: String): Result<List<PlaceAutocomplete>> {
        if (query.isBlank()) {
            return Result.success(emptyList())
        }

        return try {
            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(sessionToken)
                .setTypeFilter(TypeFilter.ESTABLISHMENT)
                .setQuery(query)
                .build()

            val response = placesClient.findAutocompletePredictions(request).await()

            val predictions = response.autocompletePredictions.map { prediction ->
                PlaceAutocomplete(
                    placeId = prediction.placeId,
                    primaryText = prediction.getPrimaryText(null).toString(),
                    secondaryText = prediction.getSecondaryText(null).toString()
                )
            }

            Result.success(predictions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaceDetails(placeId: String): Result<PlaceDetails> {
        return try {
            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
            )

            val request = FetchPlaceRequest.builder(placeId, placeFields)
                .setSessionToken(sessionToken)
                .build()

            val response = placesClient.fetchPlace(request).await()
            val place = response.place

            sessionToken = AutocompleteSessionToken.newInstance()

            val latLng = place.latLng ?: throw Exception("Place has no coordinates")

            val details = PlaceDetails(
                placeId = place.id ?: placeId,
                name = place.name ?: "",
                address = place.address ?: "",
                latitude = latLng.latitude,
                longitude = latLng.longitude
            )

            Result.success(details)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun resetSession() {
        sessionToken = AutocompleteSessionToken.newInstance()
    }
}