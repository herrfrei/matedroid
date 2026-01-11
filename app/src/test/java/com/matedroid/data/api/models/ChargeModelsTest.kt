package com.matedroid.data.api.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ChargeModels data classes.
 * Tests convenience properties and computed values.
 */
class ChargeModelsTest {

    // === ChargeData Convenience Accessors Tests ===

    @Test
    fun `ChargeData startBatteryLevel returns value from batteryDetails`() {
        val charge = ChargeData(
            chargeId = 1,
            batteryDetails = ChargeBatteryDetails(startBatteryLevel = 20)
        )
        assertEquals(20, charge.startBatteryLevel)
    }

    @Test
    fun `ChargeData endBatteryLevel returns value from batteryDetails`() {
        val charge = ChargeData(
            chargeId = 1,
            batteryDetails = ChargeBatteryDetails(endBatteryLevel = 80)
        )
        assertEquals(80, charge.endBatteryLevel)
    }

    @Test
    fun `ChargeData startBatteryLevel returns null when batteryDetails is null`() {
        val charge = ChargeData(chargeId = 1)
        assertNull(charge.startBatteryLevel)
    }

    @Test
    fun `ChargeData startRatedRangeKm returns value from rangeRated`() {
        val charge = ChargeData(
            chargeId = 1,
            rangeRated = ChargeRange(startRange = 100.0)
        )
        assertEquals(100.0, charge.startRatedRangeKm)
    }

    @Test
    fun `ChargeData endRatedRangeKm returns value from rangeRated`() {
        val charge = ChargeData(
            chargeId = 1,
            rangeRated = ChargeRange(endRange = 350.0)
        )
        assertEquals(350.0, charge.endRatedRangeKm)
    }

    @Test
    fun `ChargeData endRatedRangeKm returns null when rangeRated is null`() {
        val charge = ChargeData(chargeId = 1)
        assertNull(charge.endRatedRangeKm)
    }

    // === ChargeDetail Convenience Accessors Tests ===

    @Test
    fun `ChargeDetail startBatteryLevel returns value from batteryDetails`() {
        val detail = ChargeDetail(
            chargeId = 1,
            batteryDetails = ChargeBatteryDetails(startBatteryLevel = 15)
        )
        assertEquals(15, detail.startBatteryLevel)
    }

    @Test
    fun `ChargeDetail endBatteryLevel returns value from batteryDetails`() {
        val detail = ChargeDetail(
            chargeId = 1,
            batteryDetails = ChargeBatteryDetails(endBatteryLevel = 90)
        )
        assertEquals(90, detail.endBatteryLevel)
    }

    // === ChargePoint Convenience Accessors Tests ===

    @Test
    fun `ChargePoint chargerPower returns value from chargerDetails`() {
        val point = ChargePoint(
            chargerDetails = ChargerDetails(chargerPower = 150)
        )
        assertEquals(150, point.chargerPower)
    }

    @Test
    fun `ChargePoint chargerVoltage returns value from chargerDetails`() {
        val point = ChargePoint(
            chargerDetails = ChargerDetails(chargerVoltage = 400)
        )
        assertEquals(400, point.chargerVoltage)
    }

    @Test
    fun `ChargePoint chargerCurrent returns value from chargerDetails`() {
        val point = ChargePoint(
            chargerDetails = ChargerDetails(chargerActualCurrent = 250)
        )
        assertEquals(250, point.chargerCurrent)
    }

    @Test
    fun `ChargePoint chargerPower returns null when chargerDetails is null`() {
        val point = ChargePoint()
        assertNull(point.chargerPower)
    }

    // === ChargerDetails Tests ===

    @Test
    fun `ChargerDetails stores all charger information`() {
        val details = ChargerDetails(
            chargerPower = 150,
            chargerVoltage = 400,
            chargerActualCurrent = 375,
            chargerPhases = 0,
            fastChargerPresent = true,
            fastChargerBrand = "Tesla",
            fastChargerType = "Supercharger"
        )
        assertEquals(150, details.chargerPower)
        assertEquals(400, details.chargerVoltage)
        assertEquals(375, details.chargerActualCurrent)
        assertEquals(0, details.chargerPhases)
        assertTrue(details.fastChargerPresent == true)
        assertEquals("Tesla", details.fastChargerBrand)
        assertEquals("Supercharger", details.fastChargerType)
    }

