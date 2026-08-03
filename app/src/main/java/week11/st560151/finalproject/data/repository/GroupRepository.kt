package week11.st560151.finalproject.data.repository

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import week11.st560151.finalproject.data.model.Group
import week11.st560151.finalproject.data.model.User
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

    // Unregistered invite emails are silently skipped; notification
    // failure doesn't fail group creation.
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

            // Only emails with a registered account resolve to a uid.
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

            groupDocument
                .set(group)
                .await()

            Log.d(
                TAG,
                "Group created successfully: ${groupDocument.id}"
            )

            // No getOrThrow(): notification failure shouldn't fail creation.
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

    // Notification failure doesn't undo the join.
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

            val resolvedGroup = group.copy(
                id = group.id.ifBlank {
                    document.id
                }
            )

            // Idempotent: already-members aren't re-added or re-notified.
            if (userId in resolvedGroup.memberIds) {
                return Result.success(resolvedGroup)
            }

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


    suspend fun updateGroupName(groupId: String, newName: String): Result<Unit> = runCatching {
        val cleanName = newName.trim()
        require(groupId.isNotBlank()) { "Group ID is missing." }
        require(cleanName.isNotBlank()) { "Group name cannot be empty." }
        firestore.collection("groups").document(groupId).update("name", cleanName).await()
    }

    suspend fun updateGroupAvatar(groupId: String, avatarBase64: String): Result<Unit> = runCatching {
        require(groupId.isNotBlank()) { "Group ID is missing." }
        firestore.collection("groups").document(groupId)
            .update("avatarBase64", avatarBase64).await()
    }

    suspend fun addMemberByEmail(
        groupId: String,
        email: String
    ): Result<User> {
        return try {
            val normalizedEmail =
                email.trim().lowercase()

            require(groupId.isNotBlank()) {
                "Group ID is missing."
            }

            require(normalizedEmail.isNotBlank()) {
                "Enter a member email."
            }

            val userResult =
                userRepository.findUserByEmail(
                    normalizedEmail
                )

            val user =
                userResult.getOrElse {
                    throw it
                }

            require(user.uid.isNotBlank()) {
                "The user UID is missing."
            }

            val group =
                getGroup(groupId).getOrElse {
                    throw it
                }

            require(
                user.uid !in group.memberIds
            ) {
                "This user is already a member."
            }

            // await() first: notification rules require the recipient
            // already be in memberIds.
            firestore
                .collection("groups")
                .document(groupId)
                .update(
                    "memberIds",
                    FieldValue.arrayUnion(
                        user.uid
                    )
                )
                .await()

            Log.d(
                TAG,
                "Member ${user.uid} added to group $groupId"
            )

            // Notification failure doesn't undo the add.
            val notificationResult =
                notificationRepository
                    .createNotifications(
                        recipientIds =
                            listOf(user.uid),
                        groupId = groupId,
                        type = "MEMBER_ADDED",
                        title = "Added to group",
                        message =
                            "You were added to ${group.name}",
                        excludeUserId = null
                    )

            notificationResult
                .onSuccess {
                    Log.d(
                        TAG,
                        "Member-added notification created."
                    )
                }
                .onFailure { exception ->
                    Log.e(
                        TAG,
                        "Member added, but notification failed.",
                        exception
                    )
                }

            Result.success(user)

        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Unable to add member.",
                exception
            )

            Result.failure(exception)
        }
    }

    suspend fun removeMember(groupId: String, memberId: String): Result<Unit> = runCatching {
        require(groupId.isNotBlank()) { "Group ID is missing." }
        require(memberId.isNotBlank()) { "Member ID is missing." }
        val group = getGroup(groupId).getOrElse { throw it }
        require(memberId in group.memberIds) { "This user is not a member." }
        require(group.memberIds.size > 1) { "The final member cannot be removed." }
        firestore.collection("groups").document(groupId)
            .update("memberIds", FieldValue.arrayRemove(memberId)).await()
    }

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

    // e.g. "ABCD-1234"
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