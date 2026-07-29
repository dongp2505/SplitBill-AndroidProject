package week11.st560151.finalproject.model

import com.google.firebase.Timestamp

data class Settlement(
    val id: String = "",
    val groupId: String = "",
    val payerId: String = "",
    val payerName: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val amount: Double = 0.0,
    val status: String = "COMPLETED",
    val confirmedWithBiometric: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)