package com.matedroid.domain.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CarStats, QuickStats, DeepStats, and YearFilter models.
 */
class CarStatsTest {

    // === YearFilter Tests ===

    @Test
    fun `YearFilter AllTime displays correctly`() {
        val filter = YearFilter.AllTime
        assertEquals("All Time", filter.toDisplayString())
    }

    @Test
    fun `YearFilter Year displays correctly`() {
        val filter = YearFilter.Year(2024)
        assertEquals("2024", filter.toDisplayString())
    }

    @Test
    fun `YearFilter Year with different years`() {
        assertEquals("2023", YearFilter.Year(2023).toDisplayString())
        assertEquals("2025", YearFilter.Year(2025).toDisplayString())
        assertEquals("2020", YearFilter.Year(2020).toDisplayString())
    }

    @Test
    fun `YearFilter AllTime is singleton`() {
        val filter1 = YearFilter.AllTime
        val filter2 = YearFilter.AllTime
        assertSame(filter1, filter2)
    }

    @Test
    fun `YearFilter Year equals works correctly`() {
        val filter1 = YearFilter.Year(2024)
        val filter2 = YearFilter.Year(2024)
        val filter3 = YearFilter.Year(2023)

        assertEquals(filter1, filter2)
        assertNotEquals(filter1, filter3)
    }

    // === DeepStats acDcRatio Tests ===

    @Test
    fun `DeepStats acDcRatio calculates correctly for all AC`() {
        val stats = createDeepStats(acChargeCount = 100, dcChargeCount = 0)
        assertEquals("100% AC / 0% DC", stats.acDcRatio)
    }

    @Test
    fun `DeepStats acDcRatio calculates correctly for all DC`() {
        val stats = createDeepStats(acChargeCount = 0, dcChargeCount = 100)
        assertEquals("0% AC / 100% DC", stats.acDcRatio)
    }

    @Test
    fun `DeepStats acDcRatio calculates correctly for mixed`() {
        val stats = createDeepStats(acChargeCount = 75, dcChargeCount = 25)
        assertEquals("75% AC / 25% DC", stats.acDcRatio)
    }

    @Test
    fun `DeepStats acDcRatio calculates correctly for even split`() {
        val stats = createDeepStats(acChargeCount = 50, dcChargeCount = 50)
        assertEquals("50% AC / 50% DC", stats.acDcRatio)
    }

    @Test
    fun `DeepStats acDcRatio returns NA for no charges`() {
        val stats = createDeepStats(acChargeCount = 0, dcChargeCount = 0)
        assertEquals("N/A", stats.acDcRatio)
    }

    @Test
    fun `DeepStats acDcRatio handles odd ratios`() {
        // 33% AC, 66% DC (integer division)
        val stats = createDeepStats(acChargeCount = 1, dcChargeCount = 2)
        assertEquals("33% AC / 66% DC", stats.acDcRatio)
    }

    // === DriveElevationRecord Tests ===

    @Test
    fun `DriveElevationRecord stores values correctly`() {
        val record = DriveElevationRecord(
            driveId = 1,
            elevationM = 1500,
            elevationGainM = 300,
            date = "2024-01-15T10:00:00"
        )
        assertEquals(1, record.driveId)
        assertEquals(1500, record.elevationM)
        assertEquals(300, record.elevationGainM)
        assertEquals("2024-01-15T10:00:00", record.date)
    }

    @Test
    fun `DriveElevationRecord allows null elevationGainM`() {
        val record = DriveElevationRecord(
            driveId = 1,
            elevationM = 1500,
            elevationGainM = null,
            date = null
        )
        assertNull(record.elevationGainM)
        assertNull(record.date)
    }

    // === DriveTempRecord Tests ===

    @Test
    fun `DriveTempRecord stores values correctly`() {
        val record = DriveTempRecord(
            driveId = 1,
            tempC = 35.5,
            date = "2024-07-15T14:00:00"
        )
        assertEquals(1, record.driveId)
        assertEquals(35.5, record.tempC, 0.01)
        assertEquals("2024-07-15T14:00:00", record.date)
    }

    @Test
    fun `DriveTempRecord handles negative temperatures`() {
        val record = DriveTempRecord(
            driveId = 1,
            tempC = -15.0,
            date = "2024-01-15T08:00:00"
        )
        assertEquals(-15.0, record.tempC, 0.01)
    }

    // === ChargeTempRecord Tests ===

