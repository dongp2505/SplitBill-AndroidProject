package week11.st560151.finalproject.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthenticator(
    private val activity: FragmentActivity
) {

    sealed class Result {

        data object Success : Result()

        data object Failed : Result()

        data object NotEnrolled : Result()

        data object NotAvailable : Result()

        data class Error(
            val message: String
        ) : Result()
    }

    fun authenticate(
        onResult: (Result) -> Unit
    ) {
        val biometricManager =
            BiometricManager.from(activity)

        /*
         * Allow strong biometric authentication,
         * or the device PIN/pattern/password.
         */
        val allowedAuthenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL

        when (
            biometricManager.canAuthenticate(
                allowedAuthenticators
            )
        ) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showPrompt(
                    allowedAuthenticators =
                        allowedAuthenticators,
                    onResult = onResult
                )
            }

            BiometricManager
                .BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onResult(Result.NotEnrolled)
            }

            BiometricManager
                .BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager
                .BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                onResult(Result.NotAvailable)
            }

            else -> {
                onResult(
                    Result.Error(
                        "Biometric authentication cannot be used."
                    )
                )
            }
        }
    }

    private fun showPrompt(
        allowedAuthenticators: Int,
        onResult: (Result) -> Unit
    ) {
        val executor =
            ContextCompat.getMainExecutor(activity)

        val biometricPrompt =
            BiometricPrompt(
                activity,
                executor,
                object :
                    BiometricPrompt.AuthenticationCallback() {

                    override fun onAuthenticationSucceeded(
                        result:
                        BiometricPrompt.AuthenticationResult
                    ) {
                        super.onAuthenticationSucceeded(
                            result
                        )

                        onResult(Result.Success)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()

                        onResult(Result.Failed)
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence
                    ) {
                        super.onAuthenticationError(
                            errorCode,
                            errString
                        )

                        /*
                         * These errors happen when the user
                         * presses Cancel or the system Back button.
                         * They are not Firestore permission errors.
                         */
                        when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_CANCELED -> {
                                onResult(
                                    Result.Error(
                                        "Authentication cancelled."
                                    )
                                )
                            }

                            else -> {
                                onResult(
                                    Result.Error(
                                        errString.toString()
                                    )
                                )
                            }
                        }
                    }
                }
            )

        val promptInfo =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm Settlement")
                .setSubtitle(
                    "Verify your identity"
                )
                .setDescription(
                    "Use your fingerprint, face, PIN, pattern, or password."
                )
                .setAllowedAuthenticators(
                    allowedAuthenticators
                )
                .build()

        biometricPrompt.authenticate(promptInfo)
    }
}