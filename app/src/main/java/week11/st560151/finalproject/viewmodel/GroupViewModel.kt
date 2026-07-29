package week11.st560151.finalproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import week11.st560151.finalproject.data.model.Group
import week11.st560151.finalproject.data.repository.GroupRepository
import week11.st560151.finalproject.ui.state.UiState

class GroupViewModel(
    private val repository: GroupRepository =
        GroupRepository(),
    private val auth: FirebaseAuth =
        FirebaseAuth.getInstance()
) : ViewModel() {

    private val _groupsState =
        MutableStateFlow<UiState<List<Group>>>(
            UiState.Loading
        )

    val groupsState: StateFlow<UiState<List<Group>>> =
        _groupsState.asStateFlow()

    private val _createState =
        MutableStateFlow<UiState<String>>(UiState.Idle)

    val createState: StateFlow<UiState<String>> =
        _createState.asStateFlow()

    private var groupsJob: Job? = null

    fun observeGroups() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            _groupsState.value =
                UiState.Error("User is not signed in.")
            return
        }

        groupsJob?.cancel()

        groupsJob = viewModelScope.launch {
            try {
                repository
                    .observeGroups(userId)
                    .collect {
                        _groupsState.value =
                            UiState.Success(it)
                    }
            } catch (exception: Exception) {
                _groupsState.value =
                    UiState.Error(
                        exception.message
                            ?: "Unable to load groups."
                    )
            }
        }
    }

    fun createGroup(
        name: String
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            _createState.value =
                UiState.Error("User is not signed in.")
            return
        }

        if (name.isBlank()) {
            _createState.value =
                UiState.Error("Group name is required.")
            return
        }

        viewModelScope.launch {
            _createState.value = UiState.Loading

            _createState.value = repository
                .createGroup(name, userId)
                .fold(
                    onSuccess = {
                        UiState.Success(it)
                    },
                    onFailure = {
                        UiState.Error(
                            it.message
                                ?: "Unable to create group."
                        )
                    }
                )
        }
    }

    fun resetCreateState() {
        _createState.value = UiState.Idle
    }
}