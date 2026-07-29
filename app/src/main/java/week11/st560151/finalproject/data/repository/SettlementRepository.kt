package week11.st560151.finalproject.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import week11.st560151.finalproject.data.model.Settlement
import week11.st560151.finalproject.data.model.User

class SettlementRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    suspend fun findUserByEmail(
        email: String
    ): Result<User> {
        return try {
            val normalizedEmail =
                email.trim().lowercase()

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

            val document =
                snapshot.documents.firstOrNull()
                    ?: throw IllegalArgumentException(
                        "No registered user was found with $normalizedEmail."
                    )

            val user =
                document.toObject(User::class.java)
                    ?: throw IllegalStateException(
                        "Unable to read the user profile."
                    )

            val resolvedUser = user.copy(
                uid = if (user.uid.isBlank()) {
                    document.id
                } else {
                    user.uid
                }
            )

            Result.success(resolvedUser)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun saveSettlement(
        groupId: String,
        payer: User,
        receiver: User,
        amount: Double,
        createdById: String,
        createdByName: String
    ): Result<String> {
        return try {
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

            val document = firestore
                .collection("settlements")
                .document()

            val settlement = Settlement(
                id = document.id,
                groupId = groupId,

                payerId = payer.uid,
                payerName = payer.displayName,
                payerEmail = payer.email,

                receiverId = receiver.uid,
                receiverName = receiver.displayName,
                receiverEmail = receiver.email,

                amount = amount,
                status = "COMPLETED",

                createdById = createdById,
                createdByName = createdByName,

                confirmedWithBiometric = true,
                createdAt = System.currentTimeMillis()
            )

            document
                .set(settlement)
                .await()

            Result.success(document.id)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}