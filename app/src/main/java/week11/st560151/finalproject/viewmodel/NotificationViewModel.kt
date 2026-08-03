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

    /*
     * Restart the notification listener whenever
     * the Firebase account changes.
     */
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

                        /*
                         * Ignore results from a previously
                         * signed-in account.
                         */
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

    /*
     * Permanently marks one notification as read.
     *
     * The UI changes immediately, but it is also
     * saved permanently in Firestore.
     */
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

        /*
         * Change the UI immediately.
         */
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

                    /*
                     * Restore the unread state when
                     * Firestore rejects the update.
                     */
                    _notifications.value =
                        previousNotifications

                    _errorMessage.value =
                        exception.message
                            ?: "Unable to mark notification as read."
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