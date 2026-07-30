package week11.st560151.finalproject.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import week11.st560151.finalproject.data.model.Settlement
import week11.st560151.finalproject.data.model.User
import java.util.Locale

class SettlementRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance(),

    private val notificationRepository: NotificationRepository =
        NotificationRepository()
) {

    /*
     * Finds a registered user using their email address.
     */
    suspend fun findUserByEmail(
        email: String
    ): Result<User> {
        return try {
            val normalizedEmail = email
                .trim()
                .lowercase()

            if (normalizedEmail.isBlank()) {
                throw IllegalArgumentException(
                    "Email cannot be empty."
                )
            }

            val snapshot = firestore
                .collection("users")
                .whereEqualTo(
                    "email",
                    normalizedEmail
                )
                .limit(1)
                .get()
                .await()

            val document = snapshot
                .documents
                .firstOrNull()
                ?: throw IllegalArgumentException(
                    "No registered user was found with $normalizedEmail."
                )

            val user = document
                .toObject(User::class.java)
                ?: throw IllegalStateException(
                    "Unable to read the user profile."
                )

            /*
             * Some older user documents may not contain uid as a field.
             * In that case, use the Firestore document ID.
             */
            val resolvedUser = user.copy(
                uid = user.uid.ifBlank {
                    document.id
                }
            )

            Result.success(resolvedUser)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /*
     * Saves a completed settlement.
     *
     * After saving it, every member of the group receives
     * a notification except the user who submitted the settlement.
     */
    suspend fun saveSettlement(
        groupId: String,
        payer: User,
        receiver: User,
        amount: Double,
        createdById: String,
        createdByName: String
    ): Result<String> {
        return try {
            if (groupId.isBlank()) {
                throw IllegalArgumentException(
                    "Group ID is missing."
                )
            }

            if (payer.uid.isBlank()) {
                throw IllegalArgumentException(
                    "Payer ID is missing."
                )
            }

            if (receiver.uid.isBlank()) {
                throw IllegalArgumentException(
                    "Receiver ID is missing."
                )
            }

            if (payer.uid == receiver.uid) {
                throw IllegalArgumentException(
                    "Payer and receiver cannot be the same user."
                )
            }

            if (amount <= 0.0) {
                throw IllegalArgumentException(
                    "Settlement amount must be greater than zero."
                )
            }

            val settlementDocument = firestore
                .collection("settlements")
                .document()

            val payerDisplayName =
                payer.displayName.ifBlank {
                    payer.email
                }

            val receiverDisplayName =
                receiver.displayName.ifBlank {
                    receiver.email
                }

            val settlement = Settlement(
                id = settlementDocument.id,
                groupId = groupId,

                payerId = payer.uid,
                payerName = payerDisplayName,
                payerEmail = payer.email,

                receiverId = receiver.uid,
                receiverName = receiverDisplayName,
                receiverEmail = receiver.email,

                amount = amount,
                status = "COMPLETED",

                createdById = createdById,
                createdByName = createdByName,

                confirmedWithBiometric = true,
                createdAt = System.currentTimeMillis()
            )

            /*
             * Save the settlement first.
             */
            settlementDocument
                .set(settlement)
                .await()

            /*
             * Load the group so that we can get all member UIDs.
             */
            val groupDocument = firestore
                .collection("groups")
                .document(groupId)
                .get()
                .await()

            if (!groupDocument.exists()) {
                throw IllegalStateException(
                    "The group could not be found."
                )
            }

            /*
             * Firestore arrays may be returned as List<*>,
             * so filterIsInstance safely extracts the strings.
             */
            val memberIds = (
                    groupDocument.get("memberIds") as? List<*>
                    )
                ?.filterIsInstance<String>()
                ?.filter { memberId ->
                    memberId.isNotBlank()
                }
                ?.distinct()
                ?: emptyList()

            val formattedAmount = String.format(
                Locale.US,
                "%.2f",
                amount
            )

            /*
             * Notify group members.
             *
             * The person who submitted the settlement is excluded.
             */
            notificationRepository
                .createNotifications(
                    recipientIds = memberIds,
                    groupId = groupId,
                    type = "SETTLEMENT_COMPLETED",
                    title = "Payment settled",
                    message = "$payerDisplayName settled " +
                            "\$$formattedAmount with $receiverDisplayName",
                    excludeUserId = createdById
                )
                .getOrThrow()

            Result.success(settlementDocument.id)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /*
     * Loads all settlements belonging to one group.
     *
     * This is used by your balance calculator.
     */
    suspend fun getSettlementsOnce(
        groupId: String
    ): Result<List<Settlement>> {
        return try {
            if (groupId.isBlank()) {
                return Result.success(emptyList())
            }

            val snapshot = firestore
                .collection("settlements")
                .whereEqualTo(
                    "groupId",
                    groupId
                )
                .get()
                .await()

            val settlements = snapshot
                .documents
                .mapNotNull { document ->
                    document.toObject(
                        Settlement::class.java
                    )
                }
                .sortedByDescending { settlement ->
                    settlement.createdAt
                }

            Result.success(settlements)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}