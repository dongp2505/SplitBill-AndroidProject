package week11.st560151.finalproject.util

import week11.st560151.finalproject.data.model.Expense
import week11.st560151.finalproject.data.model.Settlement

/**
 * Pure math shared by the Groups landing list and a GroupExpand screen's
 * "Your position" / Balances tab — no Firestore calls in here.
 */
object BalanceCalculator {

    /** Net balance for [userId]: positive = owed to them, negative = they owe. */
    fun netBalance(
        expenses: List<Expense>,
        settlements: List<Settlement>,
        userId: String
    ): Double {
        val expenseBalance = expenses.sumOf { expense ->
            val paid = if (expense.paidBy == userId) expense.amount else 0.0
            val owed = expense.shares[userId] ?: 0.0
            paid - owed
        }

        // Paying off a debt (as payerId) moves your balance toward zero from
        // below; being paid (as receiverId) moves it toward zero from above.
        val settlementAdjustment = settlements.sumOf { settlement ->
            when (userId) {
                settlement.payerId -> settlement.amount
                settlement.receiverId -> -settlement.amount
                else -> 0.0
            }
        }

        return expenseBalance + settlementAdjustment
    }

    /** Net balance for every member of the group. */
    fun netBalances(
        memberIds: List<String>,
        expenses: List<Expense>,
        settlements: List<Settlement>
    ): Map<String, Double> {
        return memberIds.associateWith { netBalance(expenses, settlements, it) }
    }

    data class Suggestion(
        val fromUserId: String,
        val toUserId: String,
        val amount: Double
    )

    /**
     * Greedy debt simplification: largest debtor pays largest creditor,
     * repeat. Not a true minimal-transaction solver, but a reasonable first
     * draft for "suggested settlements".
     */
    fun suggestSettlements(
        balances: Map<String, Double>,
        epsilon: Double = 0.01
    ): List<Suggestion> {
        val debtors = balances
            .filter { it.value < -epsilon }
            .map { it.key to -it.value }
            .sortedByDescending { it.second }
            .toMutableList()

        val creditors = balances
            .filter { it.value > epsilon }
            .map { it.key to it.value }
            .sortedByDescending { it.second }
            .toMutableList()

        val suggestions = mutableListOf<Suggestion>()
        var i = 0
        var j = 0

        while (i < debtors.size && j < creditors.size) {
            val (debtorId, debtAmount) = debtors[i]
            val (creditorId, creditAmount) = creditors[j]

            val settleAmount = minOf(debtAmount, creditAmount)

            if (settleAmount > epsilon) {
                suggestions.add(Suggestion(debtorId, creditorId, settleAmount))
            }

            debtors[i] = debtorId to (debtAmount - settleAmount)
            creditors[j] = creditorId to (creditAmount - settleAmount)

            if (debtors[i].second <= epsilon) i++
            if (creditors[j].second <= epsilon) j++
        }

        return suggestions
    }
}
