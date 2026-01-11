package com.matedroid.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for CarImageResolver.
 * Tests car image asset path resolution for all Tesla models and configurations.
 */
class CarImageResolverTest {

    // === Legacy Model 3 Tests ===

    @Test
    fun `getAssetPath returns correct path for legacy Model 3 with midnight silver`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "MidnightSilver",
            wheelType = "Pinwheel18CapKit"
        )
        assertEquals("car_images/m3_PMNG_W38B.png", result)
    }

    @Test
    fun `getAssetPath returns correct path for legacy Model 3 with pearl white`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "PearlWhite",
            wheelType = "AeroTurbine19"
        )
        assertEquals("car_images/m3_PPSW_W39B.png", result)
    }

    @Test
    fun `getAssetPath returns correct path for legacy Model 3 with red multicoat`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "RedMultiCoat",
            wheelType = "Performance20"
        )
        assertEquals("car_images/m3_PPMR_W32P.png", result)
    }

    @Test
    fun `getAssetPath returns correct path for legacy Model 3 with black`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "SolidBlack",
            wheelType = "Aero18"
        )
        assertEquals("car_images/m3_PBSB_W38B.png", result)
    }

    @Test
    fun `getAssetPath returns correct path for legacy Model 3 with deep blue`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "DeepBlue",
            wheelType = "Stiletto19"
        )
        assertEquals("car_images/m3_PPSB_W39B.png", result)
    }

    // === Highland Model 3 Tests ===

    @Test
    fun `getAssetPath returns Highland Model 3 path for quicksilver color`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "QuickSilver",
            wheelType = "Photon18"
        )
        assertEquals("car_images/m3h_PN00_W38A.png", result)
    }

    @Test
    fun `getAssetPath returns Highland Model 3 path for stealth grey color`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "StealthGrey",
            wheelType = "Nova18"
        )
        assertEquals("car_images/m3h_PN01_W38A.png", result)
    }

    @Test
    fun `getAssetPath returns Highland Model 3 path for ultra red color`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "UltraRed",
            wheelType = "Glider18"
        )
        assertEquals("car_images/m3h_PR01_W38A.png", result)
    }

    @Test
    fun `getAssetPath detects Highland by wheel type Photon18`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "PearlWhite",
            wheelType = "Photon18"
        )
        assertEquals("car_images/m3h_PPSW_W38A.png", result)
    }

    @Test
    fun `getAssetPath returns Highland Performance for P trim with highland features`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "QuickSilver",
            wheelType = "Performance20",
            trimBadging = "P74D"
        )
        assertEquals("car_images/m3hp_PN00_W30P.png", result)
    }

    // === Legacy Model Y Tests ===

    @Test
    fun `getAssetPath returns correct path for legacy Model Y with white`() {
        val result = CarImageResolver.getAssetPath(
            model = "Y",
            exteriorColor = "PearlWhite",
            wheelType = "Gemini19"
        )
        assertEquals("car_images/my_PPSW_WY19B.png", result)
    }

    @Test
    fun `getAssetPath returns correct path for legacy Model Y with midnight silver`() {
        val result = CarImageResolver.getAssetPath(
            model = "Y",
            exteriorColor = "MidnightSilver",
            wheelType = "Induction20"
        )
        assertEquals("car_images/my_PMNG_WY0S.png", result)
    }

    @Test
    fun `getAssetPath returns correct path for Model Y with uberturbine wheels`() {
        // Note: Uberturbine21 wheels trigger Juniper Performance detection in the resolver
        // because 21" Uberturbine wheels are only available on Juniper Performance (myjp)
        // The color falls back to PPSW since PBSB is not available on Juniper Performance
        val result = CarImageResolver.getAssetPath(
            model = "Y",
            exteriorColor = "Black",
            wheelType = "Uberturbine21"
        )
        assertEquals("car_images/myjp_PPSW_WY21A.png", result)
    }

    // === Juniper Model Y Tests ===

    @Test
    fun `getAssetPath returns Juniper Model Y path for quicksilver color`() {
        val result = CarImageResolver.getAssetPath(
            model = "Y",
            exteriorColor = "QuickSilver",
            wheelType = "Photon18"
        )
        assertEquals("car_images/myj_PN00_WY18P.png", result)
    }

    @Test
    fun `getAssetPath returns Juniper Model Y path for stealth grey`() {
        val result = CarImageResolver.getAssetPath(
            model = "Y",
            exteriorColor = "StealthGrey",
            wheelType = "Crossflow19"
        )
        assertEquals("car_images/myj_PN01_WY19P.png", result)
    }

    @Test
    fun `getAssetPath detects Juniper by wheel type Helix20`() {
        val result = CarImageResolver.getAssetPath(
            model = "Y",
            exteriorColor = "PearlWhite",
            wheelType = "Helix20"
        )
        assertEquals("car_images/myj_PPSW_WY20A.png", result)
    }

    @Test
    fun `getAssetPath returns Juniper Performance for P74D trim`() {
        val result = CarImageResolver.getAssetPath(
            model = "Y",
            exteriorColor = "UltraRed",
            wheelType = "Uberturbine21",
            trimBadging = "P74D"
        )
        assertEquals("car_images/myjp_PR01_WY21A.png", result)
    }

    @Test
    fun `getAssetPath returns Juniper Performance for 21 inch wheels`() {
        val result = CarImageResolver.getAssetPath(
            model = "Y",
            exteriorColor = "StealthGrey",
            wheelType = "21"
        )
        assertEquals("car_images/myjp_PN01_WY21A.png", result)
    }

    // === Model S Tests ===

    @Test
    fun `getAssetPath returns correct path for Model S`() {
        val result = CarImageResolver.getAssetPath(
            model = "S",
            exteriorColor = "PearlWhite",
            wheelType = "Tempest19"
        )
        assertEquals("car_images/ms_PPSW_WT19.png", result)
    }

    @Test
    fun `getAssetPath returns correct path for Model S with midnight silver`() {
        val result = CarImageResolver.getAssetPath(
            model = "S",
            exteriorColor = "MidnightSilver",
            wheelType = "19"
        )
        assertEquals("car_images/ms_PMNG_WT19.png", result)
    }

    // === Model X Tests ===

    @Test
    fun `getAssetPath returns correct path for Model X`() {
        val result = CarImageResolver.getAssetPath(
            model = "X",
            exteriorColor = "PearlWhite",
            wheelType = "Cyberstream20"
        )
        assertEquals("car_images/mx_PPSW_WX20.png", result)
    }

    @Test
    fun `getAssetPath returns correct path for Model X with red`() {
        val result = CarImageResolver.getAssetPath(
            model = "X",
            exteriorColor = "Red",
            wheelType = "20"
        )
        assertEquals("car_images/mx_PPMR_WX20.png", result)
    }

    // === Default/Fallback Tests ===

    @Test
    fun `getAssetPath returns default for null model`() {
        val result = CarImageResolver.getAssetPath(
            model = null,
            exteriorColor = "PearlWhite",
            wheelType = "Pinwheel18"
        )
        assertEquals("car_images/m3_PPSW_W38B.png", result)
    }

    @Test
    fun `getAssetPath returns default for null color`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = null,
            wheelType = "Pinwheel18"
        )
        assertEquals("car_images/m3_PPSW_W38B.png", result)
    }

    @Test
    fun `getAssetPath returns default for null wheel`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "PearlWhite",
            wheelType = null
        )
        assertEquals("car_images/m3_PPSW_W38B.png", result)
    }

    @Test
    fun `getAssetPath returns default for all nulls`() {
        val result = CarImageResolver.getAssetPath(
            model = null,
            exteriorColor = null,
            wheelType = null
        )
        assertEquals("car_images/m3_PPSW_W38B.png", result)
    }

    @Test
    fun `getAssetPath handles unknown color gracefully`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "UnknownColor",
            wheelType = "Pinwheel18"
        )
        // Should fallback to default color
        assertEquals("car_images/m3_PPSW_W38B.png", result)
    }

    @Test
    fun `getAssetPath handles unknown wheel gracefully`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "PearlWhite",
            wheelType = "UnknownWheel"
        )
        // Should fallback to default wheel
        assertEquals("car_images/m3_PPSW_W38B.png", result)
    }

    // === getDefaultAssetPath Tests ===

    @Test
    fun `getDefaultAssetPath returns Model 3 default`() {
        val result = CarImageResolver.getDefaultAssetPath("3")
        assertEquals("car_images/m3_PPSW_W38B.png", result)
    }

    @Test
    fun `getDefaultAssetPath returns Model Y default`() {
        val result = CarImageResolver.getDefaultAssetPath("Y")
        assertEquals("car_images/my_PPSW_WY19B.png", result)
    }

    @Test
    fun `getDefaultAssetPath returns Model S default`() {
        val result = CarImageResolver.getDefaultAssetPath("S")
        assertEquals("car_images/ms_PPSW_WT19.png", result)
    }

    @Test
    fun `getDefaultAssetPath returns Model X default`() {
        val result = CarImageResolver.getDefaultAssetPath("X")
        assertEquals("car_images/mx_PPSW_WX20.png", result)
    }

    @Test
    fun `getDefaultAssetPath returns Model 3 for unknown model`() {
        val result = CarImageResolver.getDefaultAssetPath("Unknown")
        assertEquals("car_images/m3_PPSW_W38B.png", result)
    }

    @Test
    fun `getDefaultAssetPath returns Model 3 for null model`() {
        val result = CarImageResolver.getDefaultAssetPath(null)
        assertEquals("car_images/m3_PPSW_W38B.png", result)
    }

    // === getScaleFactor Tests ===

    @Test
    fun `getScaleFactor returns 1_0 for legacy Model 3`() {
        val result = CarImageResolver.getScaleFactor(
            model = "3",
            exteriorColor = "MidnightSilver",
            wheelType = "Pinwheel18"
        )
        assertEquals(1.0f, result)
    }

    @Test
    fun `getScaleFactor returns 1_35 for Highland Model 3`() {
        val result = CarImageResolver.getScaleFactor(
            model = "3",
            exteriorColor = "QuickSilver",
            wheelType = "Photon18"
        )
        assertEquals(1.35f, result)
    }

    @Test
    fun `getScaleFactor returns 1_35 for Highland Model 3 Performance`() {
        val result = CarImageResolver.getScaleFactor(
            model = "3",
            exteriorColor = "UltraRed",
            wheelType = "Performance20",
            trimBadging = "P74D"
        )
        assertEquals(1.35f, result)
    }

    @Test
    fun `getScaleFactor returns 1_0 for legacy Model Y`() {
        val result = CarImageResolver.getScaleFactor(
            model = "Y",
            exteriorColor = "MidnightSilver",
            wheelType = "Gemini19"
        )
        assertEquals(1.0f, result)
    }

    @Test
    fun `getScaleFactor returns 1_25 for Juniper Model Y`() {
        val result = CarImageResolver.getScaleFactor(
            model = "Y",
            exteriorColor = "QuickSilver",
            wheelType = "Crossflow19"
        )
        assertEquals(1.25f, result)
    }

    @Test
    fun `getScaleFactor returns 1_25 for Juniper Model Y Performance`() {
        val result = CarImageResolver.getScaleFactor(
            model = "Y",
            exteriorColor = "StealthGrey",
            wheelType = "Uberturbine21",
            trimBadging = "P74D"
        )
        assertEquals(1.25f, result)
    }

    @Test
    fun `getScaleFactor returns 1_4 for Model X`() {
        val result = CarImageResolver.getScaleFactor(
            model = "X",
            exteriorColor = "PearlWhite",
            wheelType = "20"
        )
        assertEquals(1.4f, result)
    }

    @Test
    fun `getScaleFactor returns 1_0 for Model S`() {
        val result = CarImageResolver.getScaleFactor(
            model = "S",
            exteriorColor = "PearlWhite",
            wheelType = "19"
        )
        assertEquals(1.0f, result)
    }

    // === getScaleFactorForVariant Tests ===

    @Test
    fun `getScaleFactorForVariant returns correct values for all variants`() {
        assertEquals(1.0f, CarImageResolver.getScaleFactorForVariant("m3"))
        assertEquals(1.35f, CarImageResolver.getScaleFactorForVariant("m3h"))
        assertEquals(1.35f, CarImageResolver.getScaleFactorForVariant("m3hp"))
        assertEquals(1.0f, CarImageResolver.getScaleFactorForVariant("my"))
        assertEquals(1.25f, CarImageResolver.getScaleFactorForVariant("myj"))
        assertEquals(1.25f, CarImageResolver.getScaleFactorForVariant("myjp"))
        assertEquals(1.0f, CarImageResolver.getScaleFactorForVariant("ms"))
        assertEquals(1.4f, CarImageResolver.getScaleFactorForVariant("mx"))
    }

    @Test
    fun `getScaleFactorForVariant returns 1_0 for unknown variant`() {
        assertEquals(1.0f, CarImageResolver.getScaleFactorForVariant("unknown"))
    }

    // === getFallbackAssetPath Tests ===

    @Test
    fun `getFallbackAssetPath returns exact match when asset exists`() {
        val result = CarImageResolver.getFallbackAssetPath(
            model = "3",
            exteriorColor = "MidnightSilver",
            wheelType = "Pinwheel18"
        ) { path ->
            path == "car_images/m3_PMNG_W38B.png"
        }
        assertEquals("car_images/m3_PMNG_W38B.png", result)
    }

    @Test
    fun `getFallbackAssetPath falls back to default wheel when exact not found`() {
        val result = CarImageResolver.getFallbackAssetPath(
            model = "3",
            exteriorColor = "MidnightSilver",
            wheelType = "UnknownWheel"
        ) { path ->
            path == "car_images/m3_PMNG_W38B.png"
        }
        assertEquals("car_images/m3_PMNG_W38B.png", result)
    }

    @Test
    fun `getFallbackAssetPath falls back to default asset when nothing matches`() {
        val result = CarImageResolver.getFallbackAssetPath(
            model = "3",
            exteriorColor = "UnknownColor",
            wheelType = "UnknownWheel"
        ) { path ->
            path == "car_images/m3_PPSW_W38B.png"
        }
        assertEquals("car_images/m3_PPSW_W38B.png", result)
    }

    // === Color Normalization Tests ===

    @Test
    fun `getAssetPath normalizes color with spaces`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "Midnight Silver Metallic",
            wheelType = "Pinwheel18"
        )
        assertEquals("car_images/m3_PMNG_W38B.png", result)
    }

    @Test
    fun `getAssetPath normalizes color with dashes`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "pearl-white-multi-coat",
            wheelType = "Pinwheel18"
        )
        assertEquals("car_images/m3_PPSW_W38B.png", result)
    }

    @Test
    fun `getAssetPath normalizes color with underscores`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "deep_blue_metallic",
            wheelType = "Pinwheel18"
        )
        assertEquals("car_images/m3_PPSB_W38B.png", result)
    }

    @Test
    fun `getAssetPath handles case insensitive colors`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "MIDNIGHT SILVER",
            wheelType = "Pinwheel18"
        )
        assertEquals("car_images/m3_PMNG_W38B.png", result)
    }

    // === Wheel Normalization Tests ===

    @Test
    fun `getAssetPath normalizes wheel with spaces`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "PearlWhite",
            wheelType = "Pinwheel 18 Cap Kit"
        )
        assertEquals("car_images/m3_PPSW_W38B.png", result)
    }

    @Test
    fun `getAssetPath normalizes wheel with dashes`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "PearlWhite",
            wheelType = "Aero-Turbine-19"
        )
        assertEquals("car_images/m3_PPSW_W39B.png", result)
    }

    // === Color Validation Tests ===

    @Test
    fun `getAssetPath validates color for legacy Model 3`() {
        // Stealth Grey is only available on Highland, not legacy
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "MidnightSilver", // Legacy color
            wheelType = "Pinwheel18" // Legacy wheel
        )
        assertTrue(result.contains("m3_"))
        assertTrue(result.contains("PMNG"))
    }

    @Test
    fun `getAssetPath validates color for Highland Model 3`() {
        // Midnight Silver is not available on Highland
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "MidnightSilver",
            wheelType = "Photon18" // Highland wheel
        )
        // Should fall back to default color for Highland
        assertTrue(result.contains("m3h_"))
    }

    // === Model Uppercase Handling ===

    @Test
    fun `getAssetPath handles lowercase model`() {
        val result = CarImageResolver.getAssetPath(
            model = "y",
            exteriorColor = "PearlWhite",
            wheelType = "Gemini19"
        )
        assertEquals("car_images/my_PPSW_WY19B.png", result)
    }

    // === Performance Trim Detection ===

    @Test
    fun `getAssetPath detects Performance trim from P prefix`() {
        val result = CarImageResolver.getAssetPath(
            model = "3",
            exteriorColor = "QuickSilver",
            wheelType = "Performance20",
            trimBadging = "P74D"
        )
        assertTrue(result.contains("m3hp"))
    }

    @Test
    fun `getAssetPath detects Performance trim from performance keyword`() {
        val result = CarImageResolver.getAssetPath(
            model = "Y",
            exteriorColor = "UltraRed",
            wheelType = "Uberturbine21",
            trimBadging = "performance"
        )
        assertTrue(result.contains("myjp"))
    }

    // === All Asset Paths Should Be Valid Format ===

    @Test
    fun `all asset paths follow expected format`() {
        val models = listOf("3", "Y", "S", "X", null)
        val colors = listOf("PearlWhite", "MidnightSilver", "QuickSilver", null)
        val wheels = listOf("Pinwheel18", "Gemini19", "Photon18", null)
        val trims = listOf("74D", "P74D", null)

        for (model in models) {
            for (color in colors) {
                for (wheel in wheels) {
                    for (trim in trims) {
                        val path = CarImageResolver.getAssetPath(model, color, wheel, trim)
                        assertTrue(
                            "Path $path should start with 'car_images/'",
                            path.startsWith("car_images/")
                        )
                        assertTrue(
                            "Path $path should end with '.png'",
                            path.endsWith(".png")
                        )
                        assertTrue(
                            "Path $path should contain model variant",
                            path.contains(Regex("m3|m3h|m3hp|my|myj|myjp|ms|mx"))
                        )
                    }
                }
            }
        }
    }
}
