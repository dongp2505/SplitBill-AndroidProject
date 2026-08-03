package week11.st560151.finalproject.data.model

data class AppNotification(
    val id: String = "",
    val recipientId: String = "",
    val groupId: String = "",
    val type: String = "",
    val title: String = "",
    val message: String = "",
    val read: Boolean = false,
    val createdAt: Long = 0L
)