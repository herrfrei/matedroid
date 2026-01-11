package com.matedroid.data.api.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CarModels data classes.
 * Tests convenience properties and computed values.
 */
class CarModelsTest {

    // === Units Tests ===

    @Test
    fun `Units isMetric returns true for km`() {
        val units = Units(unitOfLength = "km")
        assertTrue(units.isMetric)
        assertFalse(units.isImperial)
    }

    @Test
    fun `Units isImperial returns true for mi`() {
        val units = Units(unitOfLength = "mi")
        assertTrue(units.isImperial)
        assertFalse(units.isMetric)
    }

    @Test
    fun `Units with null unitOfLength defaults to non-metric non-imperial`() {
        val units = Units(unitOfLength = null)
        assertFalse(units.isMetric)
        assertFalse(units.isImperial)
    }

    // === CarData Tests ===

    @Test
    fun `CarData displayName returns name when not blank`() {
        val carData = CarData(carId = 1, name = "My Tesla")
        assertEquals("My Tesla", carData.displayName)
    }

    @Test
    fun `CarData displayName returns model when name is blank`() {
        val carData = CarData(
            carId = 1,
            name = "",
            carDetails = CarDetails(model = "3")
        )
        assertEquals("Model 3", carData.displayName)
    }

    @Test
    fun `CarData displayName returns model when name is null`() {
        val carData = CarData(
            carId = 1,
            name = null,
            carDetails = CarDetails(model = "Y")
        )
        assertEquals("Model Y", carData.displayName)
    }

    @Test
    fun `CarData displayName returns Tesla when no name or model`() {
        val carData = CarData(carId = 1, name = null, carDetails = null)
        assertEquals("Tesla", carData.displayName)
    }

    @Test
    fun `CarData displayName returns Tesla when name is whitespace only`() {
        val carData = CarData(carId = 1, name = "   ", carDetails = null)
        assertEquals("Tesla", carData.displayName)
    }

    // === CarStatus Convenience Accessors Tests ===

    @Test
    fun `CarStatus batteryLevel returns value from batteryDetails`() {
        val status = CarStatus(
            batteryDetails = BatteryDetails(batteryLevel = 75)
        )
        assertEquals(75, status.batteryLevel)
    }

    @Test
    fun `CarStatus batteryLevel returns null when batteryDetails is null`() {
        val status = CarStatus()
        assertNull(status.batteryLevel)
    }

    @Test
    fun `CarStatus pluggedIn returns value from chargingDetails`() {
        val status = CarStatus(
            chargingDetails = ChargingDetails(pluggedIn = true)
        )
        assertTrue(status.pluggedIn == true)
    }

    @Test
    fun `CarStatus isCharging returns true for charging state`() {
        val status = CarStatus(
            chargingDetails = ChargingDetails(chargingState = "Charging")
        )
        assertTrue(status.isCharging)
    }

    @Test
    fun `CarStatus isCharging is case insensitive`() {
        val status = CarStatus(
            chargingDetails = ChargingDetails(chargingState = "CHARGING")
        )
        assertTrue(status.isCharging)
    }

    @Test
    fun `CarStatus isCharging returns false for other states`() {
        val status = CarStatus(
            chargingDetails = ChargingDetails(chargingState = "Complete")
        )
        assertFalse(status.isCharging)
    }

    @Test
    fun `CarStatus isCharging returns false when chargingState is null`() {
        val status = CarStatus()
        assertFalse(status.isCharging)
    }

    @Test
    fun `CarStatus geofence returns value from carGeodata`() {
        val status = CarStatus(
            carGeodata = CarGeodata(geofence = "Home")
        )
        assertEquals("Home", status.geofence)
    }

    @Test
    fun `CarStatus locked returns value from carStatus`() {
        val status = CarStatus(
            carStatus = CarStatusDetails(locked = true)
        )
        assertTrue(status.locked == true)
    }

    @Test
    fun `CarStatus version returns value from carVersions`() {
        val status = CarStatus(
            carVersions = CarVersions(version = "2024.1.1")
        )
        assertEquals("2024.1.1", status.version)
    }

    @Test
    fun `CarStatus insideTemp returns value from climateDetails`() {
        val status = CarStatus(
            climateDetails = ClimateDetails(insideTemp = 22.5)
        )
        assertEquals(22.5, status.insideTemp)
    }

    @Test
    fun `CarStatus isDcCharging delegates to chargingDetails`() {
        val acCharging = CarStatus(
            chargingDetails = ChargingDetails(chargerPhases = 3)
        )
        assertFalse(acCharging.isDcCharging)

        val dcCharging = CarStatus(
            chargingDetails = ChargingDetails(chargerPhases = 0)
        )
        assertTrue(dcCharging.isDcCharging)
    }

    // === ChargingDetails isDcCharging Tests ===

    @Test
    fun `ChargingDetails isDcCharging returns true for null phases`() {
        val details = ChargingDetails(chargerPhases = null)
        assertTrue(details.isDcCharging)
    }

    @Test
    fun `ChargingDetails isDcCharging returns true for 0 phases`() {
        val details = ChargingDetails(chargerPhases = 0)
        assertTrue(details.isDcCharging)
    }

    @Test
    fun `ChargingDetails isDcCharging returns false for 1 phase AC`() {
        val details = ChargingDetails(chargerPhases = 1)
        assertFalse(details.isDcCharging)
    }

    @Test
    fun `ChargingDetails isDcCharging returns false for 2 phase AC`() {
        val details = ChargingDetails(chargerPhases = 2)
        assertFalse(details.isDcCharging)
    }

    @Test
    fun `ChargingDetails isDcCharging returns false for 3 phase AC`() {
        val details = ChargingDetails(chargerPhases = 3)
        assertFalse(details.isDcCharging)
    }

    // === TpmsDetails Tests ===

    @Test
    fun `TpmsDetails stores all tire pressures`() {
        val tpms = TpmsDetails(
            pressureFl = 2.8,
            pressureFr = 2.9,
            pressureRl = 2.7,
            pressureRr = 2.8
        )
        assertEquals(2.8, tpms.pressureFl)
        assertEquals(2.9, tpms.pressureFr)
        assertEquals(2.7, tpms.pressureRl)
        assertEquals(2.8, tpms.pressureRr)
    }

    @Test
    fun `TpmsDetails stores warnings`() {
        val tpms = TpmsDetails(
            warningFl = true,
            warningFr = false,
            warningRl = false,
            warningRr = true
        )
        assertTrue(tpms.warningFl == true)
        assertFalse(tpms.warningFr == true)
        assertFalse(tpms.warningRl == true)
        assertTrue(tpms.warningRr == true)
    }
}
