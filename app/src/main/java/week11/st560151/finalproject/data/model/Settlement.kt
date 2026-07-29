package week11.st560151.finalproject.data.model

data class Settlement(
    val id: String = "",
    val payerId: String = "",
    val payeeId: String = "",
    val amount: Double = 0.0,
    val status: String = "PENDING",
    val payerConfirmed: Boolean = false,
    val payeeConfirmed: Boolean = false,
    val createdAt: Long = 0L,
    val completedAt: Long? = null
)