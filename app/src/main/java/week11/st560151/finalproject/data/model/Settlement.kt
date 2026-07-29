package week11.st560151.finalproject.data.model

data class Settlement(
    val id: String = "",
    val groupId: String = "",

    val payerId: String = "",
    val payerName: String = "",
    val payerEmail: String = "",

    val receiverId: String = "",
    val receiverName: String = "",
    val receiverEmail: String = "",

    val amount: Double = 0.0,
    val status: String = "COMPLETED",

    val createdById: String = "",
    val createdByName: String = "",

    val confirmedWithBiometric: Boolean = false,
    val createdAt: Long = 0L
)