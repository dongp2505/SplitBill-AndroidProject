package week11.st560151.finalproject.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import week11.st560151.finalproject.data.repository.UserRepository
import week11.st560151.finalproject.viewmodel.NotificationViewModel
import week11.st560151.finalproject.ui.components.AppBottomNav
import week11.st560151.finalproject.ui.components.BottomNavTab
import week11.st560151.finalproject.ui.components.SecondaryButton
import java.io.ByteArrayOutputStream

private const val AVATAR_MAX_DIMENSION = 200
private const val AVATAR_JPEG_QUALITY = 60

private fun uriToCompressedBase64(context: Context, uri: Uri): String? {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return null
    val original = BitmapFactory.decodeStream(inputStream) ?: return null
    inputStream.close()

    val scale = minOf(
        AVATAR_MAX_DIMENSION.toFloat() / original.width,
        AVATAR_MAX_DIMENSION.toFloat() / original.height,
        1f
    )
    val resized = Bitmap.createScaledBitmap(
        original,
        (original.width * scale).toInt().coerceAtLeast(1),
        (original.height * scale).toInt().coerceAtLeast(1),
        true
    )

    val outputStream = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.JPEG, AVATAR_JPEG_QUALITY, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}

private fun base64ToBitmap(base64: String): Bitmap? {
    return try {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun ProfileScreen(
    onGroupsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSignOutClick: () -> Unit,
    userRepository: UserRepository = UserRepository(),
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val notifications by notificationViewModel.notifications.collectAsState()
    val unreadNotificationCount = notifications.count { notification ->
        !notification.read
    }

    var refreshTrigger by remember { mutableIntStateOf(0) }
    val currentUser = remember(refreshTrigger) { FirebaseAuth.getInstance().currentUser }

    val displayName = currentUser?.displayName?.ifBlank { null } ?: "SplitBill user"
    val email = currentUser?.email.orEmpty()
    val initial = (currentUser?.displayName?.firstOrNull()
        ?: currentUser?.email?.firstOrNull()
        ?: '?').uppercaseChar()

    var photoBase64 by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshTrigger, currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        userRepository.getUser(uid).onSuccess { user ->
            photoBase64 = user?.photoBase64?.ifBlank { null }
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null && currentUser != null) {
            isSaving = true
            errorMessage = null
            scope.launch {
                val encoded = uriToCompressedBase64(context, uri)
                if (encoded == null) {
                    errorMessage = "Couldn't read that image"
                    isSaving = false
                    return@launch
                }
                userRepository.updateProfile(currentUser.uid, photoBase64 = encoded)
                    .onSuccess { refreshTrigger++ }
                    .onFailure { e -> errorMessage = e.localizedMessage ?: "Failed to save avatar" }
                isSaving = false
            }
        }
    }

    Scaffold(
        bottomBar = {
            AppBottomNav(
                selected = BottomNavTab.Profile,
                unreadNotificationCount = unreadNotificationCount,
                onSelect = { tab ->
                    when (tab) {
                        BottomNavTab.Groups -> onGroupsClick()
                        BottomNavTab.Notifications -> onNotificationsClick()
                        BottomNavTab.Profile -> Unit
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = photoBase64?.let { base64ToBitmap(it) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Profile photo",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(text = initial.toString(), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(text = displayName, fontWeight = FontWeight.Bold)

                    Text(
                        text = email,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SecondaryButton(
                text = "Edit Profile",
                onClick = { showEditDialog = true }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SecondaryButton(
                text = "Change Avatar",
                onClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            if (isSaving) {
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            SecondaryButton(
                text = "Sign out",
                onClick = onSignOutClick
            )
        }
    }

    if (showEditDialog) {
        var nameInput by remember {
            mutableStateOf(currentUser?.displayName.orEmpty())
        }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditDialog = false },
            title = { Text("Edit Profile") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Display name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        val newName = nameInput.trim()
                        if (newName.isNotEmpty() && currentUser != null) {
                            isSaving = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    currentUser.updateProfile(
                                        UserProfileChangeRequest.Builder()
                                            .setDisplayName(newName)
                                            .build()
                                    ).await()
                                    userRepository.updateProfile(currentUser.uid, displayName = newName)
                                    refreshTrigger++
                                } catch (e: Exception) {
                                    errorMessage = e.localizedMessage ?: "Failed to update name"
                                } finally {
                                    isSaving = false
                                    showEditDialog = false
                                }
                            }
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = { showEditDialog = false }
                ) { Text("Cancel") }
            }
        )
    }
}