package week11.st560151.finalproject.data.model

data class Group(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    val inviteCode: String = "",
    val createdAt: Long = 0L
)