package com.matedroid.domain.model

import com.matedroid.data.api.models.Units
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for UnitFormatter.
 * Tests all conversion functions for both metric and imperial units.
 */
class UnitFormatterTest {

    // === Distance Tests ===

    @Test
    fun `formatDistance returns km for metric units`() {
        val units = Units(unitOfLength = "km")
        val result = UnitFormatter.formatDistance(100.0, units)
        assertEquals("100.0 km", result)
    }

    @Test
    fun `formatDistance returns miles for imperial units`() {
        val units = Units(unitOfLength = "mi")
        val result = UnitFormatter.formatDistance(100.0, units)
        assertEquals("62.1 mi", result)
    }

    @Test
    fun `formatDistance returns km for null units`() {
        val result = UnitFormatter.formatDistance(100.0, null)
        assertEquals("100.0 km", result)
    }

    @Test
    fun `formatDistance handles zero value`() {
        val units = Units(unitOfLength = "km")
        val result = UnitFormatter.formatDistance(0.0, units)
        assertEquals("0.0 km", result)
    }

    @Test
    fun `formatDistance respects decimal parameter`() {
        val units = Units(unitOfLength = "km")
        val result = UnitFormatter.formatDistance(123.456, units, decimals = 2)
        assertEquals("123.46 km", result)
    }

    @Test
    fun `formatDistanceValue returns converted value for imperial`() {
        val units = Units(unitOfLength = "mi")
        val result = UnitFormatter.formatDistanceValue(100.0, units)
        assertEquals(62.1371, result, 0.001)
    }

    @Test
    fun `formatDistanceValue returns same value for metric`() {
        val units = Units(unitOfLength = "km")
        val result = UnitFormatter.formatDistanceValue(100.0, units)
        assertEquals(100.0, result, 0.001)
    }

    @Test
    fun `getDistanceUnit returns mi for imperial`() {
        val units = Units(unitOfLength = "mi")
        assertEquals("mi", UnitFormatter.getDistanceUnit(units))
    }

    @Test
    fun `getDistanceUnit returns km for metric`() {
        val units = Units(unitOfLength = "km")
        assertEquals("km", UnitFormatter.getDistanceUnit(units))
    }

    @Test
    fun `getDistanceUnit returns km for null units`() {
        assertEquals("km", UnitFormatter.getDistanceUnit(null))
    }

    // === Temperature Tests ===

    @Test
    fun `formatTemperature returns celsius for C units`() {
        val units = Units(unitOfTemperature = "C")
        val result = UnitFormatter.formatTemperature(20.0, units)
        assertEquals("20°C", result)
    }

    @Test
    fun `formatTemperature returns fahrenheit for F units`() {
        val units = Units(unitOfTemperature = "F")
        val result = UnitFormatter.formatTemperature(20.0, units)
        assertEquals("68°F", result)
    }

    @Test
    fun `formatTemperature returns celsius for null units`() {
        val result = UnitFormatter.formatTemperature(20.0, null)
        assertEquals("20°C", result)
    }

    @Test
    fun `formatTemperature handles negative temperatures`() {
        val units = Units(unitOfTemperature = "C")
        val result = UnitFormatter.formatTemperature(-10.0, units)
        assertEquals("-10°C", result)
    }

    @Test
    fun `formatTemperature converts negative celsius to fahrenheit`() {
        val units = Units(unitOfTemperature = "F")
        val result = UnitFormatter.formatTemperature(-10.0, units)
        assertEquals("14°F", result)
    }

    @Test
    fun `formatTemperature freezing point conversion`() {
        val units = Units(unitOfTemperature = "F")
        val result = UnitFormatter.formatTemperature(0.0, units)
        assertEquals("32°F", result)
    }

    @Test
    fun `formatTemperatureValue returns fahrenheit value for F units`() {
        val units = Units(unitOfTemperature = "F")
        val result = UnitFormatter.formatTemperatureValue(100.0, units)
        assertEquals(212.0, result, 0.001)
    }

    @Test
    fun `formatTemperatureValue returns same value for C units`() {
        val units = Units(unitOfTemperature = "C")
        val result = UnitFormatter.formatTemperatureValue(100.0, units)
        assertEquals(100.0, result, 0.001)
    }

    @Test
    fun `getTemperatureUnit returns F for fahrenheit`() {
        val units = Units(unitOfTemperature = "F")
        assertEquals("°F", UnitFormatter.getTemperatureUnit(units))
    }

    @Test
    fun `getTemperatureUnit returns C for celsius`() {
        val units = Units(unitOfTemperature = "C")
        assertEquals("°C", UnitFormatter.getTemperatureUnit(units))
    }

    @Test
    fun `getTemperatureUnit returns C for null units`() {
        assertEquals("°C", UnitFormatter.getTemperatureUnit(null))
    }

    // === Pressure Tests ===

    @Test
    fun `formatPressure returns bar for bar units`() {
        val units = Units(unitOfPressure = "bar")
        val result = UnitFormatter.formatPressure(2.5, units)
        assertEquals("2.5 bar", result)
    }

    @Test
    fun `formatPressure returns psi for psi units`() {
        val units = Units(unitOfPressure = "psi")
        val result = UnitFormatter.formatPressure(2.5, units)
        assertEquals("36.3 psi", result)
    }

