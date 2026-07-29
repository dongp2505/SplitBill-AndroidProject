package week11.st560151.finalproject.data.model

data class Expense(
    val id: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val paidBy: String = "",
    val participantIds: List<String> = emptyList(),
    val shares: Map<String, Double> = emptyMap(),
    val createdBy: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)