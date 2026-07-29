package week11.st560151.finalproject.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import week11.st560151.finalproject.data.model.User

private val AVATAR_COLORS = listOf(
    Color(0xFFF6D775), Color(0xFFF3A6C1), Color(0xFFB9A6F3),
    Color(0xFFA6D9F3), Color(0xFFA6F3C4), Color(0xFFF3B8A6)
)

/**
 * Same person always gets the same color/initial everywhere in the app —
 * derived from their uid (falling back to email), never from list position.
 */
fun avatarColorFor(user: User): Color {
    val key = user.uid.ifBlank { user.email }
    val index = (key.hashCode() and Int.MAX_VALUE) % AVATAR_COLORS.size
    return AVATAR_COLORS[index]
}

private fun initialFor(user: User): Char {
    return (user.displayName.firstOrNull() ?: user.email.firstOrNull() ?: '?')
        .uppercaseChar()
}

/**
 * A single member's avatar. Pass a negative [overlap] via the caller's Row
 * `Arrangement.spacedBy` (not here) to stack several of these; the white
 * border ring is what keeps overlapped circles visually separated.
 */
@Composable
fun AvatarChip(
    user: User,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    borderColor: Color = MaterialTheme.colorScheme.surface
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColorFor(user))
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialFor(user).toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
