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
import week11.st560151.finalproject.data.model.User
import week11.st560151.finalproject.data.repository.SettlementRepository
import java.util.Locale

data class SettlementUiState(
    val groupId: String = "",
    val payerEmail: String = "",
    val receiverEmail: String = "",
    val amount: String = "",

    val payerUser: User? = null,
    val receiverUser: User? = null,
    val isLoadingParticipants: Boolean = false,

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
        MutableStateFlow(
            SettlementUiState()
        )

    val uiState: StateFlow<SettlementUiState> =
        _uiState.asStateFlow()

    init {
        loadCreatorName()
    }

    /*
     * Load the signed-in user's display name.
     */
    private fun loadCreatorName() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            _uiState.value =
                _uiState.value.copy(
                    createdByName = "Unknown user",
                    errorMessage =
                        "User is not signed in."
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

                    !currentUser.displayName
                        .isNullOrBlank() -> {
                        currentUser.displayName!!
                    }

                    !currentUser.email
                        .isNullOrBlank() -> {
                        currentUser.email!!
                            .substringBefore("@")
                    }

                    else -> {
                        "User"
                    }
                }

                _uiState.value =
                    _uiState.value.copy(
                        createdByName =
                            resolvedName,
                        errorMessage = null
                    )

            } catch (_: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        createdByName =
                            currentUser.displayName
                                ?: currentUser.email
                                    ?.substringBefore("@")
                                ?: "User"
                    )
            }
        }
    }

    /*
     * Called when opening Settle Up from a group.
     */
    fun prefill(
        groupId: String,
        payerEmail: String,
        receiverEmail: String,
        amount: Double
    ) {
        _uiState.value =
            _uiState.value.copy(
                groupId = groupId,
                payerEmail = payerEmail
                    .trim()
                    .lowercase(),
                receiverEmail =
                    receiverEmail
                        .trim()
                        .lowercase(),
                amount =
                    if (amount > 0.0) {
                        String.format(
                            Locale.US,
                            "%.2f",
                            amount
                        )
                    } else {
                        ""
                    },
                payerUser = null,
                receiverUser = null,
                isCompleted = false,
                errorMessage = null
            )

        loadParticipants()
    }

    /*
     * Find the payer and receiver user documents.
     */
    private fun loadParticipants() {
        val state = _uiState.value

        if (
            state.payerEmail.isBlank() ||
            state.receiverEmail.isBlank()
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    isLoadingParticipants = true,
                    errorMessage = null
                )

            val payerResult =
                repository.findUserByEmail(
                    state.payerEmail
                )

            if (payerResult.isFailure) {
                _uiState.value =
                    _uiState.value.copy(
                        payerUser = null,
                        receiverUser = null,
                        isLoadingParticipants = false,
                        errorMessage =
                            payerResult.exceptionOrNull()
                                ?.message
                                ?: "Unable to find payer."
                    )

                return@launch
            }

            val receiverResult =
                repository.findUserByEmail(
                    state.receiverEmail
                )

            if (receiverResult.isFailure) {
                _uiState.value =
                    _uiState.value.copy(
                        payerUser = null,
                        receiverUser = null,
                        isLoadingParticipants = false,
                        errorMessage =
                            receiverResult.exceptionOrNull()
                                ?.message
                                ?: "Unable to find receiver."
                    )

                return@launch
            }

            _uiState.value =
                _uiState.value.copy(
                    payerUser =
                        payerResult.getOrNull(),
                    receiverUser =
                        receiverResult.getOrNull(),
                    isLoadingParticipants = false,
                    errorMessage = null
                )
        }
    }

    fun updatePayerEmail(
        value: String
    ) {
        _uiState.value =
            _uiState.value.copy(
                payerEmail = value,
                payerUser = null,
                errorMessage = null
            )
    }

    fun updateReceiverEmail(
        value: String
    ) {
        _uiState.value =
            _uiState.value.copy(
                receiverEmail = value,
                receiverUser = null,
                errorMessage = null
            )
    }

    fun updateAmount(
        value: String
    ) {
        val filteredValue =
            value.filter { character ->
                character.isDigit() ||
                        character == '.'
            }

        if (
            filteredValue.count {
                    character ->
                character == '.'
            } > 1
        ) {
            return
        }

        val decimalPart =
            filteredValue.substringAfter(
                delimiter = ".",
                missingDelimiterValue = ""
            )

        if (
            filteredValue.contains(".") &&
            decimalPart.length > 2
        ) {
            return
        }

        _uiState.value =
            _uiState.value.copy(
                amount = filteredValue,
                errorMessage = null
            )
    }

    /*
     * Verify all settlement information before
     * showing biometric authentication.
     */
    fun validate(): Boolean {
        val state = _uiState.value

        val amountNumber =
            state.amount.toDoubleOrNull()

        return when {
            auth.currentUser == null -> {
                showError(
                    "User is not signed in."
                )

                false
            }

            state.groupId.isBlank() -> {
                showError(
                    "Group ID is missing."
                )

                false
            }

            state.payerEmail.isBlank() -> {
                showError(
                    "Payer email is required."
                )

                false
            }

            !Patterns.EMAIL_ADDRESS
                .matcher(
                    state.payerEmail.trim()
                )
                .matches() -> {
                showError(
                    "Enter a valid payer email."
                )

                false
            }

            state.receiverEmail.isBlank() -> {
                showError(
                    "Receiver email is required."
                )

                false
            }

            !Patterns.EMAIL_ADDRESS
                .matcher(
                    state.receiverEmail.trim()
                )
                .matches() -> {
                showError(
                    "Enter a valid receiver email."
                )

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

            state.payerUser == null -> {
                showError(
                    "Payer information is missing."
                )

                false
            }

            state.receiverUser == null -> {
                showError(
                    "Receiver information is missing."
                )

                false
            }

            amountNumber == null ||
                    amountNumber <= 0.0 -> {
                showError(
                    "Enter a valid amount."
                )

                false
            }

            else -> {
                true
            }
        }
    }

    /*
     * Called only after biometric authentication succeeds.
     */
    fun completeSettlement() {
        val currentState =
            _uiState.value

        if (currentState.isSaving) {
            return
        }

        if (!validate()) {
            return
        }

        val currentUser =
            auth.currentUser

        if (currentUser == null) {
            showError(
                "User is not signed in."
            )

            return
        }

        val payer =
            currentState.payerUser

        if (payer == null) {
            showError(
                "Payer information is missing."
            )

            return
        }

        val receiver =
            currentState.receiverUser

        if (receiver == null) {
            showError(
                "Receiver information is missing."
            )

            return
        }

        val amountValue =
            currentState.amount.toDoubleOrNull()

        if (
            amountValue == null ||
            amountValue <= 0.0
        ) {
            showError(
                "Enter a valid settlement amount."
            )

            return
        }

        viewModelScope.launch {
            _uiState.value =
                currentState.copy(
                    isSaving = true,
                    isCompleted = false,
                    errorMessage = null
                )

            repository.saveSettlement(
                groupId =
                    currentState.groupId,
                payer = payer,
                receiver = receiver,
                amount = amountValue,
                createdById =
                    currentUser.uid,
                createdByName =
                    currentState.createdByName
            ).onSuccess {
                _uiState.value =
                    _uiState.value.copy(
                        isSaving = false,
                        isCompleted = true,
                        errorMessage = null
                    )

            }.onFailure { exception ->
                _uiState.value =
                    _uiState.value.copy(
                        isSaving = false,
                        isCompleted = false,
                        errorMessage =
                            exception.message
                                ?: "Unable to complete settlement."
                    )
            }
        }
    }

    fun showError(
        message: String
    ) {
        _uiState.value =
            _uiState.value.copy(
                isSaving = false,
                isCompleted = false,
                errorMessage = message
            )
    }

    fun clearError() {
        _uiState.value =
            _uiState.value.copy(
                errorMessage = null
            )
    }

    fun clearForm() {
        _uiState.value =
            SettlementUiState(
                createdByName =
                    _uiState.value
                        .createdByName
            )
    }
}