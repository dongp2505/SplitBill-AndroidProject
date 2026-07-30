package week11.st560151.finalproject.data.model

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoBase64: String = "",
    val createdAt: Long = 0L
)