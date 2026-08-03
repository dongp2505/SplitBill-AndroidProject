package week11.st560151.finalproject.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import week11.st560151.finalproject.data.model.Group
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

private fun decodeAvatarBitmap(value: String): Bitmap? {
    return try {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

/**
 * Shows [User.photoBase64] when present, else a deterministic color+initial.
 * To stack several, use a negative `Arrangement.spacedBy` on the caller's
 * Row — the border ring keeps overlapped circles visually separated.
 */
@Composable
fun AvatarChip(
    user: User,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    borderColor: Color = MaterialTheme.colorScheme.surface
) {
    val bitmap = user.photoBase64.takeIf { it.isNotBlank() }?.let(::decodeAvatarBitmap)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColorFor(user))
            .border(2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Profile photo",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initialFor(user).toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * A group's own avatar (from [Group.avatarBase64]), falling back to the
 * group's initial. Shared by the groups list and the group detail header/
 * edit dialog so a change in one place is drawn identically everywhere.
 */
@Composable
fun GroupAvatar(group: Group?, size: Dp) {
    val bitmap = group?.avatarBase64?.takeIf { it.isNotBlank() }?.let(::decodeAvatarBitmap)
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Group avatar",
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = group?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "G",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
