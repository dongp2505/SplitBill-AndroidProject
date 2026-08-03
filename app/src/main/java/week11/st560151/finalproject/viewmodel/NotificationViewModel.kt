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

    // Re-subscribes notifications for whichever account is signed in.
    private val authStateListener =
        FirebaseAuth.AuthStateListener {
                firebaseAuth ->

            val userId =
                firebaseAuth.currentUser
                    ?.uid
                    .orEmpty()

            if (userId.isBlank()) {
                stopObservingNotifications()
            } else {
                observeNotificationsForUser(
                    userId
                )
            }
        }

    init {
        auth.addAuthStateListener(
            authStateListener
        )

        val currentUserId =
            auth.currentUser
                ?.uid
                .orEmpty()

        if (currentUserId.isNotBlank()) {
            observeNotificationsForUser(
                currentUserId
            )
        }
    }

    private fun observeNotificationsForUser(
        userId: String
    ) {
        notificationJob?.cancel()

        _errorMessage.value = null

        notificationJob =
            viewModelScope.launch {
                repository
                    .observeNotifications(
                        userId
                    )
                    .catch { exception ->

                        if (
                            exception
                                    is CancellationException
                        ) {
                            throw exception
                        }

                        if (auth.currentUser == null) {
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

                        // Guards against a stale emission from an
                        // account that's since signed out/switched.
                        if (
                            auth.currentUser?.uid ==
                            userId
                        ) {
                            _notifications.value =
                                notificationList

                            _errorMessage.value =
                                null
                        }
                    }
            }
    }

    private fun stopObservingNotifications() {
        notificationJob?.cancel()
        notificationJob = null

        _notifications.value =
            emptyList()

        _errorMessage.value =
            null
    }

    // Optimistic: UI updates immediately, then persists to Firestore.
    fun markAsRead(
        notificationId: String,
        onSuccess: () -> Unit = {}
    ) {
        if (
            notificationId.isBlank() ||
            auth.currentUser == null
        ) {
            return
        }

        val previousNotifications =
            _notifications.value

        _notifications.value =
            previousNotifications.map {
                    notification ->

                if (
                    notification.id ==
                    notificationId
                ) {
                    notification.copy(
                        read = true
                    )
                } else {
                    notification
                }
            }

        viewModelScope.launch {
            repository
                .markAsRead(
                    notificationId
                )
                .onSuccess {
                    _errorMessage.value = null
                    onSuccess()
                }
                .onFailure { exception ->
                    _notifications.value =
                        previousNotifications

                    _errorMessage.value =
                        exception.message
                            ?: "Unable to mark notification as read."
                }
        }
    }

    fun markAllAsRead() {
        if (auth.currentUser == null) {
            return
        }

        val previousNotifications = _notifications.value

        val unreadIds = previousNotifications
            .filter { notification -> !notification.read }
            .map { notification -> notification.id }

        if (unreadIds.isEmpty()) {
            return
        }

        _notifications.value =
            previousNotifications.map { notification ->
                notification.copy(read = true)
            }

        viewModelScope.launch {
            repository
                .markAllAsRead(unreadIds)
                .onSuccess {
                    _errorMessage.value = null
                }
                .onFailure { exception ->
                    _notifications.value = previousNotifications

                    _errorMessage.value =
                        exception.message
                            ?: "Unable to mark notifications as read."
                }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        auth.removeAuthStateListener(
            authStateListener
        )

        notificationJob?.cancel()
        notificationJob = null

        super.onCleared()
    }
}