package week11.st560151.finalproject.data.repository

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import week11.st560151.finalproject.data.model.User

class UserRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    suspend fun getUsers(uids: List<String>): Result<List<User>> {
        return try {
            if (uids.isEmpty()) return Result.success(emptyList())

            val snapshot = firestore
                .collection("users")
                .whereIn(FieldPath.documentId(), uids.distinct().take(30))
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { document ->
                document.toObject(User::class.java)?.let { user ->
                    if (user.uid.isBlank()) user.copy(uid = document.id) else user
                }
            }

            Result.success(users)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun getUser(uid: String): Result<User?> {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            Result.success(document.toObject(User::class.java))
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun updateProfile(
        uid: String,
        displayName: String? = null,
        photoBase64: String? = null
    ): Result<Unit> {
        return try {
            val updates = buildMap<String, Any> {
                displayName?.let { put("displayName", it) }
                photoBase64?.let { put("photoBase64", it) }
            }

            if (updates.isNotEmpty()) {
                firestore.collection("users").document(uid)
                    .set(updates, SetOptions.merge())
                    .await()
            }

            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}