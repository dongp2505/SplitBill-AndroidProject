package week11.st560151.finalproject.data.model

data class Group(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val ownerId: String = "",
    val memberIds: List<String> = emptyList(),
    val inviteCode: String = "",
    val avatarBase64: String = "",
    val createdAt: Long = 0L
)