package week11.st560151.finalproject.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import week11.st560151.finalproject.data.model.AppNotification

class NotificationRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    suspend fun createNotifications(
        recipientIds: List<String>,
        groupId: String,
        type: String,
        title: String,
        message: String,
        excludeUserId: String? = null
    ): Result<Unit> {
        return try {
            val recipients = recipientIds
                .map(String::trim)
                .filter(String::isNotBlank)
                .filter { it != excludeUserId }
                .distinct()

            if (recipients.isNotEmpty()) {
                val batch = firestore.batch()

                recipients.forEach { recipientId ->
                    val reference = firestore
                        .collection("notifications")
                        .document()

                    val notification = AppNotification(
                        id = reference.id,
                        recipientId = recipientId,
                        groupId = groupId,
                        type = type,
                        title = title,
                        message = message,
                        isRead = false,
                        createdAt = System.currentTimeMillis()
                    )

                    batch.set(reference, notification)
                }

                batch.commit().await()
            }

            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    fun observeNotifications(
        userId: String
    ): Flow<List<AppNotification>> = callbackFlow {

        val registration = firestore
            .collection("notifications")
            .whereEqualTo("recipientId", userId)
            .orderBy(
                "createdAt",
                Query.Direction.DESCENDING
            )
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val notifications = snapshot
                    ?.documents
                    ?.mapNotNull { document ->
                        document.toObject(
                            AppNotification::class.java
                        )
                    }
                    ?: emptyList()

                trySend(notifications)
            }

        awaitClose {
            registration.remove()
        }
    }

    suspend fun markAsRead(
        notificationId: String
    ): Result<Unit> {
        return try {
            firestore
                .collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .await()

            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}