package week11.st560151.finalproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import week11.st560151.finalproject.repository.SettlementRepository

data class SettlementUiState(
    val payerName: String = "",
    val receiverName: String = "",
    val amount: String = "",
    val isSaving: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

class SettlementViewModel(
    private val repository: SettlementRepository = SettlementRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettlementUiState())
    val uiState: StateFlow<SettlementUiState> = _uiState.asStateFlow()

    fun updatePayerName(value: String) {
        _uiState.value = _uiState.value.copy(
            payerName = value,
            errorMessage = null
        )
    }

    fun updateReceiverName(value: String) {
        _uiState.value = _uiState.value.copy(
            receiverName = value,
            errorMessage = null
        )
    }

    fun updateAmount(value: String) {
        val validValue = value.filter { character ->
            character.isDigit() || character == '.'
        }

        _uiState.value = _uiState.value.copy(
            amount = validValue,
            errorMessage = null
        )
    }

    fun validateSettlement(): Boolean {
        val state = _uiState.value
        val amountValue = state.amount.toDoubleOrNull()

        return when {
            state.payerName.isBlank() -> {
                showError("Enter the payer's name.")
                false
            }

            state.receiverName.isBlank() -> {
                showError("Enter the receiver's name.")
                false
            }

            state.payerName.trim().equals(
                state.receiverName.trim(),
                ignoreCase = true
            ) -> {
                showError("Payer and receiver cannot be the same person.")
                false
            }

            amountValue == null || amountValue <= 0.0 -> {
                showError("Enter a valid settlement amount.")
                false
            }

            else -> true
        }
    }

    fun completeSettlement(
        groupId: String,
        payerId: String,
        receiverId: String
    ) {
        val state = _uiState.value
        val amountValue = state.amount.toDoubleOrNull()

        if (amountValue == null || amountValue <= 0.0) {
            showError("Enter a valid settlement amount.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(
                isSaving = true,
                errorMessage = null
            )

            repository.saveSettlement(
                groupId = groupId,
                payerId = payerId,
                payerName = state.payerName.trim(),
                receiverId = receiverId,
                receiverName = state.receiverName.trim(),
                amount = amountValue
            ).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isCompleted = true,
                    errorMessage = null
                )
            }.onFailure { exception ->
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isCompleted = false,
                    errorMessage = exception.message
                        ?: "Unable to save settlement."
                )
            }
        }
    }

    fun showError(message: String) {
        _uiState.value = _uiState.value.copy(
            errorMessage = message,
            isSaving = false
        )
    }

    fun resetCompletion() {
        _uiState.value = _uiState.value.copy(
            isCompleted = false
        )
    }

    fun clearForm() {
        _uiState.value = SettlementUiState()
    }
}