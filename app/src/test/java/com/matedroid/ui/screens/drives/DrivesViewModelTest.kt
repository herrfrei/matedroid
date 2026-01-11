package com.matedroid.ui.screens.drives

import com.matedroid.data.api.models.DriveData
import com.matedroid.data.api.models.DriveOdometerDetails
import com.matedroid.data.api.models.Units
import com.matedroid.data.local.AppSettings
import com.matedroid.data.local.SettingsDataStore
import com.matedroid.data.repository.ApiResult
import com.matedroid.data.repository.CarStatusWithUnits
import com.matedroid.data.repository.TeslamateRepository
import com.matedroid.data.api.models.CarStatus
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
 * Unit tests for DrivesViewModel.
 * Tests date filtering, distance filtering, summary calculation, and chart data.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DrivesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TeslamateRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var viewModel: DrivesViewModel

    private val testSettings = AppSettings(serverUrl = "https://teslamate.example.com")
    private val testUnits = Units(unitOfLength = "km", unitOfTemperature = "C")
    private val testCarStatus = CarStatus(displayName = "Tesla", state = "online")
    private val testStatusWithUnits = CarStatusWithUnits(status = testCarStatus, units = testUnits)

    private val testDrives = listOf(
        DriveData(
            driveId = 1,
            startDate = "2024-01-15T10:00:00",
            odometerDetails = DriveOdometerDetails(distance = 5.0), // Commute
            durationMin = 15,
            speedMax = 80
        ),
        DriveData(
            driveId = 2,
            startDate = "2024-01-20T14:00:00",
            odometerDetails = DriveOdometerDetails(distance = 50.0), // Day trip
            durationMin = 45,
            speedMax = 120
        ),
        DriveData(
            driveId = 3,
            startDate = "2024-01-25T08:00:00",
            odometerDetails = DriveOdometerDetails(distance = 200.0), // Road trip
            durationMin = 150,
            speedMax = 130
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        settingsDataStore = mockk()

        every { settingsDataStore.settings } returns flowOf(testSettings)
        every { settingsDataStore.showShortDrivesCharges } returns flowOf(false)
        coEvery { repository.getCarStatus(any()) } returns ApiResult.Success(testStatusWithUnits)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): DrivesViewModel {
        return DrivesViewModel(repository, settingsDataStore)
    }

    // === Basic Loading Tests ===

    @Test
    fun `setCarId loads drives and units`() = runTest {
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Success(testDrives)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(3, viewModel.uiState.value.drives.size)
        assertEquals(testUnits, viewModel.uiState.value.units)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `setCarId handles API error`() = runTest {
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Error("Network error")

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Network error", viewModel.uiState.value.error)
    }

    // === Date Filter Tests ===

    @Test
    fun `setDateFilter updates date range`() = runTest {
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Success(testDrives)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        val startDate = LocalDate.now().minusDays(30)
        val endDate = LocalDate.now()
        viewModel.setDateFilter(startDate, endDate)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(startDate, viewModel.uiState.value.startDate)
        assertEquals(endDate, viewModel.uiState.value.endDate)
    }

    @Test
    fun `clearDateFilter removes date range`() = runTest {
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Success(testDrives)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(LocalDate.now().minusDays(7), LocalDate.now())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearDateFilter()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.startDate)
        assertNull(viewModel.uiState.value.endDate)
    }

    // === Distance Filter Tests ===

    @Test
    fun `setDistanceFilter filters commute drives`() = runTest {
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Success(testDrives)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setDistanceFilter(DriveDistanceFilter.COMMUTE)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DriveDistanceFilter.COMMUTE, viewModel.uiState.value.distanceFilter)
        // Only drives < 10km should be shown (drive 1 = 5km)
        assertEquals(1, viewModel.uiState.value.drives.size)
        assertEquals(1, viewModel.uiState.value.drives[0].driveId)
    }

    @Test
    fun `setDistanceFilter filters day trip drives`() = runTest {
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Success(testDrives)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setDistanceFilter(DriveDistanceFilter.DAY_TRIP)
        testDispatcher.scheduler.advanceUntilIdle()

        // Drives between 10-100km (drive 2 = 50km)
        assertEquals(1, viewModel.uiState.value.drives.size)
        assertEquals(2, viewModel.uiState.value.drives[0].driveId)
    }

    @Test
    fun `setDistanceFilter filters road trip drives`() = runTest {
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Success(testDrives)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setDistanceFilter(DriveDistanceFilter.ROAD_TRIP)
        testDispatcher.scheduler.advanceUntilIdle()

        // Drives > 100km (drive 3 = 200km)
        assertEquals(1, viewModel.uiState.value.drives.size)
        assertEquals(3, viewModel.uiState.value.drives[0].driveId)
    }

    @Test
    fun `setDistanceFilter ALL shows all drives`() = runTest {
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Success(testDrives)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setDistanceFilter(DriveDistanceFilter.ROAD_TRIP)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setDistanceFilter(DriveDistanceFilter.ALL)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.drives.size)
    }

    // === Summary Calculation Tests ===

    @Test
    fun `summary calculates correct totals`() = runTest {
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Success(testDrives)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        val summary = viewModel.uiState.value.summary
        assertEquals(3, summary.totalDrives)
        assertEquals(255.0, summary.totalDistanceKm, 0.01) // 5 + 50 + 200
        assertEquals(210, summary.totalDurationMin) // 15 + 45 + 150
        assertEquals(130, summary.maxSpeedKmh)
    }

    @Test
    fun `summary calculates correct averages`() = runTest {
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Success(testDrives)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        val summary = viewModel.uiState.value.summary
        assertEquals(85.0, summary.avgDistancePerDrive, 0.01) // 255 / 3
        assertEquals(70, summary.avgDurationPerDrive) // 210 / 3
    }

    @Test
    fun `summary is empty for no drives`() = runTest {
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Success(emptyList())

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        val summary = viewModel.uiState.value.summary
        assertEquals(0, summary.totalDrives)
        assertEquals(0.0, summary.totalDistanceKm, 0.01)
        assertEquals(0, summary.maxSpeedKmh)
    }

    // === Short Drives Filter Tests ===

    @Test
    fun `short drives are filtered when setting is disabled`() = runTest {
        val drivesWithShort = listOf(
            DriveData(
                driveId = 1,
                startDate = "2024-01-15T10:00:00",
                odometerDetails = DriveOdometerDetails(distance = 0.05), // Very short
                durationMin = 0
            ),
            DriveData(
                driveId = 2,
                startDate = "2024-01-20T14:00:00",
                odometerDetails = DriveOdometerDetails(distance = 50.0),
                durationMin = 45
            )
        )
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Success(drivesWithShort)
        every { settingsDataStore.showShortDrivesCharges } returns flowOf(false)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        // Short drive (< 0.1km or < 1 min) should be filtered
        assertEquals(1, viewModel.uiState.value.drives.size)
        assertEquals(2, viewModel.uiState.value.drives[0].driveId)
    }

    @Test
    fun `short drives are shown when setting is enabled`() = runTest {
        val drivesWithShort = listOf(
            DriveData(
                driveId = 1,
                startDate = "2024-01-15T10:00:00",
                odometerDetails = DriveOdometerDetails(distance = 0.05),
                durationMin = 0
            ),
            DriveData(
                driveId = 2,
                startDate = "2024-01-20T14:00:00",
                odometerDetails = DriveOdometerDetails(distance = 50.0),
                durationMin = 45
            )
        )
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Success(drivesWithShort)
        every { settingsDataStore.showShortDrivesCharges } returns flowOf(true)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.drives.size)
    }

    // === Refresh Tests ===

    @Test
    fun `refresh reloads drives`() = runTest {
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Success(testDrives)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)
        coVerify(exactly = 2) { repository.getDrives(1, any(), any()) }
    }

    @Test
    fun `refresh does nothing when no car selected`() = runTest {
        viewModel = createViewModel()

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { repository.getDrives(any(), any(), any()) }
    }

    // === Error Handling Tests ===

    @Test
    fun `clearError clears error state`() = runTest {
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Error("Test error")

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Test error", viewModel.uiState.value.error)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }

    // === DriveDistanceFilter Tests ===

    @Test
    fun `DriveDistanceFilter has correct distance boundaries`() {
        // Commute: < 10 km
        assertNull(DriveDistanceFilter.COMMUTE.minDistanceKm)
        assertEquals(10.0, DriveDistanceFilter.COMMUTE.maxDistanceKm)

        // Day trip: 10-100 km
        assertEquals(10.0, DriveDistanceFilter.DAY_TRIP.minDistanceKm)
        assertEquals(100.0, DriveDistanceFilter.DAY_TRIP.maxDistanceKm)

        // Road trip: > 100 km
        assertEquals(100.0, DriveDistanceFilter.ROAD_TRIP.minDistanceKm)
        assertNull(DriveDistanceFilter.ROAD_TRIP.maxDistanceKm)

        // All: no restrictions
        assertNull(DriveDistanceFilter.ALL.minDistanceKm)
        assertNull(DriveDistanceFilter.ALL.maxDistanceKm)
    }

    @Test
    fun `DriveDistanceFilter getLabel returns correct metric labels`() {
        val metricUnits = Units(unitOfLength = "km")

        assertEquals("All", DriveDistanceFilter.ALL.getLabel(metricUnits))
        assertEquals("Commute (< 10 km)", DriveDistanceFilter.COMMUTE.getLabel(metricUnits))
        assertEquals("Day trip (10-100 km)", DriveDistanceFilter.DAY_TRIP.getLabel(metricUnits))
        assertEquals("Road trip (> 100 km)", DriveDistanceFilter.ROAD_TRIP.getLabel(metricUnits))
    }

    @Test
    fun `DriveDistanceFilter getLabel returns correct imperial labels`() {
        val imperialUnits = Units(unitOfLength = "mi")

        assertEquals("All", DriveDistanceFilter.ALL.getLabel(imperialUnits))
        assertEquals("Commute (< 6 mi)", DriveDistanceFilter.COMMUTE.getLabel(imperialUnits))
        assertEquals("Day trip (6-60 mi)", DriveDistanceFilter.DAY_TRIP.getLabel(imperialUnits))
        assertEquals("Road trip (> 60 mi)", DriveDistanceFilter.ROAD_TRIP.getLabel(imperialUnits))
    }

    // === Chart Granularity Tests ===

    @Test
    fun `chart granularity is monthly for null date range`() = runTest {
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Success(testDrives)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(null, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DriveChartGranularity.MONTHLY, viewModel.uiState.value.chartGranularity)
    }

    @Test
    fun `chart granularity is daily for short date range`() = runTest {
        coEvery { repository.getDrives(1, any(), any()) } returns ApiResult.Success(testDrives)

        viewModel = createViewModel()
        viewModel.setCarId(1)
        viewModel.setDateFilter(LocalDate.now().minusDays(7), LocalDate.now())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DriveChartGranularity.DAILY, viewModel.uiState.value.chartGranularity)
    }
}
