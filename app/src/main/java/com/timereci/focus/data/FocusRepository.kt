package com.timereci.focus.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single access point for receipts, the active session, and photo files.
 * ViewModels talk to this, never to DAOs directly.
 */
@Singleton
class FocusRepository @Inject constructor(
    private val receiptDao: ReceiptDao,
    private val activeSessionDao: ActiveSessionDao,
    private val photoStorage: PhotoStorage,
) {
    // ---- Receipts ----

    fun observeReceipts(): Flow<List<ReceiptEntity>> = receiptDao.observeAll()

    fun observeReceipt(id: Long): Flow<ReceiptEntity?> = receiptDao.observeById(id)

    suspend fun getReceipt(id: Long): ReceiptEntity? = receiptDao.getById(id)

    suspend fun publish(receipt: ReceiptEntity): Long = receiptDao.insert(receipt)

    suspend fun updateReceipt(receipt: ReceiptEntity) = receiptDao.update(receipt)

    suspend fun deleteReceipt(receipt: ReceiptEntity) {
        receipt.photos.forEach { it.fileName?.let(photoStorage::delete) }
        receiptDao.delete(receipt)
    }

    // ---- Active session (crash-recoverable) ----

    fun observeActiveSession(): Flow<ActiveSessionEntity?> = activeSessionDao.observe()

    suspend fun getActiveSession(): ActiveSessionEntity? = activeSessionDao.get()

    suspend fun saveActiveSession(session: ActiveSessionEntity) = activeSessionDao.upsert(session)

    suspend fun clearActiveSession() = activeSessionDao.clear()

    // ---- Photos ----

    val photoStorageRef: PhotoStorage get() = photoStorage
}
