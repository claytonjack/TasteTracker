package week11.st421007.finalproject.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.util.Date

data class JournalEntry(
    @DocumentId
    val id: String = "",
    val restaurantName: String = "",
    val visitDate: Timestamp = Timestamp.now(),
    val foodQualityRating: Float = 3.0f,
    val priceLevel: Int = 2,
    val location: String = "",
    val notes: String = "",
    val userId: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    fun getVisitDateAsDate(): Date = visitDate.toDate()

    fun toMap(): Map<String, Any> {
        val map = mutableMapOf(
            "restaurantName" to restaurantName,
            "visitDate" to visitDate,
            "foodQualityRating" to foodQualityRating,
            "priceLevel" to priceLevel,
            "location" to location,
            "notes" to notes,
            "userId" to userId,
            "createdAt" to createdAt
        )

        latitude?.let { map["latitude"] = it }
        longitude?.let { map["longitude"] = it }

        return map
    }

    fun getPriceLevelDisplay(): String {
        return "$".repeat(priceLevel)
    }

    fun hasValidCoordinates(): Boolean {
        return latitude != null && longitude != null
    }

    companion object {
        fun fromMap(id: String, map: Map<String, Any>): JournalEntry {
            return JournalEntry(
                id = id,
                restaurantName = map["restaurantName"] as? String ?: "",
                visitDate = map["visitDate"] as? Timestamp ?: Timestamp.now(),
                foodQualityRating = (map["foodQualityRating"] as? Number)?.toFloat() ?: 3.0f,
                priceLevel = (map["priceLevel"] as? Number)?.toInt() ?: 2,
                location = map["location"] as? String ?: "",
                notes = map["notes"] as? String ?: "",
                userId = map["userId"] as? String ?: "",
                createdAt = map["createdAt"] as? Timestamp ?: Timestamp.now(),
                latitude = (map["latitude"] as? Number)?.toDouble(),
                longitude = (map["longitude"] as? Number)?.toDouble()
            )
        }
    }
}