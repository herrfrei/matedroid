package com.matedroid.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for BatteryTypeHelper.
 * Tests battery chemistry detection based on trim badging.
 */
class BatteryTypeHelperTest {

    // === getBatteryChemistry Tests ===

    @Test
    fun `getBatteryChemistry returns LFP for trim badging 50`() {
        val result = BatteryTypeHelper.getBatteryChemistry("50")
        assertEquals(BatteryTypeHelper.BatteryChemistry.LFP, result)
    }

    @Test
    fun `getBatteryChemistry returns NMC for trim badging 74`() {
        val result = BatteryTypeHelper.getBatteryChemistry("74")
        assertEquals(BatteryTypeHelper.BatteryChemistry.NMC, result)
    }

    @Test
    fun `getBatteryChemistry returns NMC for trim badging 74D`() {
        val result = BatteryTypeHelper.getBatteryChemistry("74D")
        assertEquals(BatteryTypeHelper.BatteryChemistry.NMC, result)
    }

    @Test
    fun `getBatteryChemistry returns NMC for trim badging P74D`() {
        val result = BatteryTypeHelper.getBatteryChemistry("P74D")
        assertEquals(BatteryTypeHelper.BatteryChemistry.NMC, result)
    }

    @Test
    fun `getBatteryChemistry handles lowercase trim badging`() {
        val result = BatteryTypeHelper.getBatteryChemistry("p74d")
        assertEquals(BatteryTypeHelper.BatteryChemistry.NMC, result)
    }

    @Test
    fun `getBatteryChemistry returns NMC for null trim badging`() {
        val result = BatteryTypeHelper.getBatteryChemistry(null)
        assertEquals(BatteryTypeHelper.BatteryChemistry.NMC, result)
    }

    @Test
    fun `getBatteryChemistry returns NMC for empty trim badging`() {
        val result = BatteryTypeHelper.getBatteryChemistry("")
        assertEquals(BatteryTypeHelper.BatteryChemistry.NMC, result)
    }

    @Test
    fun `getBatteryChemistry returns NMC for unknown trim badging`() {
        val result = BatteryTypeHelper.getBatteryChemistry("100D")
        assertEquals(BatteryTypeHelper.BatteryChemistry.NMC, result)
    }

    @Test
    fun `getBatteryChemistry strips P prefix correctly`() {
        // P50 would be Performance Standard Range (hypothetical)
        // The P is stripped, so "50" should still be detected as LFP
        val result = BatteryTypeHelper.getBatteryChemistry("P50")
        assertEquals(BatteryTypeHelper.BatteryChemistry.LFP, result)
    }

    // === getMaxDcPowerKw Tests ===

    @Test
    fun `getMaxDcPowerKw returns 170 for LFP battery`() {
        val result = BatteryTypeHelper.getMaxDcPowerKw("50")
        assertEquals(170, result)
    }

    @Test
    fun `getMaxDcPowerKw returns 250 for NMC battery`() {
        val result = BatteryTypeHelper.getMaxDcPowerKw("74")
        assertEquals(250, result)
    }

    @Test
    fun `getMaxDcPowerKw returns 250 for Long Range`() {
        val result = BatteryTypeHelper.getMaxDcPowerKw("74D")
        assertEquals(250, result)
    }

    @Test
    fun `getMaxDcPowerKw returns 250 for Performance`() {
        val result = BatteryTypeHelper.getMaxDcPowerKw("P74D")
        assertEquals(250, result)
    }

    @Test
    fun `getMaxDcPowerKw returns 250 for null trim`() {
        val result = BatteryTypeHelper.getMaxDcPowerKw(null)
        assertEquals(250, result)
    }

    // === isLfp Tests ===

    @Test
    fun `isLfp returns true for trim badging 50`() {
        assertTrue(BatteryTypeHelper.isLfp("50"))
    }

    @Test
    fun `isLfp returns false for trim badging 74`() {
        assertFalse(BatteryTypeHelper.isLfp("74"))
    }

    @Test
    fun `isLfp returns false for trim badging 74D`() {
        assertFalse(BatteryTypeHelper.isLfp("74D"))
    }

    @Test
    fun `isLfp returns false for trim badging P74D`() {
        assertFalse(BatteryTypeHelper.isLfp("P74D"))
    }

    @Test
    fun `isLfp returns false for null`() {
        assertFalse(BatteryTypeHelper.isLfp(null))
    }

    // === isNmc Tests ===

    @Test
    fun `isNmc returns false for trim badging 50`() {
        assertFalse(BatteryTypeHelper.isNmc("50"))
    }

    @Test
    fun `isNmc returns true for trim badging 74`() {
        assertTrue(BatteryTypeHelper.isNmc("74"))
    }

    @Test
    fun `isNmc returns true for trim badging 74D`() {
        assertTrue(BatteryTypeHelper.isNmc("74D"))
    }

    @Test
    fun `isNmc returns true for trim badging P74D`() {
        assertTrue(BatteryTypeHelper.isNmc("P74D"))
    }

    @Test
    fun `isNmc returns true for null`() {
        assertTrue(BatteryTypeHelper.isNmc(null))
    }

    // === Edge Cases ===

    @Test
    fun `getBatteryChemistry handles whitespace`() {
        // Trim badging with leading/trailing whitespace
        val result = BatteryTypeHelper.getBatteryChemistry(" 50 ")
        // After uppercase and trimStart('P'), " 50 " becomes " 50 " which doesn't match "50"
        // This is expected behavior - the API should provide clean data
        assertEquals(BatteryTypeHelper.BatteryChemistry.NMC, result)
    }

    @Test
    fun `getBatteryChemistry handles mixed case with P prefix`() {
        val result = BatteryTypeHelper.getBatteryChemistry("p74D")
        assertEquals(BatteryTypeHelper.BatteryChemistry.NMC, result)
    }

    @Test
    fun `isLfp and isNmc are mutually exclusive`() {
        // For any trim badging, exactly one should be true
        val trims = listOf("50", "74", "74D", "P74D", null, "")
        for (trim in trims) {
            val isLfp = BatteryTypeHelper.isLfp(trim)
            val isNmc = BatteryTypeHelper.isNmc(trim)
            assertTrue("For trim '$trim': isLfp=$isLfp, isNmc=$isNmc should be mutually exclusive", isLfp xor isNmc)
        }
    }

    @Test
    fun `getMaxDcPowerKw is consistent with getBatteryChemistry`() {
        // Verify that power ratings match chemistry
        val lfpTrims = listOf("50", "P50")
        val nmcTrims = listOf("74", "74D", "P74D", null)

        for (trim in lfpTrims) {
            val chemistry = BatteryTypeHelper.getBatteryChemistry(trim)
            val power = BatteryTypeHelper.getMaxDcPowerKw(trim)
            if (chemistry == BatteryTypeHelper.BatteryChemistry.LFP) {
                assertEquals("LFP battery should have 170 kW max power", 170, power)
            }
        }

        for (trim in nmcTrims) {
            val chemistry = BatteryTypeHelper.getBatteryChemistry(trim)
            val power = BatteryTypeHelper.getMaxDcPowerKw(trim)
            if (chemistry == BatteryTypeHelper.BatteryChemistry.NMC) {
                assertEquals("NMC battery should have 250 kW max power", 250, power)
            }
        }
    }
}
