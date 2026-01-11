package com.matedroid.data.repository

import com.matedroid.data.api.TeslamateApi
import com.matedroid.data.api.models.*
import com.matedroid.data.local.AppSettings
import com.matedroid.data.local.SettingsDataStore
import com.matedroid.di.TeslamateApiFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import javax.net.ssl.SSLHandshakeException

/**
 * Unit tests for TeslamateRepository.
 * Tests all API methods with success and error scenarios.
 */
class TeslamateRepositoryTest {

    private lateinit var apiFactory: TeslamateApiFactory
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var teslamateApi: TeslamateApi
    private lateinit var repository: TeslamateRepository

    private val testSettings = AppSettings(
        serverUrl = "https://teslamate.example.com",
        apiToken = "test-token",
        acceptInvalidCerts = false
    )

    @Before
    fun setup() {
        apiFactory = mockk()
        settingsDataStore = mockk()
        teslamateApi = mockk()

        every { settingsDataStore.settings } returns flowOf(testSettings)
        every { apiFactory.create(any(), any()) } returns teslamateApi

        repository = TeslamateRepository(apiFactory, settingsDataStore)
    }

    // === testConnection Tests ===

    @Test
    fun `testConnection returns success when ping succeeds`() = runTest {
        coEvery { teslamateApi.ping() } returns Response.success(PingResponse(ping = "pong"))

        val result = repository.testConnection("https://teslamate.example.com", false)

        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `testConnection returns error with status code on HTTP error`() = runTest {
        coEvery { teslamateApi.ping() } returns Response.error(
            401,
            okhttp3.ResponseBody.create(null, "Unauthorized")
        )

        val result = repository.testConnection("https://teslamate.example.com", false)

        assertTrue(result is ApiResult.Error)
        assertEquals(401, (result as ApiResult.Error).code)
        assertTrue(result.message.contains("401"))
    }

    @Test
    fun `testConnection returns SSL error message on SSLHandshakeException`() = runTest {
        coEvery { teslamateApi.ping() } throws SSLHandshakeException("Certificate not trusted")

        val result = repository.testConnection("https://teslamate.example.com", false)

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).message.contains("SSL certificate error"))
    }

    @Test
    fun `testConnection returns error message on generic exception`() = runTest {
        coEvery { teslamateApi.ping() } throws RuntimeException("Connection refused")

        val result = repository.testConnection("https://teslamate.example.com", false)

        assertTrue(result is ApiResult.Error)
        assertEquals("Connection refused", (result as ApiResult.Error).message)
    }

    // === getCars Tests ===

    @Test
    fun `getCars returns list of cars on success`() = runTest {
        val cars = listOf(
            CarData(carId = 1, name = "Model 3"),
            CarData(carId = 2, name = "Model Y")
        )
        val response = CarsResponse(data = CarsData(cars = cars))
        coEvery { teslamateApi.getCars() } returns Response.success(response)

        val result = repository.getCars()

        assertTrue(result is ApiResult.Success)
        assertEquals(2, (result as ApiResult.Success).data.size)
        assertEquals("Model 3", result.data[0].name)
    }

    @Test
    fun `getCars returns empty list when no cars`() = runTest {
        val response = CarsResponse(data = CarsData(cars = emptyList()))
        coEvery { teslamateApi.getCars() } returns Response.success(response)

        val result = repository.getCars()

        assertTrue(result is ApiResult.Success)
        assertTrue((result as ApiResult.Success).data.isEmpty())
    }

    @Test
    fun `getCars returns error when server not configured`() = runTest {
        val emptySettings = AppSettings(serverUrl = "")
        every { settingsDataStore.settings } returns flowOf(emptySettings)
        repository = TeslamateRepository(apiFactory, settingsDataStore)

        val result = repository.getCars()

        assertTrue(result is ApiResult.Error)
        assertEquals("Server not configured", (result as ApiResult.Error).message)
    }

    @Test
    fun `getCars returns error on HTTP error`() = runTest {
        coEvery { teslamateApi.getCars() } returns Response.error(
            500,
            okhttp3.ResponseBody.create(null, "Server error")
        )

        val result = repository.getCars()

        assertTrue(result is ApiResult.Error)
        assertEquals(500, (result as ApiResult.Error).code)
    }

    @Test
    fun `getCars returns error on exception`() = runTest {
        coEvery { teslamateApi.getCars() } throws RuntimeException("Network error")

        val result = repository.getCars()

        assertTrue(result is ApiResult.Error)
        assertEquals("Network error", (result as ApiResult.Error).message)
    }

    // === getCarStatus Tests ===

    @Test
    fun `getCarStatus returns status with units on success`() = runTest {
        val status = CarStatus(
            displayName = "My Tesla",
            state = "online",
            batteryDetails = BatteryDetails(batteryLevel = 80)
        )
        val units = Units(unitOfLength = "km", unitOfTemperature = "C")
        val response = CarStatusResponse(data = CarStatusData(status = status, units = units))
        coEvery { teslamateApi.getCarStatus(1) } returns Response.success(response)

        val result = repository.getCarStatus(1)

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals("My Tesla", data.status.displayName)
        assertEquals(80, data.status.batteryLevel)
        assertEquals("km", data.units.unitOfLength)
    }

    @Test
    fun `getCarStatus returns error when no status data`() = runTest {
        val response = CarStatusResponse(data = CarStatusData(status = null, units = null))
        coEvery { teslamateApi.getCarStatus(1) } returns Response.success(response)

        val result = repository.getCarStatus(1)

        assertTrue(result is ApiResult.Error)
        assertEquals("No status data returned", (result as ApiResult.Error).message)
    }

    @Test
    fun `getCarStatus returns default units when units null`() = runTest {
        val status = CarStatus(displayName = "Tesla", state = "asleep")
        val response = CarStatusResponse(data = CarStatusData(status = status, units = null))
        coEvery { teslamateApi.getCarStatus(1) } returns Response.success(response)

        val result = repository.getCarStatus(1)

        assertTrue(result is ApiResult.Success)
        assertNotNull((result as ApiResult.Success).data.units)
    }

    // === getCharges Tests ===

    @Test
    fun `getCharges returns list of charges on success`() = runTest {
        val charges = listOf(
            ChargeData(chargeId = 1, chargeEnergyAdded = 50.0),
            ChargeData(chargeId = 2, chargeEnergyAdded = 30.0)
        )
        val response = ChargesResponse(data = ChargesData(charges = charges))
        coEvery { teslamateApi.getCharges(1, any(), any(), any(), any()) } returns Response.success(response)

        val result = repository.getCharges(1)

        assertTrue(result is ApiResult.Success)
        assertEquals(2, (result as ApiResult.Success).data.size)
    }

    @Test
    fun `getCharges passes date parameters`() = runTest {
        val response = ChargesResponse(data = ChargesData(charges = emptyList()))
        coEvery {
            teslamateApi.getCharges(1, "2024-01-01T00:00:00Z", "2024-01-31T23:59:59Z", 1, 50000)
        } returns Response.success(response)

        val result = repository.getCharges(1, "2024-01-01T00:00:00Z", "2024-01-31T23:59:59Z")

        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `getCharges returns empty list when no charges`() = runTest {
        val response = ChargesResponse(data = ChargesData(charges = null))
        coEvery { teslamateApi.getCharges(any(), any(), any(), any(), any()) } returns Response.success(response)

        val result = repository.getCharges(1)

        assertTrue(result is ApiResult.Success)
        assertTrue((result as ApiResult.Success).data.isEmpty())
    }

    // === getChargeDetail Tests ===

    @Test
    fun `getChargeDetail returns charge detail on success`() = runTest {
        val detail = ChargeDetail(
            chargeId = 1,
            chargeEnergyAdded = 45.0,
            chargeEnergyUsed = 48.0
        )
        val response = ChargeDetailResponse(data = ChargeDetailData(charge = detail))
        coEvery { teslamateApi.getChargeDetail(1, 1) } returns Response.success(response)

        val result = repository.getChargeDetail(1, 1)

        assertTrue(result is ApiResult.Success)
        assertEquals(45.0, (result as ApiResult.Success).data.chargeEnergyAdded!!, 0.01)
    }

    @Test
    fun `getChargeDetail returns error when no detail`() = runTest {
        val response = ChargeDetailResponse(data = ChargeDetailData(charge = null))
        coEvery { teslamateApi.getChargeDetail(1, 1) } returns Response.success(response)

        val result = repository.getChargeDetail(1, 1)

        assertTrue(result is ApiResult.Error)
        assertEquals("No charge detail returned", (result as ApiResult.Error).message)
    }

    // === getDrives Tests ===

    @Test
    fun `getDrives returns list of drives on success`() = runTest {
        val drives = listOf(
            DriveData(driveId = 1, odometerDetails = DriveOdometerDetails(distance = 100.0)),
            DriveData(driveId = 2, odometerDetails = DriveOdometerDetails(distance = 50.0))
        )
        val response = DrivesResponse(data = DrivesData(drives = drives))
        coEvery { teslamateApi.getDrives(1, any(), any(), any(), any()) } returns Response.success(response)

        val result = repository.getDrives(1)

        assertTrue(result is ApiResult.Success)
        assertEquals(2, (result as ApiResult.Success).data.size)
    }

    @Test
    fun `getDrives passes date parameters`() = runTest {
        val response = DrivesResponse(data = DrivesData(drives = emptyList()))
        coEvery {
            teslamateApi.getDrives(1, "2024-01-01T00:00:00Z", "2024-01-31T23:59:59Z", 1, 50000)
        } returns Response.success(response)

        val result = repository.getDrives(1, "2024-01-01T00:00:00Z", "2024-01-31T23:59:59Z")

        assertTrue(result is ApiResult.Success)
    }

    // === getDriveDetail Tests ===

    @Test
    fun `getDriveDetail returns drive detail on success`() = runTest {
        val detail = DriveDetail(
            driveId = 1,
            odometerDetails = DriveOdometerDetails(distance = 150.0),
            durationMin = 90
        )
        val response = DriveDetailResponse(data = DriveDetailData(drive = detail))
        coEvery { teslamateApi.getDriveDetail(1, 1) } returns Response.success(response)

        val result = repository.getDriveDetail(1, 1)

        assertTrue(result is ApiResult.Success)
        assertEquals(150.0, (result as ApiResult.Success).data.distance!!, 0.01)
    }

    @Test
    fun `getDriveDetail returns error when no detail`() = runTest {
        val response = DriveDetailResponse(data = DriveDetailData(drive = null))
        coEvery { teslamateApi.getDriveDetail(1, 1) } returns Response.success(response)

        val result = repository.getDriveDetail(1, 1)

        assertTrue(result is ApiResult.Error)
        assertEquals("No drive detail returned", (result as ApiResult.Error).message)
    }

    // === getBatteryHealth Tests ===

    @Test
    fun `getBatteryHealth returns battery health on success`() = runTest {
        val health = BatteryHealth(
            batteryHealthPercentage = 95.0,
            maxCapacity = 75.0,
            currentCapacity = 71.25
        )
        val response = BatteryHealthResponse(data = BatteryHealthData(batteryHealth = health))
        coEvery { teslamateApi.getBatteryHealth(1) } returns Response.success(response)

        val result = repository.getBatteryHealth(1)

        assertTrue(result is ApiResult.Success)
        assertEquals(95.0, (result as ApiResult.Success).data.batteryHealthPercentage!!, 0.01)
    }

    @Test
    fun `getBatteryHealth returns error when no data`() = runTest {
        val response = BatteryHealthResponse(data = BatteryHealthData(batteryHealth = null))
        coEvery { teslamateApi.getBatteryHealth(1) } returns Response.success(response)

        val result = repository.getBatteryHealth(1)

        assertTrue(result is ApiResult.Error)
        assertEquals("No battery health data returned", (result as ApiResult.Error).message)
    }

    // === getUpdates Tests ===

    @Test
    fun `getUpdates returns list of updates on success`() = runTest {
        val updates = listOf(
            UpdateData(id = 1, version = "2024.1.1"),
            UpdateData(id = 2, version = "2024.2.1")
        )
        val response = UpdatesResponse(data = UpdatesResponseData(updates = updates))
        coEvery { teslamateApi.getUpdates(1, any(), any()) } returns Response.success(response)

        val result = repository.getUpdates(1)

        assertTrue(result is ApiResult.Success)
        assertEquals(2, (result as ApiResult.Success).data.size)
        assertEquals("2024.1.1", result.data[0].version)
    }

    @Test
    fun `getUpdates returns empty list when no updates`() = runTest {
        val response = UpdatesResponse(data = UpdatesResponseData(updates = null))
        coEvery { teslamateApi.getUpdates(any(), any(), any()) } returns Response.success(response)

        val result = repository.getUpdates(1)

        assertTrue(result is ApiResult.Success)
        assertTrue((result as ApiResult.Success).data.isEmpty())
    }

    // === ApiResult Tests ===

    @Test
    fun `ApiResult Success contains correct data`() {
        val data = listOf("item1", "item2")
        val result: ApiResult<List<String>> = ApiResult.Success(data)

        assertTrue(result is ApiResult.Success)
        assertEquals(data, (result as ApiResult.Success).data)
    }

    @Test
    fun `ApiResult Error contains message and code`() {
        val result: ApiResult<Nothing> = ApiResult.Error("Test error", 404)

        assertTrue(result is ApiResult.Error)
        assertEquals("Test error", (result as ApiResult.Error).message)
        assertEquals(404, result.code)
    }

    @Test
    fun `ApiResult Error code is optional`() {
        val result: ApiResult<Nothing> = ApiResult.Error("Test error")

        assertTrue(result is ApiResult.Error)
        assertNull((result as ApiResult.Error).code)
    }
}
