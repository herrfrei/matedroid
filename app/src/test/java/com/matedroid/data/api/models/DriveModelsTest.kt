package com.matedroid.data.api.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DriveModels data classes.
 * Tests convenience properties and computed values.
 */
class DriveModelsTest {

    // === DriveData Convenience Accessors Tests ===

    @Test
    fun `DriveData id returns driveId`() {
        val drive = DriveData(driveId = 123)
        assertEquals(123, drive.id)
    }

    @Test
    fun `DriveData distance returns value from odometerDetails`() {
        val drive = DriveData(
            driveId = 1,
            odometerDetails = DriveOdometerDetails(distance = 150.5)
        )
        assertEquals(150.5, drive.distance)
    }

    @Test
    fun `DriveData distance returns null when odometerDetails is null`() {
        val drive = DriveData(driveId = 1)
        assertNull(drive.distance)
    }

    @Test
    fun `DriveData startBatteryLevel returns value from batteryDetails`() {
        val drive = DriveData(
            driveId = 1,
            batteryDetails = DriveBatteryDetails(startBatteryLevel = 80)
        )
        assertEquals(80, drive.startBatteryLevel)
    }

    @Test
    fun `DriveData endBatteryLevel returns value from batteryDetails`() {
        val drive = DriveData(
            driveId = 1,
            batteryDetails = DriveBatteryDetails(endBatteryLevel = 60)
        )
        assertEquals(60, drive.endBatteryLevel)
    }

    @Test
    fun `DriveData startRatedRangeKm returns value from rangeRated`() {
        val drive = DriveData(
            driveId = 1,
            rangeRated = DriveRange(startRange = 300.0)
        )
        assertEquals(300.0, drive.startRatedRangeKm)
    }

    @Test
    fun `DriveData endRatedRangeKm returns value from rangeRated`() {
        val drive = DriveData(
            driveId = 1,
            rangeRated = DriveRange(endRange = 200.0)
        )
        assertEquals(200.0, drive.endRatedRangeKm)
    }

    // === DriveData efficiencyWhKm Tests ===

    @Test
    fun `DriveData efficiencyWhKm calculates correctly`() {
        val drive = DriveData(
            driveId = 1,
            odometerDetails = DriveOdometerDetails(distance = 100.0),
            energyConsumedNet = 15.0 // 15 kWh for 100 km = 150 Wh/km
        )
        assertEquals(150.0, drive.efficiencyWhKm!!, 0.01)
    }

    @Test
    fun `DriveData efficiencyWhKm returns null when distance is null`() {
        val drive = DriveData(
            driveId = 1,
            odometerDetails = null,
            energyConsumedNet = 15.0
        )
        assertNull(drive.efficiencyWhKm)
    }

    @Test
    fun `DriveData efficiencyWhKm returns null when distance is 0`() {
        val drive = DriveData(
            driveId = 1,
            odometerDetails = DriveOdometerDetails(distance = 0.0),
            energyConsumedNet = 15.0
        )
        assertNull(drive.efficiencyWhKm)
    }

    @Test
    fun `DriveData efficiencyWhKm returns null when distance is negative`() {
        val drive = DriveData(
            driveId = 1,
            odometerDetails = DriveOdometerDetails(distance = -10.0),
            energyConsumedNet = 15.0
        )
        assertNull(drive.efficiencyWhKm)
    }

    @Test
    fun `DriveData efficiencyWhKm returns null when energyConsumedNet is null`() {
        val drive = DriveData(
            driveId = 1,
            odometerDetails = DriveOdometerDetails(distance = 100.0),
            energyConsumedNet = null
        )
        assertNull(drive.efficiencyWhKm)
    }

    @Test
    fun `DriveData efficiencyWhKm handles typical highway efficiency`() {
        // 200 km at 18 kWh = 90 Wh/km (very efficient highway driving)
        val drive = DriveData(
            driveId = 1,
            odometerDetails = DriveOdometerDetails(distance = 200.0),
            energyConsumedNet = 18.0
        )
        assertEquals(90.0, drive.efficiencyWhKm!!, 0.01)
    }

    @Test
    fun `DriveData efficiencyWhKm handles typical city efficiency`() {
        // 50 km at 10 kWh = 200 Wh/km (city driving with heating)
        val drive = DriveData(
            driveId = 1,
            odometerDetails = DriveOdometerDetails(distance = 50.0),
            energyConsumedNet = 10.0
        )
        assertEquals(200.0, drive.efficiencyWhKm!!, 0.01)
    }

    // === DriveDetail Tests ===

    @Test
    fun `DriveDetail id returns driveId`() {
        val detail = DriveDetail(driveId = 456)
        assertEquals(456, detail.id)
    }

    @Test
    fun `DriveDetail distance returns value from odometerDetails`() {
        val detail = DriveDetail(
            driveId = 1,
            odometerDetails = DriveOdometerDetails(distance = 250.0)
        )
        assertEquals(250.0, detail.distance)
    }

    @Test
    fun `DriveDetail startBatteryLevel returns value from batteryDetails`() {
        val detail = DriveDetail(
            driveId = 1,
            batteryDetails = DriveBatteryDetails(startBatteryLevel = 90)
        )
        assertEquals(90, detail.startBatteryLevel)
    }

    @Test
    fun `DriveDetail endBatteryLevel returns value from batteryDetails`() {
        val detail = DriveDetail(
            driveId = 1,
            batteryDetails = DriveBatteryDetails(endBatteryLevel = 70)
        )
        assertEquals(70, detail.endBatteryLevel)
    }

    // === DrivePosition Convenience Accessors Tests ===

    @Test
    fun `DrivePosition insideTemp returns value from climateInfo`() {
        val position = DrivePosition(
            climateInfo = DriveClimateInfo(insideTemp = 22.0)
        )
        assertEquals(22.0, position.insideTemp)
    }

    @Test
    fun `DrivePosition outsideTemp returns value from climateInfo`() {
        val position = DrivePosition(
            climateInfo = DriveClimateInfo(outsideTemp = 15.0)
        )
        assertEquals(15.0, position.outsideTemp)
    }

    @Test
    fun `DrivePosition isClimateOn returns true when climate is on`() {
        val position = DrivePosition(
            climateInfo = DriveClimateInfo(isClimateOn = true)
        )
        assertTrue(position.isClimateOn)
    }

    @Test
    fun `DrivePosition isClimateOn returns false when climate is off`() {
        val position = DrivePosition(
            climateInfo = DriveClimateInfo(isClimateOn = false)
        )
        assertFalse(position.isClimateOn)
    }

    @Test
    fun `DrivePosition isClimateOn returns false when climateInfo is null`() {
        val position = DrivePosition()
        assertFalse(position.isClimateOn)
    }

    // === DriveOdometerDetails Tests ===

    @Test
    fun `DriveOdometerDetails stores odometer values`() {
        val details = DriveOdometerDetails(
            odometerStart = 10000.0,
            odometerEnd = 10150.0,
            distance = 150.0
        )
        assertEquals(10000.0, details.odometerStart)
        assertEquals(10150.0, details.odometerEnd)
        assertEquals(150.0, details.distance)
    }

    // === DriveRange Tests ===

    @Test
    fun `DriveRange stores range values`() {
        val range = DriveRange(
            startRange = 350.0,
            endRange = 250.0,
            rangeDiff = -100.0
        )
        assertEquals(350.0, range.startRange)
        assertEquals(250.0, range.endRange)
        assertEquals(-100.0, range.rangeDiff)
    }
}
