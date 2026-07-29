package week11.st560151.finalproject.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import week11.st560151.finalproject.data.model.User

class AuthRepository(
    private val auth: FirebaseAuth =
        FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) {

    suspend fun register(
        displayName: String,
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth
                .createUserWithEmailAndPassword(
                    email.trim(),
                    password
                )
                .await()

            val firebaseUser = result.user
                ?: throw IllegalStateException(
                    "Firebase did not return a user."
                )

            val profileUpdates = userProfileChangeRequest {
                this.displayName = displayName.trim()
            }

            firebaseUser
                .updateProfile(profileUpdates)
                .await()

            val user = User(
                uid = firebaseUser.uid,
                displayName = displayName.trim(),
                email = email.trim().lowercase(),
                createdAt = System.currentTimeMillis()
            )

            firestore
                .collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()

            Result.success(firebaseUser)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth
                .signInWithEmailAndPassword(
                    email.trim(),
                    password
                )
                .await()

            val user = result.user
                ?: throw IllegalStateException(
                    "Firebase did not return a user."
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
            auth
                .sendPasswordResetEmail(email.trim())
                .await()

            Result.success(Unit)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun logout() {
        auth.signOut()
    }
}