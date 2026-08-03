package week11.st560151.finalproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import week11.st560151.finalproject.data.model.AppNotification
import week11.st560151.finalproject.data.repository.NotificationRepository

class NotificationViewModel(
    private val repository:
    NotificationRepository =
        NotificationRepository(),

    private val auth:
    FirebaseAuth =
        FirebaseAuth.getInstance()
) : ViewModel() {

    private val _notifications =
        MutableStateFlow<List<AppNotification>>(
            emptyList()
        )

    val notifications:
            StateFlow<List<AppNotification>> =
        _notifications.asStateFlow()

    private val _errorMessage =
        MutableStateFlow<String?>(null)

    val errorMessage:
            StateFlow<String?> =
        _errorMessage.asStateFlow()

    private var notificationJob: Job? = null

    init {
        observeCurrentUserNotifications()
    }

    /*
     * Reads only documents where recipientId matches
     * the signed-in Firebase Authentication UID.
     */
    fun observeCurrentUserNotifications() {
        val currentUserId =
            auth.currentUser?.uid.orEmpty()

        notificationJob?.cancel()

        if (currentUserId.isBlank()) {
            _notifications.value =
                emptyList()

            _errorMessage.value =
                "User is not signed in."

            return
        }

        notificationJob =
            viewModelScope.launch {
                repository
                    .observeNotifications(
                        currentUserId
                    )
                    .collect { notificationList ->

                        _notifications.value =
                            notificationList

                        _errorMessage.value =
                            null
                    }
            }

        notificationJob
            ?.invokeOnCompletion { error ->

                if (error != null) {
                    _errorMessage.value =
                        error.message
                            ?: "Unable to load notifications."
                }
            }
    }

    fun markAsRead(
        notificationId: String
    ) {
        if (notificationId.isBlank()) {
            return
        }

        viewModelScope.launch {
            repository
                .markAsRead(
                    notificationId
                )
                .onFailure { exception ->

                    _errorMessage.value =
                        exception.message
                            ?: "Unable to update notification."
                }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        notificationJob?.cancel()
        super.onCleared()
    }
}