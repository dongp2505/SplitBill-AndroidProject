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
    private val repository: NotificationRepository =
        NotificationRepository(),

    private val auth: FirebaseAuth =
        FirebaseAuth.getInstance()
) : ViewModel() {

    private val _notifications =
        MutableStateFlow<List<AppNotification>>(
            emptyList()
        )

    val notifications: StateFlow<List<AppNotification>> =
        _notifications.asStateFlow()

    private val _errorMessage =
        MutableStateFlow<String?>(null)

    val errorMessage: StateFlow<String?> =
        _errorMessage.asStateFlow()

    private val _isLoading =
        MutableStateFlow(true)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private var notificationJob: Job? = null

    init {
        startObserving()
    }

    private fun startObserving() {

        val userId = auth.currentUser?.uid

        if (userId.isNullOrBlank()) {
            _notifications.value = emptyList()
            _isLoading.value = false
            _errorMessage.value =
                "You must sign in to view notifications."
            return
        }

        notificationJob?.cancel()

        notificationJob = viewModelScope.launch {

            _isLoading.value = true
            _errorMessage.value = null

            repository
                .observeNotifications(userId)
                .catch { exception ->

                    _isLoading.value = false
                    _notifications.value = emptyList()

                    _errorMessage.value =
                        exception.message
                            ?: "Unable to load notifications."
                }
                .collect { notificationList ->

                    _notifications.value = notificationList
                    _isLoading.value = false
                    _errorMessage.value = null
                }
        }
    }

    fun refresh() {
        startObserving()
    }

    fun markAsRead(
        notificationId: String
    ) {
        viewModelScope.launch {

            repository
                .markAsRead(notificationId)
                .onFailure { exception ->

                    _errorMessage.value =
                        exception.message
                            ?: "Unable to update notification."
                }
        }
    }

    fun deleteNotification(
        notificationId: String
    ) {
        viewModelScope.launch {

            repository
                .deleteNotification(notificationId)
                .onFailure { exception ->

                    _errorMessage.value =
                        exception.message
                            ?: "Unable to delete notification."
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