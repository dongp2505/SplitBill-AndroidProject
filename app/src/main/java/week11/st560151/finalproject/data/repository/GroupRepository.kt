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
        SettlementRepository()
) {

    suspend fun createGroup(
        name: String,
        type: String,
        ownerId: String,
        inviteEmails: List<String> = emptyList()
    ): Result<String> {
        return try {
            // Emails that match an existing account are added as members right
            // away; unmatched ones are silently skipped (no email-sending
            // service is wired up — the invite code is the real join path).
            val invitedUids = inviteEmails
                .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
                .mapNotNull { email ->
                    userRepository.findUserByEmail(email).getOrNull()?.uid
                }

            val memberIds = (listOf(ownerId) + invitedUids).distinct()

            val document = firestore
                .collection("groups")
                .document()

            val group = Group(
                id = document.id,
                name = name.trim(),
                type = type,
                ownerId = ownerId,
                memberIds = memberIds,
                inviteCode = generateInviteCode(),
                createdAt = System.currentTimeMillis()
            )

            document.set(group).await()

            Result.success(document.id)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun getGroup(
        groupId: String
    ): Result<Group> {
        return try {
            val document = firestore
                .collection("groups")
                .document(groupId)
                .get()
                .await()

            val group = document.toObject(Group::class.java)
                ?: throw IllegalStateException("Group not found.")

            Result.success(group)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /** Adds [userId] to the group matching [inviteCode], if one exists. */
    suspend fun joinGroupByInviteCode(
        inviteCode: String,
        userId: String
    ): Result<Group> {
        return try {
            val normalizedCode = inviteCode.trim().uppercase()

            if (normalizedCode.isBlank()) {
                throw IllegalArgumentException("Enter an invite code.")
            }

            val snapshot = firestore
                .collection("groups")
                .whereEqualTo("inviteCode", normalizedCode)
                .limit(1)
                .get()
                .await()

            val document = snapshot.documents.firstOrNull()
                ?: throw IllegalArgumentException("No group matches that invite code.")

            val group = document.toObject(Group::class.java)
                ?: throw IllegalStateException("Unable to read the group.")

            if (userId in group.memberIds) {
                return Result.success(group)
            }

            document.reference
                .update("memberIds", FieldValue.arrayUnion(userId))
                .await()

            Result.success(group.copy(memberIds = group.memberIds + userId))
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

    private fun generateInviteCode(): String {
        val raw = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .take(8)
            .uppercase()

        return "${raw.take(4)}-${raw.drop(4)}"
    }
}
