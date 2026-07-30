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
     * Finds a registered SplitBill user by email.
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
             * Some older user documents might not contain
             * the UID field.
             *
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
     * After saving the settlement, this function creates
     * notifications for the other group members.
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

            /*
             * Validate group ID.
             */
            if (groupId.isBlank()) {
                throw IllegalArgumentException(
                    "Group ID is missing."
                )
            }

            /*
             * Validate the payer.
             */
            if (payer.uid.isBlank()) {
                throw IllegalArgumentException(
                    "Payer ID is missing."
                )
            }

            /*
             * Validate the receiver.
             */
            if (receiver.uid.isBlank()) {
                throw IllegalArgumentException(
                    "Receiver ID is missing."
                )
            }

            /*
             * A user cannot settle with themselves.
             */
            if (payer.uid == receiver.uid) {
                throw IllegalArgumentException(
                    "Payer and receiver cannot be the same user."
                )
            }

            /*
             * Settlement amount must be positive.
             */
            if (amount <= 0.0) {
                throw IllegalArgumentException(
                    "Settlement amount must be greater than zero."
                )
            }

            /*
             * Create a new Firestore document reference.
             */
            val settlementDocument = firestore
                .collection("settlements")
                .document()

            /*
             * Use the user's display name.
             *
             * If the display name is empty, use their email.
             */
            val payerDisplayName =
                payer.displayName.ifBlank {
                    payer.email
                }

            val receiverDisplayName =
                receiver.displayName.ifBlank {
                    receiver.email
                }

            /*
             * Create the Settlement object.
             */
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
             * Save the settlement to Firestore.
             */
            settlementDocument
                .set(settlement)
                .await()

            /*
             * Load the group document.
             *
             * We need the group member IDs so we know
             * which users should receive notifications.
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
             * Read the memberIds array from the group.
             *
             * Firestore returns arrays as List<*>, so
             * filterIsInstance safely gets String values.
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

            /*
             * Format the settlement amount with two decimals.
             *
             * Example:
             * 20 becomes 20.00
             */
            val formattedAmount = String.format(
                Locale.US,
                "%.2f",
                amount
            )

            /*
             * Create a notification for the other group members.
             *
             * The user who submitted the settlement is excluded
             * because they already know that it was completed.
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

            /*
             * Return the ID of the saved settlement.
             */
            Result.success(settlementDocument.id)

        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /*
     * Gets all completed settlements for one group.
     *
     * This data can be used when calculating group balances.
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

                    val settlement = document.toObject(
                        Settlement::class.java
                    )

                    settlement?.copy(
                        id = settlement.id.ifBlank {
                            document.id
                        }
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