    @Test
    fun `ChargeTempRecord stores values correctly`() {
        val record = ChargeTempRecord(
            chargeId = 1,
            tempC = 25.0,
            date = "2024-05-15T20:00:00"
        )
        assertEquals(1, record.chargeId)
        assertEquals(25.0, record.tempC, 0.01)
        assertEquals("2024-05-15T20:00:00", record.date)
    }

    // === ChargePowerRecord Tests ===

    @Test
    fun `ChargePowerRecord stores values correctly`() {
        val record = ChargePowerRecord(
            chargeId = 1,
            powerKw = 250,
            date = "2024-03-15T12:00:00"
        )
        assertEquals(1, record.chargeId)
        assertEquals(250, record.powerKw)
        assertEquals("2024-03-15T12:00:00", record.date)
    }

    @Test
    fun `ChargePowerRecord handles typical DC power`() {
        val record = ChargePowerRecord(chargeId = 1, powerKw = 150, date = null)
        assertEquals(150, record.powerKw)
    }

    @Test
    fun `ChargePowerRecord handles typical AC power`() {
        val record = ChargePowerRecord(chargeId = 1, powerKw = 11, date = null)
        assertEquals(11, record.powerKw)
    }

    // === CarStats Tests ===

    @Test
    fun `CarStats stores all components`() {
        val quickStats = createQuickStats()
        val deepStats = createDeepStats()
        val syncProgress = SyncProgress(1, SyncPhase.COMPLETE, 100, 100)

        val carStats = CarStats(
            carId = 1,
            yearFilter = YearFilter.Year(2024),
            quickStats = quickStats,
            deepStats = deepStats,
            syncProgress = syncProgress
        )

        assertEquals(1, carStats.carId)
        assertEquals(YearFilter.Year(2024), carStats.yearFilter)
        assertNotNull(carStats.quickStats)
        assertNotNull(carStats.deepStats)
        assertNotNull(carStats.syncProgress)
    }

    @Test
    fun `CarStats allows null deepStats and syncProgress`() {
        val quickStats = createQuickStats()

        val carStats = CarStats(
            carId = 1,
            yearFilter = YearFilter.AllTime,
            quickStats = quickStats,
            deepStats = null,
            syncProgress = null
        )

        assertNull(carStats.deepStats)
        assertNull(carStats.syncProgress)
    }

    // === Helper Functions ===

    private fun createQuickStats(): QuickStats {
        return QuickStats(
            totalDrives = 100,
            totalDistanceKm = 5000.0,
            totalEnergyConsumedKwh = 800.0,
            avgEfficiencyWhKm = 160.0,
            maxSpeedKmh = 160,
            avgDriveMinutes = 30.0,
            totalDrivingDays = 50,
            totalCharges = 80,
            totalEnergyAddedKwh = 900.0,
            totalCost = 150.0,
            avgCostPerKwh = 0.17,
            avgChargeMinutes = 45.0,
            longestDrive = null,
            fastestDrive = null,
            mostEfficientDrive = null,
            leastEfficientDrive = null,
            biggestCharge = null,
            mostExpensiveCharge = null,
            mostExpensivePerKwhCharge = null,
            firstDriveDate = "2024-01-01T08:00:00",
            firstChargeDate = "2024-01-01T20:00:00",
            busiestDay = null,
            mostDistanceDay = null
        )
    }

    private fun createDeepStats(
        acChargeCount: Int = 50,
        dcChargeCount: Int = 30
    ): DeepStats {
        return DeepStats(
            maxElevationM = 2000,
            minElevationM = -10,
            driveWithMaxElevation = null,
            driveWithMinElevation = null,
            driveWithMostClimbing = null,
            maxOutsideTempDrivingC = 40.0,
            minOutsideTempDrivingC = -15.0,
            maxCabinTempC = 35.0,
            minCabinTempC = 5.0,
            hottestDrive = null,
            coldestDrive = null,
            maxOutsideTempChargingC = 38.0,
            minOutsideTempChargingC = -10.0,
            hottestCharge = null,
            coldestCharge = null,
            maxChargerPowerKw = 250,
            chargeWithMaxPower = null,
            acChargeCount = acChargeCount,
            dcChargeCount = dcChargeCount,
            acChargeEnergyKwh = 500.0,
            dcChargeEnergyKwh = 400.0,
            driveDetailsProcessed = 100,
            chargeDetailsProcessed = 80
        )
    }
}
