package week11.st560151.finalproject.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import week11.st560151.finalproject.data.model.Expense

class ExpenseRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
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
        createdBy: String
    ): Result<String> {
        return try {
            if (description.isBlank()) {
                throw IllegalArgumentException("Description is required.")
            }

            if (amount <= 0.0) {
                throw IllegalArgumentException("Amount must be greater than zero.")
            }

            if (participantIds.isEmpty()) {
                throw IllegalArgumentException("Select at least one participant.")
            }

            val document = collection().document()

            val expense = Expense(
                id = document.id,
                groupId = groupId,
                description = description.trim(),
                amount = amount,
                category = category,
                paidBy = paidBy,
                participantIds = participantIds,
                shares = shares,
                createdBy = createdBy,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            document.set(expense).await()

            Result.success(document.id)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun updateExpense(
        expense: Expense
    ): Result<Unit> {
        return try {
            if (expense.id.isBlank()) {
                throw IllegalArgumentException("Expense id is missing.")
            }

            collection()
                .document(expense.id)
                .set(expense.copy(updatedAt = System.currentTimeMillis()))
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
            collection()
                .document(expenseId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /** One-shot fetch used for balance calculations (no live listener needed). */
    suspend fun getExpensesOnce(
        groupId: String
    ): Result<List<Expense>> {
        return try {
            val snapshot = collection()
                .whereEqualTo("groupId", groupId)
                .get()
                .await()

            Result.success(
                snapshot.documents.mapNotNull { it.toObject(Expense::class.java) }
            )
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    /** Real-time feed of a group's expenses, most recent first. */
    fun observeExpenses(
        groupId: String
    ): Flow<List<Expense>> = callbackFlow {
        val listener = collection()
            .whereEqualTo("groupId", groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val expenses = snapshot
                    ?.documents
                    ?.mapNotNull { it.toObject(Expense::class.java) }
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()

                trySend(expenses)
            }

        awaitClose { listener.remove() }
    }
}
