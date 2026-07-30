package week11.st560151.finalproject.data.repository

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

    /*
     * Creates a new group.
     *
     * Registered users whose emails are entered on the create-group
     * screen are immediately added to memberIds.
     *
     * After the group is created, every invited member receives
     * a notification, except the group owner.
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
             * Find the UID belonging to each invited email.
             *
             * Emails without an existing registered account are skipped.
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
                    userRepository
                        .findUserByEmail(email)
                        .getOrNull()
                        ?.uid
                }
                .filter { uid ->
                    uid.isNotBlank()
                }
                .distinct()

            /*
             * The owner must always be a member.
             */
            val memberIds = (
                    listOf(ownerId) + invitedUids
                    ).distinct()

            val groupDocument = firestore
                .collection("groups")
                .document()

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
             * Save the group to Firestore.
             */
            groupDocument
                .set(group)
                .await()

            /*
             * STEP 4:
             * Notify all invited registered users.
             *
             * The owner does not receive this notification.
             */
            notificationRepository
                .createNotifications(
                    recipientIds = invitedUids,
                    groupId = groupDocument.id,
                    type = "GROUP_CREATED",
                    title = "New group",
                    message = "You were added to ${group.name}",
                    excludeUserId = ownerId
                )
                .getOrThrow()

            Result.success(groupDocument.id)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /*
     * Loads one group by its document ID.
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

            val group = document
                .toObject(Group::class.java)
                ?: throw IllegalStateException(
                    "Group not found."
                )

            Result.success(group)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /*
     * Adds the current user to a group using its invite code.
     *
     * STEP 5:
     * After the user joins, every existing group member receives
     * a notification, except the user who just joined.
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

            /*
             * If the user is already a member, do not add them again
             * and do not create another notification.
             */
            if (userId in group.memberIds) {
                return Result.success(group)
            }

            /*
             * Add the new member to the group's memberIds array.
             */
            document.reference
                .update(
                    "memberIds",
                    FieldValue.arrayUnion(userId)
                )
                .await()

            /*
             * Notify the members who were already in the group.
             */
            notificationRepository
                .createNotifications(
                    recipientIds = group.memberIds,
                    groupId = group.id,
                    type = "MEMBER_JOINED",
                    title = "New group member",
                    message = "A new member joined ${group.name}",
                    excludeUserId = userId
                )
                .getOrThrow()

            val updatedGroup = group.copy(
                memberIds = (
                        group.memberIds + userId
                        ).distinct()
            )

            Result.success(updatedGroup)
        } catch (exception: Exception) {
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
                    close(error)
                    return@addSnapshotListener
                }

                val groups = snapshot
                    ?.documents
                    ?.mapNotNull { document ->
                        document.toObject(
                            Group::class.java
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
     * Creates an invite code such as:
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