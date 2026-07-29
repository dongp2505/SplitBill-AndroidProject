package week11.st560151.finalproject.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import week11.st560151.finalproject.model.Settlement

class SettlementRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun saveSettlement(
        groupId: String,
        payerId: String,
        payerName: String,
        receiverId: String,
        receiverName: String,
        amount: Double
    ): Result<String> {
        return try {
            val settlementReference = firestore
                .collection("settlements")
                .document()

            val settlement = Settlement(
                id = settlementReference.id,
                groupId = groupId,
                payerId = payerId,
                payerName = payerName,
                receiverId = receiverId,
                receiverName = receiverName,
                amount = amount,
                status = "COMPLETED",
                confirmedWithBiometric = true,
                createdAt = Timestamp.now()
            )

            settlementReference
                .set(settlement)
                .await()

            Result.success(settlementReference.id)
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }
}