    @Test
    fun `ChargerDetails for AC charging`() {
        val details = ChargerDetails(
            chargerPower = 11,
            chargerVoltage = 230,
            chargerActualCurrent = 16,
            chargerPhases = 3,
            fastChargerPresent = false
        )
        assertEquals(11, details.chargerPower)
        assertEquals(3, details.chargerPhases)
        assertFalse(details.fastChargerPresent == true)
    }

    // === ChargeBatteryDetails Tests ===

    @Test
    fun `ChargeBatteryDetails stores battery levels`() {
        val details = ChargeBatteryDetails(
            startBatteryLevel = 25,
            endBatteryLevel = 85
        )
        assertEquals(25, details.startBatteryLevel)
        assertEquals(85, details.endBatteryLevel)
    }

    // === ChargeRange Tests ===

    @Test
    fun `ChargeRange stores range values`() {
        val range = ChargeRange(
            startRange = 100.0,
            endRange = 350.0
        )
        assertEquals(100.0, range.startRange)
        assertEquals(350.0, range.endRange)
    }

    // === ChargeBatteryInfo Tests ===

    @Test
    fun `ChargeBatteryInfo stores battery info`() {
        val info = ChargeBatteryInfo(
            idealBatteryRangeKm = 400.0,
            ratedBatteryRangeKm = 350.0,
            usableBatteryLevel = 78
        )
        assertEquals(400.0, info.idealBatteryRangeKm)
        assertEquals(350.0, info.ratedBatteryRangeKm)
        assertEquals(78, info.usableBatteryLevel)
    }

    // === Typical Charging Scenarios Tests ===

    @Test
    fun `typical home AC charging scenario`() {
        val charge = ChargeData(
            chargeId = 1,
            startDate = "2024-01-15T22:00:00",
            endDate = "2024-01-16T06:00:00",
            address = "Home",
            chargeEnergyAdded = 45.0,
            chargeEnergyUsed = 48.0,
            cost = 8.0,
            durationMin = 480, // 8 hours
            batteryDetails = ChargeBatteryDetails(startBatteryLevel = 20, endBatteryLevel = 80),
            rangeRated = ChargeRange(startRange = 80.0, endRange = 320.0)
        )

        assertEquals(20, charge.startBatteryLevel)
        assertEquals(80, charge.endBatteryLevel)
        assertEquals(80.0, charge.startRatedRangeKm)
        assertEquals(320.0, charge.endRatedRangeKm)
    }

    @Test
    fun `typical supercharger DC charging scenario`() {
        val charge = ChargeData(
            chargeId = 2,
            startDate = "2024-01-20T14:00:00",
            endDate = "2024-01-20T14:30:00",
            address = "Tesla Supercharger",
            chargeEnergyAdded = 50.0,
            chargeEnergyUsed = 55.0,
            cost = 15.0,
            durationMin = 30,
            batteryDetails = ChargeBatteryDetails(startBatteryLevel = 15, endBatteryLevel = 80),
            rangeRated = ChargeRange(startRange = 60.0, endRange = 320.0)
        )

        assertEquals(15, charge.startBatteryLevel)
        assertEquals(80, charge.endBatteryLevel)
        assertEquals(50.0, charge.chargeEnergyAdded)
        assertEquals(30, charge.durationMin)
    }

    @Test
    fun `charge detail with charge points`() {
        val points = listOf(
            ChargePoint(
                date = "2024-01-20T14:00:00",
                batteryLevel = 15,
                chargeEnergyAdded = 0.0,
                chargerDetails = ChargerDetails(chargerPower = 150, chargerPhases = 0),
                outsideTemp = 20.0
            ),
            ChargePoint(
                date = "2024-01-20T14:15:00",
                batteryLevel = 50,
                chargeEnergyAdded = 25.0,
                chargerDetails = ChargerDetails(chargerPower = 120, chargerPhases = 0),
                outsideTemp = 20.0
            ),
            ChargePoint(
                date = "2024-01-20T14:30:00",
                batteryLevel = 80,
                chargeEnergyAdded = 50.0,
                chargerDetails = ChargerDetails(chargerPower = 50, chargerPhases = 0),
                outsideTemp = 21.0
            )
        )

        val detail = ChargeDetail(
            chargeId = 2,
            chargePoints = points,
            chargeEnergyAdded = 50.0
        )

        assertEquals(3, detail.chargePoints?.size)
        assertEquals(150, detail.chargePoints?.first()?.chargerPower)
        assertEquals(50, detail.chargePoints?.last()?.chargerPower)
    }
}
