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
        private const val TAG = "NotificationRepository"
        private const val NOTIFICATIONS_COLLECTION = "notifications"
    }

    /**
     * Creates one notification document for each recipient.
     *
     * recipientIds must contain Firebase Authentication UIDs,
     * not email addresses.
     */
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

            if (type.isBlank()) {
                throw IllegalArgumentException(
                    "Notification type is missing."
                )
            }

            if (title.isBlank()) {
                throw IllegalArgumentException(
                    "Notification title is missing."
                )
            }

            if (message.isBlank()) {
                throw IllegalArgumentException(
                    "Notification message is missing."
                )
            }

            /*
             * Clean the recipient list:
             * - remove blank UIDs;
             * - remove duplicates;
             * - remove the excluded user;
             * - normally the excluded user is the person
             *   who performed the action.
             */
            val recipients = recipientIds
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

            Log.d(
                TAG,
                "createNotifications: " +
                        "groupId=$groupId, " +
                        "recipients=$recipients, " +
                        "excludedUser=$excludeUserId"
            )

            /*
             * Nothing to create is still a successful result.
             */
            if (recipients.isEmpty()) {
                Log.d(
                    TAG,
                    "No notification recipients found."
                )

                return Result.success(Unit)
            }

            /*
             * Use one batch so all notification documents
             * are written together.
             */
            val batch = firestore.batch()

            recipients.forEach { recipientId ->

                val notificationDocument = firestore
                    .collection(NOTIFICATIONS_COLLECTION)
                    .document()

                val notification = AppNotification(
                    id = notificationDocument.id,
                    recipientId = recipientId,
                    groupId = groupId,
                    type = type,
                    title = title,
                    message = message,
                    isRead = false,
                    createdAt = System.currentTimeMillis()
                )

                Log.d(
                    TAG,
                    "Adding notification for UID: $recipientId"
                )

                batch.set(
                    notificationDocument,
                    notification
                )
            }

            batch.commit().await()

            Log.d(
                TAG,
                "Notifications created successfully."
            )

            Result.success(Unit)

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Failed to create notifications.",
                exception
            )

            Result.failure(exception)
        }
    }

    /**
     * Observes notifications belonging to one Firebase user.
     *
     * The whereEqualTo condition is required because the
     * Firestore rules only allow a user to read documents
     * where recipientId equals their Authentication UID.
     */
    fun observeNotifications(
        userId: String
    ): Flow<List<AppNotification>> = callbackFlow {

        if (userId.isBlank()) {
            Log.w(
                TAG,
                "observeNotifications called with blank userId."
            )

            trySend(emptyList())
            close()
            return@callbackFlow
        }

        Log.d(
            TAG,
            "Starting listener for user UID: $userId"
        )

        val listenerRegistration = firestore
            .collection(NOTIFICATIONS_COLLECTION)
            .whereEqualTo(
                "recipientId",
                userId
            )
            .addSnapshotListener { snapshot, error ->

                if (error != null) {

                    Log.e(
                        TAG,
                        "Notification listener failed.",
                        error
                    )

                    close(error)
                    return@addSnapshotListener
                }

                val notifications = snapshot
                    ?.documents
                    ?.mapNotNull { document ->

                        document
                            .toObject(
                                AppNotification::class.java
                            )
                            ?.copy(
                                id = document.id
                            )
                    }
                    /*
                     * Sort locally to avoid requiring a
                     * Firestore composite index.
                     */
                    ?.sortedByDescending { notification ->
                        notification.createdAt
                    }
                    ?: emptyList()

                Log.d(
                    TAG,
                    "Loaded ${notifications.size} notifications."
                )

                trySend(notifications)
            }

        awaitClose {
            Log.d(
                TAG,
                "Removing notification listener."
            )

            listenerRegistration.remove()
        }
    }

    /**
     * Marks one notification as read.
     */
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
                .collection(NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .update(
                    "isRead",
                    true
                )
                .await()

            Log.d(
                TAG,
                "Notification marked as read: $notificationId"
            )

            Result.success(Unit)

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Failed to mark notification as read.",
                exception
            )

            Result.failure(exception)
        }
    }

    /**
     * Deletes one notification.
     */
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
                .collection(NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .delete()
                .await()

            Log.d(
                TAG,
                "Notification deleted: $notificationId"
            )

            Result.success(Unit)

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Failed to delete notification.",
                exception
            )

            Result.failure(exception)
        }
    }
}