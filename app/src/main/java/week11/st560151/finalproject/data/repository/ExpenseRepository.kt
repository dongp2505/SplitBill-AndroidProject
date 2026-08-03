package week11.st560151.finalproject.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import week11.st560151.finalproject.data.model.Expense

class ExpenseRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance(),

    private val notificationRepository:
    NotificationRepository =
        NotificationRepository()
) {

    private fun collection() =
        firestore.collection("expenses")

    suspend fun createExpense(
        groupId: String,
        description: String,
        amount: Double,
        category: String,
        paidBy: String,
        participantIds: List<String>,
        shares: Map<String, Double>,
        createdBy: String,
        receiptBase64: String = ""
    ): Result<String> {
        return try {
            if (description.isBlank()) {
                throw IllegalArgumentException(
                    "Description is required."
                )
            }

            if (amount <= 0.0) {
                throw IllegalArgumentException(
                    "Amount must be greater than zero."
                )
            }

            if (participantIds.isEmpty()) {
                throw IllegalArgumentException(
                    "Select at least one participant."
                )
            }

            if (groupId.isBlank()) {
                throw IllegalArgumentException(
                    "Group ID is missing."
                )
            }

            if (createdBy.isBlank()) {
                throw IllegalArgumentException(
                    "Creator UID is missing."
                )
            }

            val document =
                collection().document()

            val expense =
                Expense(
                    id = document.id,
                    groupId = groupId,
                    description =
                        description.trim(),
                    amount = amount,
                    category = category,
                    paidBy = paidBy,
                    participantIds =
                        participantIds,
                    shares = shares,
                    receiptBase64 = receiptBase64,
                    createdBy = createdBy,
                    createdAt =
                        System.currentTimeMillis(),
                    updatedAt =
                        System.currentTimeMillis()
                )

            document
                .set(expense)
                .await()

            val groupDocument =
                firestore
                    .collection("groups")
                    .document(groupId)
                    .get()
                    .await()

            val memberIds =
                (
                        groupDocument.get(
                            "memberIds"
                        ) as? List<*>
                        )
                    ?.filterIsInstance<String>()
                    ?.filter { memberId ->
                        memberId.isNotBlank()
                    }
                    ?.distinct()
                    ?: emptyList()

            val formattedAmount =
                String.format(
                    java.util.Locale.US,
                    "%.2f",
                    amount
                )

            notificationRepository
                .createNotifications(
                    recipientIds = memberIds,
                    groupId = groupId,
                    type = "EXPENSE_ADDED",
                    title = "New expense",
                    message =
                        "${description.trim()} - " +
                                "\$$formattedAmount",
                    excludeUserId = createdBy
                )
                .getOrThrow()

            Result.success(
                document.id
            )

        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun updateExpense(
        expense: Expense
    ): Result<Unit> {
        return try {
            if (expense.id.isBlank()) {
                throw IllegalArgumentException(
                    "Expense ID is missing."
                )
            }

            // Ownership is enforced server-side by the Firestore rule.
            collection()
                .document(expense.id)
                .set(
                    expense.copy(
                        updatedAt =
                            System.currentTimeMillis()
                    )
                )
                .await()

            Result.success(Unit)

        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun deleteExpense(
        expenseId: String
    ): Result<Unit> {
        return try {
            if (expenseId.isBlank()) {
                throw IllegalArgumentException(
                    "Expense ID is missing."
                )
            }

            collection()
                .document(expenseId)
                .delete()
                .await()

            Result.success(Unit)

        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun getExpensesOnce(
        groupId: String
    ): Result<List<Expense>> {
        return try {
            val snapshot =
                collection()
                    .whereEqualTo(
                        "groupId",
                        groupId
                    )
                    .get()
                    .await()

            val expenses =
                snapshot.documents
                    .mapNotNull { document ->
                        document
                            .toObject(
                                Expense::class.java
                            )
                            ?.copy(
                                id = document.id
                            )
                    }

            Result.success(expenses)

        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    fun observeExpenses(
        groupId: String
    ): Flow<List<Expense>> =
        callbackFlow {

            val listener =
                collection()
                    .whereEqualTo(
                        "groupId",
                        groupId
                    )
                    .addSnapshotListener {
                            snapshot,
                            error ->

                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }

                        val expenses =
                            snapshot
                                ?.documents
                                ?.mapNotNull {
                                        document ->

                                    document
                                        .toObject(
                                            Expense::class.java
                                        )
                                        ?.copy(
                                            id =
                                                document.id
                                        )
                                }
                                ?.sortedByDescending {
                                        expense ->
                                    expense.createdAt
                                }
                                ?: emptyList()

                        trySend(expenses)
                    }

            awaitClose {
                listener.remove()
            }
        }
}