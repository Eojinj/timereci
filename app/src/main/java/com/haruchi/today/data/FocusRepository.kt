package com.haruchi.today.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    private val plannedFocusDao: PlannedFocusDao,
    private val photoStorage: PhotoStorage,
) {
    // ---- Receipts ----

    fun observeReceipts(): Flow<List<ReceiptEntity>> = receiptDao.observeAll()

    fun observeReceipt(id: Long): Flow<ReceiptEntity?> = receiptDao.observeById(id)

    suspend fun getReceipt(id: Long): ReceiptEntity? = receiptDao.getById(id)

    suspend fun receiptExistsForSession(startedAtEpoch: Long): Boolean =
        startedAtEpoch != 0L && receiptDao.countForSession(startedAtEpoch) > 0

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

    // ---- Planned focus (오늘 할 집중) ----

    fun observePlannedFocus(): Flow<List<PlannedFocusEntity>> = plannedFocusDao.observeAll()

    suspend fun addPlannedFocus(label: String, plannedMs: Long, isRepeating: Boolean = false) {
        plannedFocusDao.insert(
            PlannedFocusEntity(
                label = label,
                plannedMs = plannedMs,
                createdAtEpoch = System.currentTimeMillis(),
                isRepeating = isRepeating,
            ),
        )
    }

    suspend fun deletePlannedFocus(id: Long) = plannedFocusDao.deleteById(id)

    suspend fun updatePlannedFocus(item: PlannedFocusEntity) = plannedFocusDao.update(item)

    /**
     * Head of the todo queue, if any — used to offer "continue with the next task".
     *
     * [excludingLabel] skips the task that was just finished. Without it a repeating task,
     * which by design stays on the list after being started, would immediately offer itself
     * back and every session would end in "up next: the thing you just did".
     */
    suspend fun nextPlannedFocus(excludingLabel: String? = null): PlannedFocusEntity? {
        val excluded = excludingLabel?.takeIf { it.isNotBlank() }?.let(TaskKey::of)
        return plannedFocusDao.getQueue()
            .firstOrNull { excluded == null || TaskKey.of(it.label) != excluded }
    }

    // ---- Photos ----

    val photoStorageRef: PhotoStorage get() = photoStorage

    /** Most recently used photos across past receipts, for "pick from recent" pickers. */
    fun observeRecentPhotos(limit: Int = 18): Flow<List<PhotoRef>> = receiptDao.observeAll().map { receipts ->
        receipts.flatMap { it.photos }
            .filter { it.fileName != null }
            .distinctBy { it.fileName }
            .take(limit)
    }

    /** Duplicates a recent photo's file so it becomes an independent copy for a new session. */
    suspend fun reusePhoto(ref: PhotoRef, toneIndex: Int): PhotoRef? =
        ref.fileName?.let { photoStorage.copy(it, ref.aspect, toneIndex) }
}
