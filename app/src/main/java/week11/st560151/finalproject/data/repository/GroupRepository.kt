package week11.st560151.finalproject.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import week11.st560151.finalproject.data.model.Group
import java.util.UUID

class GroupRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance(),

    private val userRepository: SettlementRepository =
        SettlementRepository(),

    private val notificationRepository: NotificationRepository =
        NotificationRepository()
) {

    companion object {
        private const val TAG = "GroupRepository"
    }

    /*
     * Creates a new group.
     *
     * Registered users whose emails are entered are immediately
     * added to the group's memberIds.
     *
     * If notification creation fails, the group still remains
     * successfully created.
     */
    suspend fun createGroup(
        name: String,
        type: String,
        ownerId: String,
        inviteEmails: List<String> = emptyList()
    ): Result<String> {
        return try {

            if (name.isBlank()) {
                throw IllegalArgumentException(
                    "Group name cannot be empty."
                )
            }

            if (ownerId.isBlank()) {
                throw IllegalArgumentException(
                    "The group owner is missing."
                )
            }

            /*
             * Convert invited emails into Firebase user UIDs.
             *
             * Only users who already have a SplitBill account
             * will be immediately added.
             */
            val invitedUids = inviteEmails
                .map { email ->
                    email.trim().lowercase()
                }
                .filter { email ->
                    email.isNotBlank()
                }
                .distinct()
                .mapNotNull { email ->

                    val userResult =
                        userRepository.findUserByEmail(email)

                    val user = userResult.getOrNull()

                    if (user == null) {
                        Log.w(
                            TAG,
                            "No registered user found for email: $email"
                        )
                    }

                    user?.uid
                }
                .filter { uid ->
                    uid.isNotBlank()
                }
                .filter { uid ->
                    uid != ownerId
                }
                .distinct()

            Log.d(
                TAG,
                "Owner UID: $ownerId"
            )

            Log.d(
                TAG,
                "Invited UIDs: $invitedUids"
            )

            /*
             * The group owner must always be included.
             */
            val memberIds = (
                    listOf(ownerId) + invitedUids
                    ).distinct()

            /*
             * Create a new group document.
             */
            val groupDocument = firestore
                .collection("groups")
                .document()

            /*
             * Build the group object.
             */
            val group = Group(
                id = groupDocument.id,
                name = name.trim(),
                type = type.trim(),
                ownerId = ownerId,
                memberIds = memberIds,
                inviteCode = generateInviteCode(),
                createdAt = System.currentTimeMillis()
            )

            /*
             * Save the group first.
             */
            groupDocument
                .set(group)
                .await()

            Log.d(
                TAG,
                "Group created successfully: ${groupDocument.id}"
            )

            /*
             * Send notifications to invited users.
             *
             * Do not use getOrThrow() here.
             *
             * If notification creation fails, the group should
             * still be reported as successfully created.
             */
            val notificationResult =
                notificationRepository.createNotifications(
                    recipientIds = invitedUids,
                    groupId = groupDocument.id,
                    type = "GROUP_CREATED",
                    title = "New group",
                    message = "You were added to ${group.name}",
                    excludeUserId = ownerId
                )

            notificationResult
                .onSuccess {
                    Log.d(
                        TAG,
                        "Group invitation notifications created."
                    )
                }
                .onFailure { exception ->
                    Log.e(
                        TAG,
                        "Group was created, but notifications failed.",
                        exception
                    )
                }

            /*
             * The group was successfully created even if
             * notification creation failed.
             */
            Result.success(groupDocument.id)

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Unable to create group.",
                exception
            )

            Result.failure(exception)
        }
    }

    /*
     * Loads one group by its Firestore document ID.
     */
    suspend fun getGroup(
        groupId: String
    ): Result<Group> {
        return try {

            if (groupId.isBlank()) {
                throw IllegalArgumentException(
                    "Group ID cannot be empty."
                )
            }

            val document = firestore
                .collection("groups")
                .document(groupId)
                .get()
                .await()

            if (!document.exists()) {
                throw IllegalStateException(
                    "Group not found."
                )
            }

            val group = document
                .toObject(Group::class.java)
                ?: throw IllegalStateException(
                    "Unable to read the group."
                )

            val resolvedGroup = group.copy(
                id = group.id.ifBlank {
                    document.id
                }
            )

            Result.success(resolvedGroup)

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Unable to load group.",
                exception
            )

            Result.failure(exception)
        }
    }

    /*
     * Adds a user to a group using the invite code.
     *
     * Existing group members receive a notification.
     *
     * Notification failure will not undo the successful join.
     */
    suspend fun joinGroupByInviteCode(
        inviteCode: String,
        userId: String
    ): Result<Group> {
        return try {

            val normalizedCode = inviteCode
                .trim()
                .uppercase()

            if (normalizedCode.isBlank()) {
                throw IllegalArgumentException(
                    "Enter an invite code."
                )
            }

            if (userId.isBlank()) {
                throw IllegalArgumentException(
                    "User ID is missing."
                )
            }

            /*
             * Find the group using its invite code.
             */
            val snapshot = firestore
                .collection("groups")
                .whereEqualTo(
                    "inviteCode",
                    normalizedCode
                )
                .limit(1)
                .get()
                .await()

            val document = snapshot
                .documents
                .firstOrNull()
                ?: throw IllegalArgumentException(
                    "No group matches that invite code."
                )

            val group = document
                .toObject(Group::class.java)
                ?: throw IllegalStateException(
                    "Unable to read the group."
                )

            val resolvedGroup = group.copy(
                id = group.id.ifBlank {
                    document.id
                }
            )

            /*
             * If already a member, return without duplicating
             * the user or creating another notification.
             */
            if (userId in resolvedGroup.memberIds) {
                return Result.success(resolvedGroup)
            }

            /*
             * Add the current user UID to memberIds.
             */
            document.reference
                .update(
                    "memberIds",
                    FieldValue.arrayUnion(userId)
                )
                .await()

            Log.d(
                TAG,
                "User $userId joined group ${resolvedGroup.id}"
            )

            /*
             * Notify the members who were already in the group.
             *
             * The joining user does not receive this notification.
             */
            val notificationResult =
                notificationRepository.createNotifications(
                    recipientIds = resolvedGroup.memberIds,
                    groupId = resolvedGroup.id,
                    type = "MEMBER_JOINED",
                    title = "New group member",
                    message = "A new member joined ${resolvedGroup.name}",
                    excludeUserId = userId
                )

            notificationResult
                .onSuccess {
                    Log.d(
                        TAG,
                        "Member-joined notifications created."
                    )
                }
                .onFailure { exception ->
                    Log.e(
                        TAG,
                        "User joined, but notifications failed.",
                        exception
                    )
                }

            /*
             * Return the updated group locally.
             */
            val updatedGroup = resolvedGroup.copy(
                memberIds = (
                        resolvedGroup.memberIds + userId
                        ).distinct()
            )

            Result.success(updatedGroup)

        } catch (exception: Exception) {

            Log.e(
                TAG,
                "Unable to join group.",
                exception
            )

            Result.failure(exception)
        }
    }

    /*
     * Observes every group containing the current user's UID.
     */
    fun observeGroups(
        userId: String
    ): Flow<List<Group>> = callbackFlow {

        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore
            .collection("groups")
            .whereArrayContains(
                "memberIds",
                userId
            )
            .addSnapshotListener { snapshot, error ->

                if (error != null) {

                    Log.e(
                        TAG,
                        "Group listener failed.",
                        error
                    )

                    close(error)
                    return@addSnapshotListener
                }

                val groups = snapshot
                    ?.documents
                    ?.mapNotNull { document ->

                        document
                            .toObject(Group::class.java)
                            ?.copy(
                                id = document.id
                            )
                    }
                    ?.sortedByDescending { group ->
                        group.createdAt
                    }
                    ?: emptyList()

                trySend(groups)
            }

        awaitClose {
            listener.remove()
        }
    }

    /*
     * Generates an invite code such as:
     *
     * ABCD-1234
     */
    private fun generateInviteCode(): String {

        val rawCode = UUID
            .randomUUID()
            .toString()
            .replace("-", "")
            .take(8)
            .uppercase()

        return buildString {
            append(rawCode.take(4))
            append("-")
            append(rawCode.drop(4))
        }
    }
}