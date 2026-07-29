package week11.st560151.finalproject.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import week11.st560151.finalproject.data.model.Group
import java.util.UUID

class GroupRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    suspend fun createGroup(
        name: String,
        ownerId: String
    ): Result<String> {
        return try {
            val document = firestore
                .collection("groups")
                .document()

            val group = Group(
                id = document.id,
                name = name.trim(),
                ownerId = ownerId,
                memberIds = listOf(ownerId),
                inviteCode = UUID.randomUUID()
                    .toString()
                    .take(6)
                    .uppercase(),
                createdAt = System.currentTimeMillis()
            )

            document.set(group).await()

            Result.success(document.id)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    fun observeGroups(
        userId: String
    ): Flow<List<Group>> = callbackFlow {
        val listener = firestore
            .collection("groups")
            .whereArrayContains("memberIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val groups = snapshot
                    ?.documents
                    ?.mapNotNull {
                        it.toObject(Group::class.java)
                    }
                    ?: emptyList()

                trySend(groups)
            }

        awaitClose {
            listener.remove()
        }
    }
}