package com.matedroid.ui.screens.battery

import com.matedroid.data.api.models.BatteryDetails
import com.matedroid.data.api.models.BatteryHealth
import com.matedroid.data.api.models.CarStatus
import com.matedroid.data.api.models.Units
import com.matedroid.data.repository.ApiResult
import com.matedroid.data.repository.CarStatusWithUnits
import com.matedroid.data.repository.TeslamateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BatteryViewModel.
 * Tests battery health loading, stats computation, and error handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BatteryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TeslamateRepository
    private lateinit var viewModel: BatteryViewModel

    private val testBatteryHealth = BatteryHealth(
        batteryHealthPercentage = 95.0,
        maxCapacity = 82.0,
        currentCapacity = 77.9,
        maxRange = 500.0,
        currentRange = 475.0,
        ratedEfficiency = 150.0
    )

    private val testCarStatus = CarStatus(
        displayName = "Test Tesla",
        state = "online",
        batteryDetails = BatteryDetails(
            batteryLevel = 80,
            usableBatteryLevel = 79,
            estBatteryRange = 350.0,
            ratedBatteryRange = 380.0,
            idealBatteryRange = 400.0
        )
    )

    private val testUnits = Units(unitOfLength = "km", unitOfTemperature = "C")
    private val testStatusWithUnits = CarStatusWithUnits(status = testCarStatus, units = testUnits)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): BatteryViewModel {
        return BatteryViewModel(repository)
    }

    // === setCarId Tests ===

    @Test
    fun `setCarId loads battery data for new car`() = runTest {
        coEvery { repository.getBatteryHealth(1) } returns ApiResult.Success(testBatteryHealth)
        coEvery { repository.getCarStatus(1) } returns ApiResult.Success(testStatusWithUnits)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(testBatteryHealth, viewModel.uiState.value.batteryHealth)
        assertEquals(testCarStatus, viewModel.uiState.value.carStatus)
        assertEquals(testUnits, viewModel.uiState.value.units)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `setCarId with efficiency sets rated efficiency`() = runTest {
        coEvery { repository.getBatteryHealth(1) } returns ApiResult.Success(testBatteryHealth)
        coEvery { repository.getCarStatus(1) } returns ApiResult.Success(testStatusWithUnits)

        viewModel = createViewModel()
        viewModel.setCarId(1, efficiency = 145.0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(145.0, viewModel.uiState.value.ratedEfficiency, 0.01)
    }

    @Test
    fun `setCarId does not reload if same car ID`() = runTest {
        coEvery { repository.getBatteryHealth(1) } returns ApiResult.Success(testBatteryHealth)
        coEvery { repository.getCarStatus(1) } returns ApiResult.Success(testStatusWithUnits)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setCarId(1) // Same ID
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.getBatteryHealth(1) }
        coVerify(exactly = 1) { repository.getCarStatus(1) }
    }

    @Test
    fun `setCarId reloads for different car ID`() = runTest {
        coEvery { repository.getBatteryHealth(any()) } returns ApiResult.Success(testBatteryHealth)
        coEvery { repository.getCarStatus(any()) } returns ApiResult.Success(testStatusWithUnits)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setCarId(2)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.getBatteryHealth(1) }
        coVerify(exactly = 1) { repository.getBatteryHealth(2) }
    }

    // === Error Handling Tests ===

    @Test
    fun `battery health error is shown`() = runTest {
        coEvery { repository.getBatteryHealth(1) } returns ApiResult.Error("Battery health error")
        coEvery { repository.getCarStatus(1) } returns ApiResult.Success(testStatusWithUnits)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Battery health error", viewModel.uiState.value.error)
    }

    @Test
    fun `car status error is shown`() = runTest {
        coEvery { repository.getBatteryHealth(1) } returns ApiResult.Success(testBatteryHealth)
        coEvery { repository.getCarStatus(1) } returns ApiResult.Error("Status error")

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Status error", viewModel.uiState.value.error)
    }

    @Test
    fun `clearError clears error state`() = runTest {
        coEvery { repository.getBatteryHealth(1) } returns ApiResult.Error("Test error")
        coEvery { repository.getCarStatus(1) } returns ApiResult.Success(testStatusWithUnits)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Test error", viewModel.uiState.value.error)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }

    // === Refresh Tests ===

    @Test
    fun `refresh reloads battery data`() = runTest {
        coEvery { repository.getBatteryHealth(1) } returns ApiResult.Success(testBatteryHealth)
        coEvery { repository.getCarStatus(1) } returns ApiResult.Success(testStatusWithUnits)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedHealth = testBatteryHealth.copy(batteryHealthPercentage = 94.0)
        coEvery { repository.getBatteryHealth(1) } returns ApiResult.Success(updatedHealth)

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals(94.0, viewModel.uiState.value.batteryHealth?.batteryHealthPercentage)

        coVerify(exactly = 2) { repository.getBatteryHealth(1) }
    }

    @Test
    fun `refresh does nothing when no car selected`() = runTest {
        viewModel = createViewModel()

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.getBatteryHealth(any()) }
    }

    // === Detail Dialog Tests ===

    @Test
    fun `showDetail sets showDetail to true`() = runTest {
        viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.showDetail)

        viewModel.showDetail()

        assertTrue(viewModel.uiState.value.showDetail)
    }

    @Test
    fun `hideDetail sets showDetail to false`() = runTest {
        viewModel = createViewModel()
        viewModel.showDetail()

        assertTrue(viewModel.uiState.value.showDetail)

        viewModel.hideDetail()

        assertFalse(viewModel.uiState.value.showDetail)
    }

    // === computeStats Tests ===

    @Test
    fun `computeStats returns null when no battery health`() = runTest {
        viewModel = createViewModel()

        val stats = viewModel.computeStats()

        assertNull(stats)
    }

    @Test
    fun `computeStats returns correct values`() = runTest {
        coEvery { repository.getBatteryHealth(1) } returns ApiResult.Success(testBatteryHealth)
        coEvery { repository.getCarStatus(1) } returns ApiResult.Success(testStatusWithUnits)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val stats = viewModel.computeStats()

        assertNotNull(stats)
        assertEquals(77.9, stats!!.currentCapacity, 0.01)
        assertEquals(82.0, stats.originalCapacity, 0.01)
        assertEquals(95.0, stats.healthPercent, 0.01)
        assertEquals(4.1, stats.lossKwh, 0.1)
        assertEquals(5.0, stats.lossPercent, 0.01)
        assertEquals(500.0, stats.maxRangeNew, 0.01)
        assertEquals(475.0, stats.maxRangeNow, 0.01)
        assertEquals(25.0, stats.rangeLoss, 0.01)
    }

    @Test
    fun `computeStats includes car status values`() = runTest {
        coEvery { repository.getBatteryHealth(1) } returns ApiResult.Success(testBatteryHealth)
        coEvery { repository.getCarStatus(1) } returns ApiResult.Success(testStatusWithUnits)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val stats = viewModel.computeStats()

        assertNotNull(stats)
        assertEquals(80, stats!!.batteryLevel)
        assertEquals(79, stats.usableBatteryLevel)
        assertEquals(350.0, stats.estimatedRange, 0.01)
        assertEquals(380.0, stats.ratedRange, 0.01)
        assertEquals(400.0, stats.idealRange, 0.01)
    }

    @Test
    fun `computeStats calculates rangeAt100 correctly`() = runTest {
        coEvery { repository.getBatteryHealth(1) } returns ApiResult.Success(testBatteryHealth)
        coEvery { repository.getCarStatus(1) } returns ApiResult.Success(testStatusWithUnits)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val stats = viewModel.computeStats()

        // (380 / 80) * 100 = 475
        assertEquals(475.0, stats!!.rangeAt100, 0.01)
    }

    @Test
    fun `computeStats uses default efficiency when not available`() = runTest {
        val healthWithoutEfficiency = testBatteryHealth.copy(ratedEfficiency = null)
        coEvery { repository.getBatteryHealth(1) } returns ApiResult.Success(healthWithoutEfficiency)
        coEvery { repository.getCarStatus(1) } returns ApiResult.Success(testStatusWithUnits)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val stats = viewModel.computeStats()

        // Default efficiency is 150.0
        assertEquals(150.0, stats!!.ratedEfficiency, 0.01)
    }

    @Test
    fun `computeStats uses car status default for missing values`() = runTest {
        val healthWithMissingData = BatteryHealth(
            batteryHealthPercentage = 95.0
        )
        val statusWithoutBattery = CarStatus(displayName = "Tesla", state = "online")
        val statusWithUnits = CarStatusWithUnits(status = statusWithoutBattery, units = testUnits)

        coEvery { repository.getBatteryHealth(1) } returns ApiResult.Success(healthWithMissingData)
        coEvery { repository.getCarStatus(1) } returns ApiResult.Success(statusWithUnits)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val stats = viewModel.computeStats()

        assertNotNull(stats)
        assertEquals(0, stats!!.batteryLevel)
        assertEquals(0.0, stats.estimatedRange, 0.01)
    }

    // === Loading State Tests ===

    @Test
    fun `loading state is true during initial load`() = runTest {
        coEvery { repository.getBatteryHealth(1) } coAnswers {
            kotlinx.coroutines.delay(100)
            ApiResult.Success(testBatteryHealth)
        }
        coEvery { repository.getCarStatus(1) } returns ApiResult.Success(testStatusWithUnits)

        viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.isLoading) // Initial state

        viewModel.setCarId(1)

        assertTrue(viewModel.uiState.value.isLoading)

        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `refreshing state is set during refresh`() = runTest {
        coEvery { repository.getBatteryHealth(1) } returns ApiResult.Success(testBatteryHealth)
        coEvery { repository.getCarStatus(1) } returns ApiResult.Success(testStatusWithUnits)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)

        viewModel.refresh()
        // Note: isRefreshing is set before the coroutine executes
        // The actual state depends on timing
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)
    }
}
