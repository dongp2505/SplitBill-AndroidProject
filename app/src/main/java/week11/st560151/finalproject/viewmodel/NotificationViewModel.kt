package week11.st560151.finalproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import week11.st560151.finalproject.data.model.AppNotification
import week11.st560151.finalproject.data.repository.NotificationRepository

class NotificationViewModel(
    private val repository: NotificationRepository = NotificationRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _notifications =
        MutableStateFlow<List<AppNotification>>(emptyList())

    val notifications: StateFlow<List<AppNotification>> =
        _notifications.asStateFlow()

    private val _errorMessage =
        MutableStateFlow<String?>(null)

    val errorMessage: StateFlow<String?> =
        _errorMessage.asStateFlow()

    private var notificationJob: Job? = null

    init {
        startObserving()
    }

    private fun startObserving() {

        val userId = auth.currentUser?.uid

        if (userId == null) {
            _errorMessage.value = "User not logged in."
            return
        }

        notificationJob?.cancel()

        notificationJob = viewModelScope.launch {

            _errorMessage.value = null

            repository
                .observeNotifications(userId)
                .catch { exception ->
                    _errorMessage.value =
                        exception.message ?: "Failed to load notifications."
                }
                .collect { notificationList ->

                    _errorMessage.value = null
                    _notifications.value = notificationList
                }
        }
    }

    fun refresh() {
        startObserving()
    }

    fun markAsRead(notificationId: String) {

        viewModelScope.launch {

            repository
                .markAsRead(notificationId)
                .onFailure {

                    _errorMessage.value =
                        it.message ?: "Failed to update notification."
                }
        }
    }

    override fun onCleared() {
        notificationJob?.cancel()
        super.onCleared()
    }
}