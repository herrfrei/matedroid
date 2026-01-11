package com.matedroid.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for SyncProgress and related sync models.
 */
class SyncProgressTest {

    // === SyncProgress Tests ===

    @Test
    fun `percentage is 0 when totalItems is 0`() {
        val progress = SyncProgress(
            carId = 1,
            phase = SyncPhase.SYNCING_SUMMARIES,
            currentItem = 0,
            totalItems = 0
        )
        assertEquals(0f, progress.percentage)
    }

    @Test
    fun `percentage is calculated correctly`() {
        val progress = SyncProgress(
            carId = 1,
            phase = SyncPhase.SYNCING_DRIVE_DETAILS,
            currentItem = 50,
            totalItems = 100
        )
        assertEquals(0.5f, progress.percentage)
    }

    @Test
    fun `percentage is 1 when complete`() {
        val progress = SyncProgress(
            carId = 1,
            phase = SyncPhase.SYNCING_DRIVE_DETAILS,
            currentItem = 100,
            totalItems = 100
        )
        assertEquals(1f, progress.percentage)
    }

    @Test
    fun `percentageInt rounds correctly`() {
        val progress = SyncProgress(
            carId = 1,
            phase = SyncPhase.SYNCING_DRIVE_DETAILS,
            currentItem = 33,
            totalItems = 100
        )
        assertEquals(33, progress.percentageInt)
    }

    @Test
    fun `isComplete returns true for COMPLETE phase`() {
        val progress = SyncProgress(
            carId = 1,
            phase = SyncPhase.COMPLETE,
            currentItem = 100,
            totalItems = 100
        )
        assertTrue(progress.isComplete)
    }

    @Test
    fun `isComplete returns false for non-COMPLETE phases`() {
        val phases = listOf(
            SyncPhase.IDLE,
            SyncPhase.SYNCING_SUMMARIES,
            SyncPhase.SYNCING_DRIVE_DETAILS,
            SyncPhase.SYNCING_CHARGE_DETAILS,
            SyncPhase.ERROR
        )
        for (phase in phases) {
            val progress = SyncProgress(
                carId = 1,
                phase = phase,
                currentItem = 0,
                totalItems = 0
            )
            assertFalse("Phase $phase should not be complete", progress.isComplete)
        }
    }

    @Test
    fun `message is optional`() {
        val progressWithMessage = SyncProgress(
            carId = 1,
            phase = SyncPhase.ERROR,
            currentItem = 0,
            totalItems = 0,
            message = "Error message"
        )
        assertEquals("Error message", progressWithMessage.message)

        val progressWithoutMessage = SyncProgress(
            carId = 1,
            phase = SyncPhase.SYNCING_SUMMARIES,
            currentItem = 0,
            totalItems = 0
        )
        assertNull(progressWithoutMessage.message)
    }

    // === SyncPhase Tests ===

    @Test
    fun `SyncPhase has all expected values`() {
        val phases = SyncPhase.values()
        assertEquals(6, phases.size)
        assertTrue(phases.contains(SyncPhase.IDLE))
        assertTrue(phases.contains(SyncPhase.SYNCING_SUMMARIES))
        assertTrue(phases.contains(SyncPhase.SYNCING_DRIVE_DETAILS))
        assertTrue(phases.contains(SyncPhase.SYNCING_CHARGE_DETAILS))
        assertTrue(phases.contains(SyncPhase.COMPLETE))
        assertTrue(phases.contains(SyncPhase.ERROR))
    }

    // === OverallSyncStatus Tests ===

    @Test
    fun `OverallSyncStatus IDLE has expected default values`() {
        val idle = OverallSyncStatus.IDLE
        assertTrue(idle.carProgresses.isEmpty())
        assertFalse(idle.isAnySyncing)
        assertFalse(idle.allComplete)
    }

    @Test
    fun `OverallSyncStatus isAnySyncing is true when any car is syncing`() {
        val progresses = mapOf(
            1 to SyncProgress(1, SyncPhase.COMPLETE, 100, 100),
            2 to SyncProgress(2, SyncPhase.SYNCING_DRIVE_DETAILS, 50, 100)
        )
        val status = OverallSyncStatus(
            carProgresses = progresses,
            isAnySyncing = true,
            allComplete = false
        )
        assertTrue(status.isAnySyncing)
        assertFalse(status.allComplete)
    }

    @Test
    fun `OverallSyncStatus allComplete is true when all cars complete`() {
        val progresses = mapOf(
            1 to SyncProgress(1, SyncPhase.COMPLETE, 100, 100),
            2 to SyncProgress(2, SyncPhase.COMPLETE, 50, 50)
        )
        val status = OverallSyncStatus(
            carProgresses = progresses,
            isAnySyncing = false,
            allComplete = true
        )
        assertFalse(status.isAnySyncing)
        assertTrue(status.allComplete)
    }

    @Test
    fun `OverallSyncStatus carProgresses contains all car progress`() {
        val progresses = mapOf(
            1 to SyncProgress(1, SyncPhase.COMPLETE, 100, 100),
            2 to SyncProgress(2, SyncPhase.SYNCING_SUMMARIES, 0, 1)
        )
        val status = OverallSyncStatus(
            carProgresses = progresses,
            isAnySyncing = true,
            allComplete = false
        )
        assertEquals(2, status.carProgresses.size)
        assertEquals(SyncPhase.COMPLETE, status.carProgresses[1]?.phase)
        assertEquals(SyncPhase.SYNCING_SUMMARIES, status.carProgresses[2]?.phase)
    }
}
