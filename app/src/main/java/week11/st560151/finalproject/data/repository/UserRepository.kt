package week11.st560151.finalproject.data.repository

import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import week11.st560151.finalproject.data.model.User

class UserRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    /** Resolves display profiles for a group's member ids, e.g. for "Paid by" pickers. */
    suspend fun getUsers(
        uids: List<String>
    ): Result<List<User>> {
        return try {
            if (uids.isEmpty()) {
                return Result.success(emptyList())
            }

            val snapshot = firestore
                .collection("users")
                .whereIn(FieldPath.documentId(), uids.distinct().take(30))
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { document ->
                document.toObject(User::class.java)?.let { user ->
                    if (user.uid.isBlank()) {
                        user.copy(uid = document.id)
                    } else {
                        user
                    }
                }
            }

            Result.success(users)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}