    @Test
    fun `formatPressure returns bar for null units`() {
        val result = UnitFormatter.formatPressure(2.5, null)
        assertEquals("2.5 bar", result)
    }

    @Test
    fun `formatPressure handles typical tire pressure`() {
        val units = Units(unitOfPressure = "psi")
        // Typical tire pressure is around 2.4-2.8 bar
        val result = UnitFormatter.formatPressure(2.8, units)
        assertEquals("40.6 psi", result)
    }

    @Test
    fun `getPressureUnit returns psi for psi units`() {
        val units = Units(unitOfPressure = "psi")
        assertEquals("psi", UnitFormatter.getPressureUnit(units))
    }

    @Test
    fun `getPressureUnit returns bar for bar units`() {
        val units = Units(unitOfPressure = "bar")
        assertEquals("bar", UnitFormatter.getPressureUnit(units))
    }

    @Test
    fun `getPressureUnit returns bar for null units`() {
        assertEquals("bar", UnitFormatter.getPressureUnit(null))
    }

    // === Efficiency Tests ===

    @Test
    fun `formatEfficiency returns Wh per km for metric`() {
        val units = Units(unitOfLength = "km")
        val result = UnitFormatter.formatEfficiency(150.0, units)
        assertEquals("150.0 Wh/km", result)
    }

    @Test
    fun `formatEfficiency returns Wh per mile for imperial`() {
        val units = Units(unitOfLength = "mi")
        val result = UnitFormatter.formatEfficiency(150.0, units)
        assertEquals("241.4 Wh/mi", result)
    }

    @Test
    fun `formatEfficiency returns Wh per km for null units`() {
        val result = UnitFormatter.formatEfficiency(150.0, null)
        assertEquals("150.0 Wh/km", result)
    }

    @Test
    fun `getEfficiencyUnit returns Wh per mile for imperial`() {
        val units = Units(unitOfLength = "mi")
        assertEquals("Wh/mi", UnitFormatter.getEfficiencyUnit(units))
    }

    @Test
    fun `getEfficiencyUnit returns Wh per km for metric`() {
        val units = Units(unitOfLength = "km")
        assertEquals("Wh/km", UnitFormatter.getEfficiencyUnit(units))
    }

    @Test
    fun `getEfficiencyUnit returns Wh per km for null units`() {
        assertEquals("Wh/km", UnitFormatter.getEfficiencyUnit(null))
    }

    // === Speed Tests ===

    @Test
    fun `formatSpeed returns km per h for metric`() {
        val units = Units(unitOfLength = "km")
        val result = UnitFormatter.formatSpeed(100.0, units)
        assertEquals("100 km/h", result)
    }

    @Test
    fun `formatSpeed returns mph for imperial`() {
        val units = Units(unitOfLength = "mi")
        val result = UnitFormatter.formatSpeed(100.0, units)
        assertEquals("62 mph", result)
    }

    @Test
    fun `formatSpeed returns km per h for null units`() {
        val result = UnitFormatter.formatSpeed(100.0, null)
        assertEquals("100 km/h", result)
    }

    @Test
    fun `formatSpeed handles highway speed`() {
        val units = Units(unitOfLength = "km")
        val result = UnitFormatter.formatSpeed(130.0, units)
        assertEquals("130 km/h", result)
    }

    @Test
    fun `formatSpeed handles zero`() {
        val units = Units(unitOfLength = "km")
        val result = UnitFormatter.formatSpeed(0.0, units)
        assertEquals("0 km/h", result)
    }

    @Test
    fun `getSpeedUnit returns mph for imperial`() {
        val units = Units(unitOfLength = "mi")
        assertEquals("mph", UnitFormatter.getSpeedUnit(units))
    }

    @Test
    fun `getSpeedUnit returns km per h for metric`() {
        val units = Units(unitOfLength = "km")
        assertEquals("km/h", UnitFormatter.getSpeedUnit(units))
    }

    @Test
    fun `getSpeedUnit returns km per h for null units`() {
        assertEquals("km/h", UnitFormatter.getSpeedUnit(null))
    }

    // === Edge Cases ===

    @Test
    fun `formatDistance handles very large distances`() {
        val units = Units(unitOfLength = "km")
        val result = UnitFormatter.formatDistance(100000.0, units)
        assertEquals("100000.0 km", result)
    }

    @Test
    fun `formatDistance handles very small distances`() {
        val units = Units(unitOfLength = "km")
        val result = UnitFormatter.formatDistance(0.001, units, decimals = 3)
        assertEquals("0.001 km", result)
    }

    @Test
    fun `formatTemperature handles extreme heat`() {
        val units = Units(unitOfTemperature = "C")
        val result = UnitFormatter.formatTemperature(50.0, units)
        assertEquals("50°C", result)
    }

    @Test
    fun `formatTemperature handles extreme cold`() {
        val units = Units(unitOfTemperature = "C")
        val result = UnitFormatter.formatTemperature(-40.0, units)
        assertEquals("-40°C", result)
    }

    @Test
    fun `formatTemperature minus 40 is same in C and F`() {
        // -40°C = -40°F is the point where both scales intersect
        val unitsF = Units(unitOfTemperature = "F")
        val result = UnitFormatter.formatTemperature(-40.0, unitsF)
        assertEquals("-40°F", result)
    }
}
