package week11.st560151.finalproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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

    fun observeCurrentUserNotifications() {
        notificationJob?.cancel()

        val currentUserId =
            auth.currentUser
                ?.uid
                .orEmpty()

        if (currentUserId.isBlank()) {
            _notifications.value =
                emptyList()

            _errorMessage.value =
                null

            return
        }

        notificationJob =
            viewModelScope.launch {
                repository
                    .observeNotifications(
                        currentUserId
                    )
                    .catch { exception ->
                        /*
                         * Cancellation is normal when navigating
                         * away or signing out.
                         */
                        if (
                            exception
                                    is CancellationException
                        ) {
                            throw exception
                        }

                        /*
                         * Do not crash when Firebase Auth changes
                         * while the listener is closing.
                         */
                        if (
                            auth.currentUser == null
                        ) {
                            _notifications.value =
                                emptyList()

                            _errorMessage.value =
                                null
                        } else {
                            _errorMessage.value =
                                exception.message
                                    ?: "Unable to load notifications."
                        }
                    }
                    .collect {
                            notificationList ->

                        _notifications.value =
                            notificationList

                        _errorMessage.value =
                            null
                    }
            }
    }

    fun markAsRead(
        notificationId: String
    ) {
        if (notificationId.isBlank()) {
            return
        }

        if (auth.currentUser == null) {
            return
        }

        viewModelScope.launch {
            repository
                .markAsRead(
                    notificationId
                )
                .onFailure {
                        exception ->

                    if (auth.currentUser != null) {
                        _errorMessage.value =
                            exception.message
                                ?: "Unable to update notification."
                    }
                }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        notificationJob?.cancel()
        notificationJob = null

        super.onCleared()
    }
}