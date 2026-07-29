package week11.st560151.finalproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import week11.st560151.finalproject.data.model.Expense
import week11.st560151.finalproject.data.model.User
import week11.st560151.finalproject.data.repository.ExpenseRepository
import week11.st560151.finalproject.data.repository.SettlementRepository
import week11.st560151.finalproject.data.repository.UserRepository
import week11.st560151.finalproject.ui.state.UiState
import week11.st560151.finalproject.util.BalanceCalculator

class ExpenseViewModel(
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val settlementRepository: SettlementRepository = SettlementRepository(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _expensesState =
        MutableStateFlow<UiState<List<Expense>>>(UiState.Loading)
    val expensesState: StateFlow<UiState<List<Expense>>> =
        _expensesState.asStateFlow()

    private val _membersState =
        MutableStateFlow<UiState<List<User>>>(UiState.Loading)
    val membersState: StateFlow<UiState<List<User>>> =
        _membersState.asStateFlow()

    private val _saveState =
        MutableStateFlow<UiState<String>>(UiState.Idle)
    val saveState: StateFlow<UiState<String>> =
        _saveState.asStateFlow()

    private val _deleteState =
        MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val deleteState: StateFlow<UiState<Unit>> =
        _deleteState.asStateFlow()

    private val _balancesState =
        MutableStateFlow<UiState<Map<String, Double>>>(UiState.Loading)
    val balancesState: StateFlow<UiState<Map<String, Double>>> =
        _balancesState.asStateFlow()

    private var expensesJob: Job? = null
    private var balancesJob: Job? = null

    /** Recomputes net balances for [memberIds] whenever the live expense feed updates. */
    fun loadBalances(groupId: String, memberIds: List<String>) {
        balancesJob?.cancel()

        balancesJob = viewModelScope.launch {
            expensesState.collect { state ->
                val expenses = (state as? UiState.Success)?.data ?: return@collect

                _balancesState.value = UiState.Loading

                val settlements = settlementRepository
                    .getSettlementsOnce(groupId)
                    .getOrElse {
                        _balancesState.value = UiState.Error(
                            it.message ?: "Unable to load balances."
                        )
                        return@collect
                    }

                _balancesState.value = UiState.Success(
                    BalanceCalculator.netBalances(memberIds, expenses, settlements)
                )
            }
        }
    }

    fun observeExpenses(groupId: String) {
        expensesJob?.cancel()

        expensesJob = viewModelScope.launch {
            _expensesState.value = UiState.Loading

            try {
                expenseRepository.observeExpenses(groupId).collect {
                    _expensesState.value = UiState.Success(it)
                }
            } catch (exception: Exception) {
                _expensesState.value = UiState.Error(
                    exception.message ?: "Unable to load expenses."
                )
            }
        }
    }

    fun loadMembers(memberIds: List<String>) {
        viewModelScope.launch {
            _membersState.value = UiState.Loading

            _membersState.value = userRepository
                .getUsers(memberIds)
                .fold(
                    onSuccess = { UiState.Success(it) },
                    onFailure = {
                        UiState.Error(it.message ?: "Unable to load group members.")
                    }
                )
        }
    }

    fun createExpense(
        groupId: String,
        description: String,
        amount: Double,
        category: String,
        paidBy: String,
        participantIds: List<String>,
        shares: Map<String, Double>
    ) {
        val createdBy = auth.currentUser?.uid

        if (createdBy == null) {
            _saveState.value = UiState.Error("User is not signed in.")
            return
        }

        viewModelScope.launch {
            _saveState.value = UiState.Loading

            _saveState.value = expenseRepository
                .createExpense(
                    groupId = groupId,
                    description = description,
                    amount = amount,
                    category = category,
                    paidBy = paidBy,
                    participantIds = participantIds,
                    shares = shares,
                    createdBy = createdBy
                )
                .fold(
                    onSuccess = { UiState.Success(it) },
                    onFailure = {
                        UiState.Error(it.message ?: "Unable to add expense.")
                    }
                )
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            _saveState.value = UiState.Loading

            _saveState.value = expenseRepository
                .updateExpense(expense)
                .fold(
                    onSuccess = { UiState.Success(expense.id) },
                    onFailure = {
                        UiState.Error(it.message ?: "Unable to update expense.")
                    }
                )
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            _deleteState.value = UiState.Loading

            _deleteState.value = expenseRepository
                .deleteExpense(expenseId)
                .fold(
                    onSuccess = { UiState.Success(Unit) },
                    onFailure = {
                        UiState.Error(it.message ?: "Unable to delete expense.")
                    }
                )
        }
    }

    fun resetSaveState() {
        _saveState.value = UiState.Idle
    }

    fun resetDeleteState() {
        _deleteState.value = UiState.Idle
    }
}
