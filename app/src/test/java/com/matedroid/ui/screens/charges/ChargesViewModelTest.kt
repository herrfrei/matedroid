package com.matedroid.ui.screens.charges

import com.matedroid.data.api.models.ChargeData
import com.matedroid.data.local.AppSettings
import com.matedroid.data.local.SettingsDataStore
import com.matedroid.data.local.dao.AggregateDao
import com.matedroid.data.repository.ApiResult
import com.matedroid.data.repository.TeslamateRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for ChargesViewModel.
 * Tests date filtering, charge type filtering, summary calculation, and chart data.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChargesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TeslamateRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var aggregateDao: AggregateDao
    private lateinit var viewModel: ChargesViewModel

    private val testSettings = AppSettings(
        serverUrl = "https://teslamate.example.com",
        currencyCode = "EUR"
    )

    private val testCharges = listOf(
        ChargeData(
            chargeId = 1,
            startDate = "2024-01-15T10:00:00",
            chargeEnergyAdded = 30.0,
            cost = 10.0
        ),
        ChargeData(
            chargeId = 2,
            startDate = "2024-01-20T14:00:00",
            chargeEnergyAdded = 45.0,
            cost = 15.0
        ),
        ChargeData(
            chargeId = 3,
            startDate = "2024-01-25T18:00:00",
            chargeEnergyAdded = 20.0,
            cost = 7.0
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        settingsDataStore = mockk()
        aggregateDao = mockk()

        every { settingsDataStore.settings } returns flowOf(testSettings)
        every { settingsDataStore.showShortDrivesCharges } returns flowOf(false)
        coEvery { aggregateDao.getDcChargeIds(any()) } returns emptyList()
        coEvery { aggregateDao.getAllProcessedChargeIds(any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ChargesViewModel {
        return ChargesViewModel(repository, settingsDataStore, aggregateDao)
    }

    // === Basic Loading Tests ===

    @Test
    fun `setCarId loads charges with default date filter`() = runTest {
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(testCharges)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(3, viewModel.uiState.value.charges.size)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `setCarId handles API error`() = runTest {
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Error("Network error")

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Network error", viewModel.uiState.value.error)
    }

    @Test
    fun `setCarId loads currency from settings`() = runTest {
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(testCharges)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        // EUR should be loaded from settings
        // The symbol comes from Currency.findByCode
    }

    // === Date Filter Tests ===

    @Test
    fun `setDateFilter updates date range`() = runTest {
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(testCharges)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setDateFilter(DateFilter.LAST_30_DAYS)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DateFilter.LAST_30_DAYS, viewModel.uiState.value.selectedFilter)
        assertNotNull(viewModel.uiState.value.startDate)
        assertNotNull(viewModel.uiState.value.endDate)
    }

    @Test
    fun `clearDateFilter removes date range`() = runTest {
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(testCharges)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setDateFilter(DateFilter.LAST_30_DAYS)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearDateFilter()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DateFilter.ALL_TIME, viewModel.uiState.value.selectedFilter)
        assertNull(viewModel.uiState.value.startDate)
        assertNull(viewModel.uiState.value.endDate)
    }

    @Test
    fun `setDateFilter ALL_TIME has null dates`() = runTest {
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(testCharges)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setDateFilter(DateFilter.ALL_TIME)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DateFilter.ALL_TIME, viewModel.uiState.value.selectedFilter)
        assertNull(viewModel.uiState.value.startDate)
    }

    // === Charge Type Filter Tests ===

    @Test
    fun `setChargeTypeFilter filters DC charges`() = runTest {
        val dcChargeIds = listOf(2) // Charge ID 2 is DC
        coEvery { aggregateDao.getDcChargeIds(1) } returns dcChargeIds
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(testCharges)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setChargeTypeFilter(ChargeTypeFilter.DC)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ChargeTypeFilter.DC, viewModel.uiState.value.chargeTypeFilter)
        assertEquals(1, viewModel.uiState.value.charges.size)
        assertEquals(2, viewModel.uiState.value.charges[0].chargeId)
    }

    @Test
    fun `setChargeTypeFilter filters AC charges`() = runTest {
        val dcChargeIds = listOf(2) // Only charge ID 2 is DC
        coEvery { aggregateDao.getDcChargeIds(1) } returns dcChargeIds
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(testCharges)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setChargeTypeFilter(ChargeTypeFilter.AC)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ChargeTypeFilter.AC, viewModel.uiState.value.chargeTypeFilter)
        assertEquals(2, viewModel.uiState.value.charges.size)
        assertTrue(viewModel.uiState.value.charges.all { it.chargeId != 2 })
    }

    @Test
    fun `setChargeTypeFilter toggles back to ALL when same filter selected`() = runTest {
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(testCharges)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setChargeTypeFilter(ChargeTypeFilter.DC)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ChargeTypeFilter.DC, viewModel.uiState.value.chargeTypeFilter)

        viewModel.setChargeTypeFilter(ChargeTypeFilter.DC)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ChargeTypeFilter.ALL, viewModel.uiState.value.chargeTypeFilter)
    }

    // === Summary Calculation Tests ===

    @Test
    fun `summary calculates correct totals`() = runTest {
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(testCharges)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val summary = viewModel.uiState.value.summary
        assertEquals(3, summary.totalCharges)
        assertEquals(95.0, summary.totalEnergyAdded, 0.01) // 30 + 45 + 20
        assertEquals(32.0, summary.totalCost, 0.01) // 10 + 15 + 7
    }

    @Test
    fun `summary calculates correct averages`() = runTest {
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(testCharges)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val summary = viewModel.uiState.value.summary
        assertEquals(31.67, summary.avgEnergyPerCharge, 0.01) // 95 / 3
        assertEquals(10.67, summary.avgCostPerCharge, 0.01) // 32 / 3
    }

    @Test
    fun `summary is empty for no charges`() = runTest {
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(emptyList())

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        val summary = viewModel.uiState.value.summary
        assertEquals(0, summary.totalCharges)
        assertEquals(0.0, summary.totalEnergyAdded, 0.01)
        assertEquals(0.0, summary.avgEnergyPerCharge, 0.01)
    }

    // === Short Charges Filter Tests ===

    @Test
    fun `short charges are filtered when setting is disabled`() = runTest {
        val chargesWithShort = listOf(
            ChargeData(chargeId = 1, startDate = "2024-01-15T10:00:00", chargeEnergyAdded = 0.05), // Short
            ChargeData(chargeId = 2, startDate = "2024-01-20T14:00:00", chargeEnergyAdded = 30.0)
        )
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(chargesWithShort)
        every { settingsDataStore.showShortDrivesCharges } returns flowOf(false)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Short charge (< 0.1 kWh) should be filtered out
        assertEquals(1, viewModel.uiState.value.charges.size)
        assertEquals(2, viewModel.uiState.value.charges[0].chargeId)
    }

    @Test
    fun `short charges are shown when setting is enabled`() = runTest {
        val chargesWithShort = listOf(
            ChargeData(chargeId = 1, startDate = "2024-01-15T10:00:00", chargeEnergyAdded = 0.05), // Short
            ChargeData(chargeId = 2, startDate = "2024-01-20T14:00:00", chargeEnergyAdded = 30.0)
        )
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(chargesWithShort)
        every { settingsDataStore.showShortDrivesCharges } returns flowOf(true)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        // Both charges should be shown
        assertEquals(2, viewModel.uiState.value.charges.size)
    }

    // === Refresh Tests ===

    @Test
    fun `refresh reloads charges`() = runTest {
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(testCharges)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)
        coVerify(exactly = 2) { repository.getCharges(1, any(), any()) }
    }

    @Test
    fun `refresh does nothing when no car selected`() = runTest {
        viewModel = createViewModel()

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.getCharges(any(), any(), any()) }
    }

    // === Error Handling Tests ===

    @Test
    fun `clearError clears error state`() = runTest {
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Error("Test error")

        viewModel = createViewModel()
        viewModel.setCarId(1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Test error", viewModel.uiState.value.error)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }

    // === Scroll Position Tests ===

    @Test
    fun `saveScrollPosition saves position`() = runTest {
        viewModel = createViewModel()

        viewModel.saveScrollPosition(10, 50)

        assertEquals(10, viewModel.uiState.value.scrollPosition)
        assertEquals(50, viewModel.uiState.value.scrollOffset)
    }

    // === Chart Granularity Tests ===

    @Test
    fun `chart granularity is daily for 7 day filter`() = runTest {
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(testCharges)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(DateFilter.LAST_7_DAYS)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ChartGranularity.DAILY, viewModel.uiState.value.chartGranularity)
    }

    @Test
    fun `chart granularity is weekly for 30 day filter`() = runTest {
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(testCharges)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(DateFilter.LAST_30_DAYS)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ChartGranularity.DAILY, viewModel.uiState.value.chartGranularity)
    }

    @Test
    fun `chart granularity is monthly for all time filter`() = runTest {
        coEvery { repository.getCharges(1, any(), any()) } returns ApiResult.Success(testCharges)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(DateFilter.ALL_TIME)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ChartGranularity.MONTHLY, viewModel.uiState.value.chartGranularity)
    }

    // === DateFilter Enum Tests ===

    @Test
    fun `DateFilter has correct day values`() {
        assertEquals(7L, DateFilter.LAST_7_DAYS.days)
        assertEquals(30L, DateFilter.LAST_30_DAYS.days)
        assertEquals(90L, DateFilter.LAST_90_DAYS.days)
        assertEquals(365L, DateFilter.LAST_YEAR.days)
        assertNull(DateFilter.ALL_TIME.days)
    }

    @Test
    fun `DateFilter has correct labels`() {
        assertEquals("Last 7 days", DateFilter.LAST_7_DAYS.label)
        assertEquals("Last 30 days", DateFilter.LAST_30_DAYS.label)
        assertEquals("Last 90 days", DateFilter.LAST_90_DAYS.label)
        assertEquals("Last year", DateFilter.LAST_YEAR.label)
        assertEquals("All time", DateFilter.ALL_TIME.label)
    }

    // === ChargeTypeFilter Enum Tests ===

    @Test
    fun `ChargeTypeFilter has correct labels`() {
        assertEquals("All", ChargeTypeFilter.ALL.label)
        assertEquals("AC", ChargeTypeFilter.AC.label)
        assertEquals("DC", ChargeTypeFilter.DC.label)
    }
}
