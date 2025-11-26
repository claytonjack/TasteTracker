package week11.st421007.finalproject.model

data class PlaceAutocomplete(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String
)

data class PlaceDetails(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)