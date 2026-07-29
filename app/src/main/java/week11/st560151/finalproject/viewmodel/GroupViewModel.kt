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
import week11.st560151.finalproject.data.model.User
import week11.st560151.finalproject.data.repository.ExpenseRepository
import week11.st560151.finalproject.data.repository.GroupRepository
import week11.st560151.finalproject.data.repository.SettlementRepository
import week11.st560151.finalproject.data.repository.UserRepository
import week11.st560151.finalproject.ui.state.UiState
import week11.st560151.finalproject.util.BalanceCalculator

class GroupViewModel(
    private val repository: GroupRepository =
        GroupRepository(),
    private val expenseRepository: ExpenseRepository =
        ExpenseRepository(),
    private val settlementRepository: SettlementRepository =
        SettlementRepository(),
    private val userRepository: UserRepository =
        UserRepository(),
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

    private val _groupState =
        MutableStateFlow<UiState<Group>>(UiState.Loading)

    val groupState: StateFlow<UiState<Group>> =
        _groupState.asStateFlow()

    private val _joinState =
        MutableStateFlow<UiState<Group>>(UiState.Idle)

    val joinState: StateFlow<UiState<Group>> =
        _joinState.asStateFlow()

    // groupId -> current user's net balance, for the landing list's chips.
    private val _balancesState =
        MutableStateFlow<Map<String, Double>>(emptyMap())

    val balancesState: StateFlow<Map<String, Double>> =
        _balancesState.asStateFlow()

    // groupId -> resolved member profiles, for the landing list's avatar chips.
    private val _membersByGroupState =
        MutableStateFlow<Map<String, List<User>>>(emptyMap())

    val membersByGroupState: StateFlow<Map<String, List<User>>> =
        _membersByGroupState.asStateFlow()

    private var groupsJob: Job? = null
    private var summariesJob: Job? = null

    fun loadGroup(groupId: String) {
        viewModelScope.launch {
            _groupState.value = UiState.Loading

            _groupState.value = repository
                .getGroup(groupId)
                .fold(
                    onSuccess = { UiState.Success(it) },
                    onFailure = {
                        UiState.Error(
                            it.message ?: "Unable to load group."
                        )
                    }
                )
        }
    }

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
                    .collect { groups ->
                        _groupsState.value =
                            UiState.Success(groups)

                        loadGroupSummaries(groups, userId)
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
        name: String,
        type: String,
        inviteEmails: List<String> = emptyList()
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
                .createGroup(
                    name = name,
                    type = type,
                    ownerId = userId,
                    inviteEmails = inviteEmails
                )
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

    fun joinGroup(inviteCode: String) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            _joinState.value =
                UiState.Error("User is not signed in.")
            return
        }

        if (inviteCode.isBlank()) {
            _joinState.value =
                UiState.Error("Enter an invite code.")
            return
        }

        viewModelScope.launch {
            _joinState.value = UiState.Loading

            _joinState.value = repository
                .joinGroupByInviteCode(inviteCode, userId)
                .fold(
                    onSuccess = { UiState.Success(it) },
                    onFailure = {
                        UiState.Error(
                            it.message ?: "Unable to join group."
                        )
                    }
                )
        }
    }

    private fun loadGroupSummaries(groups: List<Group>, userId: String) {
        summariesJob?.cancel()

        summariesJob = viewModelScope.launch {
            val balances = mutableMapOf<String, Double>()
            val membersByGroup = mutableMapOf<String, List<User>>()

            for (group in groups) {
                val expenses = expenseRepository
                    .getExpensesOnce(group.id)
                    .getOrDefault(emptyList())

                val settlements = settlementRepository
                    .getSettlementsOnce(group.id)
                    .getOrDefault(emptyList())

                balances[group.id] =
                    BalanceCalculator.netBalance(expenses, settlements, userId)

                membersByGroup[group.id] = userRepository
                    .getUsers(group.memberIds)
                    .getOrDefault(emptyList())
            }

            _balancesState.value = balances
            _membersByGroupState.value = membersByGroup
        }
    }

    fun resetCreateState() {
        _createState.value = UiState.Idle
    }

    fun resetJoinState() {
        _joinState.value = UiState.Idle
    }
}