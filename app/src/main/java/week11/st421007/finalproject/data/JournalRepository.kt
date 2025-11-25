package week11.st421007.finalproject.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import week11.st421007.finalproject.model.JournalEntry

class JournalRepository {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private fun getUserEntriesCollection(userId: String) =
        firestore.collection("users").document(userId).collection("entries")

    fun getUserEntries(userId: String): Flow<Result<List<JournalEntry>>> = callbackFlow {
        val listenerRegistration = getUserEntriesCollection(userId)
            .orderBy("visitDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val entries = snapshot.documents.mapNotNull { doc ->
                        try {
                            JournalEntry.fromMap(doc.id, doc.data as Map<String, Any>)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(Result.success(entries))
                } else {
                    trySend(Result.success(emptyList()))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun addEntry(entry: JournalEntry): Result<String> {
        return try {
            val docRef = getUserEntriesCollection(entry.userId).document()
            val entryWithId = entry.copy(id = docRef.id)
            docRef.set(entryWithId.toMap()).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEntry(entry: JournalEntry): Result<Unit> {
        return try {
            getUserEntriesCollection(entry.userId)
                .document(entry.id)
                .set(entry.toMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEntry(userId: String, entryId: String): Result<Unit> {
        return try {
            getUserEntriesCollection(userId)
                .document(entryId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getEntryById(userId: String, entryId: String): Result<JournalEntry> {
        return try {
            val doc = getUserEntriesCollection(userId)
                .document(entryId)
                .get()
                .await()

            if (doc.exists()) {
                val entry = JournalEntry.fromMap(doc.id, doc.data as Map<String, Any>)
                Result.success(entry)
            } else {
                Result.failure(Exception("Entry not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchEntriesByName(userId: String, query: String): Result<List<JournalEntry>> {
        return try {
            val snapshot = getUserEntriesCollection(userId)
                .orderBy("restaurantName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()

            val entries = snapshot.documents.mapNotNull { doc ->
                try {
                    JournalEntry.fromMap(doc.id, doc.data as Map<String, Any>)
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}