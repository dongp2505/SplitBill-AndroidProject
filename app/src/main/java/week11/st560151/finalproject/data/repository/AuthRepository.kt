package week11.st560151.finalproject.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    suspend fun register(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth
                .createUserWithEmailAndPassword(
                    email.trim(),
                    password
                )
                .await()

            val user = authResult.user
                ?: return Result.failure(
                    IllegalStateException(
                        "Firebase did not return a user."
                    )
                )

            Result.success(user)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth
                .signInWithEmailAndPassword(
                    email.trim(),
                    password
                )
                .await()

            val user = authResult.user
                ?: return Result.failure(
                    IllegalStateException(
                        "Firebase did not return a user."
                    )
                )

            Result.success(user)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun sendPasswordReset(
        email: String
    ): Result<Unit> {
        return try {
            firebaseAuth
                .sendPasswordResetEmail(email.trim())
                .await()

            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
}