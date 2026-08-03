package week11.st560151.finalproject.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import week11.st560151.finalproject.data.model.AppNotification

class NotificationRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG =
            "NotificationRepository"

        private const val COLLECTION =
            "notifications"
    }

    suspend fun createNotifications(
        recipientIds: List<String>,
        groupId: String,
        type: String,
        title: String,
        message: String,
        excludeUserId: String? = null
    ): Result<Unit> {
        return try {
            if (groupId.isBlank()) {
                throw IllegalArgumentException(
                    "Group ID is missing."
                )
            }

            val recipients =
                recipientIds
                    .map { recipientId ->
                        recipientId.trim()
                    }
                    .filter { recipientId ->
                        recipientId.isNotBlank()
                    }
                    .filter { recipientId ->
                        recipientId != excludeUserId
                    }
                    .distinct()

            if (recipients.isEmpty()) {
                return Result.success(Unit)
            }

            val batch =
                firestore.batch()

            recipients.forEach {
                    recipientId ->

                val document =
                    firestore
                        .collection(COLLECTION)
                        .document()

                val notification =
                    AppNotification(
                        id = document.id,
                        recipientId =
                            recipientId,
                        groupId = groupId,
                        type = type,
                        title = title,
                        message = message,
                        isRead = false,
                        createdAt =
                            System.currentTimeMillis()
                    )

                batch.set(
                    document,
                    notification
                )
            }

            batch.commit().await()

            Log.d(
                TAG,
                "Notifications created."
            )

            Result.success(Unit)

        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Unable to create notifications.",
                exception
            )

            Result.failure(exception)
        }
    }

    fun observeNotifications(
        userId: String
    ): Flow<List<AppNotification>> =
        callbackFlow {

            if (userId.isBlank()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            val listener =
                firestore
                    .collection(COLLECTION)
                    .whereEqualTo(
                        "recipientId",
                        userId
                    )
                    .addSnapshotListener {
                            snapshot,
                            error ->

                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }

                        val notifications =
                            snapshot
                                ?.documents
                                ?.mapNotNull {
                                        document ->

                                    document
                                        .toObject(
                                            AppNotification::class.java
                                        )
                                        ?.copy(
                                            id =
                                                document.id
                                        )
                                }
                                ?.sortedByDescending {
                                        notification ->
                                    notification.createdAt
                                }
                                ?: emptyList()

                        trySend(notifications)
                    }

            awaitClose {
                listener.remove()
            }
        }

    suspend fun markAsRead(
        notificationId: String
    ): Result<Unit> {
        return try {
            if (notificationId.isBlank()) {
                throw IllegalArgumentException(
                    "Notification ID is missing."
                )
            }

            firestore
                .collection(COLLECTION)
                .document(notificationId)
                .update(
                    "isRead",
                    true
                )
                .await()

            Result.success(Unit)

        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun deleteNotification(
        notificationId: String
    ): Result<Unit> {
        return try {
            if (notificationId.isBlank()) {
                throw IllegalArgumentException(
                    "Notification ID is missing."
                )
            }

            firestore
                .collection(COLLECTION)
                .document(notificationId)
                .delete()
                .await()

            Result.success(Unit)

        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}