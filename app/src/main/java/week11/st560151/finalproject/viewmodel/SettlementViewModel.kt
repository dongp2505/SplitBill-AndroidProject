package week11.st560151.finalproject.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import week11.st560151.finalproject.data.repository.SettlementRepository

data class SettlementUiState(
    val payerEmail: String = "",
    val receiverEmail: String = "",
    val amount: String = "",

    val payerName: String = "",
    val receiverName: String = "",

    val createdByName: String = "",

    val isSaving: Boolean = false,
    val isCompleted: Boolean = false,
    val errorMessage: String? = null
)

class SettlementViewModel(
    private val repository: SettlementRepository =
        SettlementRepository(),
    private val auth: FirebaseAuth =
        FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(SettlementUiState())

    val uiState: StateFlow<SettlementUiState> =
        _uiState.asStateFlow()

    init {
        loadCreatorName()
    }

    private fun loadCreatorName() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(
                createdByName = "Unknown user",
                errorMessage = "User is not signed in."
            )
            return
        }

        viewModelScope.launch {
            try {
                val snapshot = firestore
                    .collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                val firestoreName =
                    snapshot.getString("displayName")

                val resolvedName = when {
                    !firestoreName.isNullOrBlank() -> {
                        firestoreName
                    }

                    !currentUser.displayName.isNullOrBlank() -> {
                        currentUser.displayName!!
                    }

                    !currentUser.email.isNullOrBlank() -> {
                        currentUser.email!!
                            .substringBefore("@")
                    }

                    else -> {
                        "User"
                    }
                }

                _uiState.value = _uiState.value.copy(
                    createdByName = resolvedName,
                    errorMessage = null
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    createdByName =
                        currentUser.displayName
                            ?: currentUser.email
                                ?.substringBefore("@")
                            ?: "User"
                )
            }
        }
    }

    fun updatePayerEmail(
        value: String
    ) {
        _uiState.value = _uiState.value.copy(
            payerEmail = value,
            errorMessage = null
        )
    }

    fun updateReceiverEmail(
        value: String
    ) {
        _uiState.value = _uiState.value.copy(
            receiverEmail = value,
            errorMessage = null
        )
    }

    fun updateAmount(
        value: String
    ) {
        val filteredValue = value.filter { character ->
            character.isDigit() || character == '.'
        }

        if (filteredValue.count { it == '.' } > 1) {
            return
        }

        val decimalPart = filteredValue.substringAfter(
            delimiter = ".",
            missingDelimiterValue = ""
        )

        if (
            filteredValue.contains(".") &&
            decimalPart.length > 2
        ) {
            return
        }

        _uiState.value = _uiState.value.copy(
            amount = filteredValue,
            errorMessage = null
        )
    }

    fun validate(): Boolean {
        val state = _uiState.value
        val amountNumber =
            state.amount.toDoubleOrNull()

        return when {
            auth.currentUser == null -> {
                showError("User is not signed in.")
                false
            }

            state.payerEmail.isBlank() -> {
                showError("Payer email is required.")
                false
            }

            !Patterns.EMAIL_ADDRESS
                .matcher(state.payerEmail.trim())
                .matches() -> {
                showError("Enter a valid payer email.")
                false
            }

            state.receiverEmail.isBlank() -> {
                showError("Receiver email is required.")
                false
            }

            !Patterns.EMAIL_ADDRESS
                .matcher(state.receiverEmail.trim())
                .matches() -> {
                showError("Enter a valid receiver email.")
                false
            }

            state.payerEmail.trim().equals(
                state.receiverEmail.trim(),
                ignoreCase = true
            ) -> {
                showError(
                    "Payer and receiver cannot be the same user."
                )
                false
            }

            amountNumber == null ||
                    amountNumber <= 0.0 -> {
                showError("Enter a valid amount.")
                false
            }

            else -> true
        }
    }

    fun completeSettlement() {
        if (!validate()) {
            return
        }

        val currentUser = auth.currentUser

        if (currentUser == null) {
            showError("User is not signed in.")
            return
        }

        val currentState = _uiState.value
        val amountNumber =
            currentState.amount.toDoubleOrNull()

        if (amountNumber == null) {
            showError("Enter a valid amount.")
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(
                isSaving = true,
                isCompleted = false,
                errorMessage = null
            )

            val payerResult =
                repository.findUserByEmail(
                    currentState.payerEmail
                )

            val payer =
                payerResult.getOrElse { exception ->
                    showError(
                        exception.message
                            ?: "Payer was not found."
                    )
                    return@launch
                }

            val receiverResult =
                repository.findUserByEmail(
                    currentState.receiverEmail
                )

            val receiver =
                receiverResult.getOrElse { exception ->
                    showError(
                        exception.message
                            ?: "Receiver was not found."
                    )
                    return@launch
                }

            if (payer.uid == receiver.uid) {
                showError(
                    "Payer and receiver cannot be the same user."
                )
                return@launch
            }

            repository.saveSettlement(
                groupId = "",
                payer = payer,
                receiver = receiver,
                amount = amountNumber,
                createdById = currentUser.uid,
                createdByName =
                    currentState.createdByName
            ).onSuccess {
                _uiState.value =
                    _uiState.value.copy(
                        payerName =
                            payer.displayName.ifBlank {
                                payer.email
                                    .substringBefore("@")
                            },

                        receiverName =
                            receiver.displayName.ifBlank {
                                receiver.email
                                    .substringBefore("@")
                            },

                        isSaving = false,
                        isCompleted = true,
                        errorMessage = null
                    )
            }.onFailure { exception ->
                showError(
                    exception.message
                        ?: "Unable to save settlement."
                )
            }
        }
    }

    fun showError(
        message: String
    ) {
        _uiState.value = _uiState.value.copy(
            isSaving = false,
            isCompleted = false,
            errorMessage = message
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }

    fun clearForm() {
        _uiState.value = _uiState.value.copy(
            payerEmail = "",
            receiverEmail = "",
            amount = "",
            payerName = "",
            receiverName = "",
            isSaving = false,
            isCompleted = false,
            errorMessage = null
        )
    }
}