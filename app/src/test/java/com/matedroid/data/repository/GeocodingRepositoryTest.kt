package com.matedroid.data.repository

import com.matedroid.data.api.NominatimAddress
import com.matedroid.data.api.NominatimApi
import com.matedroid.data.api.NominatimResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Unit tests for GeocodingRepository.
 * Tests reverse geocoding with caching behavior.
 */
class GeocodingRepositoryTest {

    private lateinit var nominatimApi: NominatimApi
    private lateinit var repository: GeocodingRepository

    @Before
    fun setup() {
        nominatimApi = mockk()
        repository = GeocodingRepository(nominatimApi)
    }

    // === Basic Geocoding Tests ===

    @Test
    fun `reverseGeocode returns formatted address with road and city`() = runTest {
        val address = NominatimAddress(
            road = "Main Street",
            house_number = "123",
            city = "Berlin"
        )
        val response = NominatimResponse(
            displayName = "123, Main Street, Berlin, Germany",
            address = address
        )
        coEvery { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) } returns Response.success(response)

        val result = repository.reverseGeocode(52.5200, 13.4050)

        assertEquals("Main Street 123, Berlin", result)
    }

    @Test
    fun `reverseGeocode returns formatted address with road only`() = runTest {
        val address = NominatimAddress(
            road = "Highway A1",
            city = "Munich"
        )
        val response = NominatimResponse(
            displayName = "Highway A1, Munich, Germany",
            address = address
        )
        coEvery { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) } returns Response.success(response)

        val result = repository.reverseGeocode(48.1351, 11.5820)

        assertEquals("Highway A1, Munich", result)
    }

    @Test
    fun `reverseGeocode uses town when city is null`() = runTest {
        val address = NominatimAddress(
            road = "Village Road",
            town = "SmallTown"
        )
        val response = NominatimResponse(
            displayName = "Village Road, SmallTown",
            address = address
        )
        coEvery { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) } returns Response.success(response)

        val result = repository.reverseGeocode(50.0, 10.0)

        assertEquals("Village Road, SmallTown", result)
    }

    @Test
    fun `reverseGeocode uses village when city and town are null`() = runTest {
        val address = NominatimAddress(
            road = "Farm Lane",
            village = "Countryside"
        )
        val response = NominatimResponse(
            displayName = "Farm Lane, Countryside",
            address = address
        )
        coEvery { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) } returns Response.success(response)

        val result = repository.reverseGeocode(51.0, 11.0)

        assertEquals("Farm Lane, Countryside", result)
    }

    @Test
    fun `reverseGeocode uses municipality as last resort`() = runTest {
        val address = NominatimAddress(
            road = "Industrial Park",
            municipality = "RegionName"
        )
        val response = NominatimResponse(
            displayName = "Industrial Park, RegionName",
            address = address
        )
        coEvery { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) } returns Response.success(response)

        val result = repository.reverseGeocode(52.0, 12.0)

        assertEquals("Industrial Park, RegionName", result)
    }

    @Test
    fun `reverseGeocode falls back to displayName when no structured address`() = runTest {
        val response = NominatimResponse(
            displayName = "Some Location,Country,Continent",
            address = null
        )
        coEvery { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) } returns Response.success(response)

        val result = repository.reverseGeocode(53.0, 13.0)

        // Takes first 3 parts of displayName, joined with ", "
        assertEquals("Some Location, Country, Continent", result)
    }

    @Test
    fun `reverseGeocode truncates displayName to 3 parts`() = runTest {
        val response = NominatimResponse(
            displayName = "Street,District,City,State,Country",
            address = null
        )
        coEvery { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) } returns Response.success(response)

        val result = repository.reverseGeocode(54.0, 14.0)

        assertEquals("Street, District, City", result)
    }

    // === Caching Tests ===

    @Test
    fun `reverseGeocode caches results`() = runTest {
        val response = NominatimResponse(
            displayName = "Cached Location",
            address = NominatimAddress(road = "Cached Road", city = "CachedCity")
        )
        coEvery { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) } returns Response.success(response)

        // First call
        val result1 = repository.reverseGeocode(52.5200, 13.4050)
        // Second call with same coordinates
        val result2 = repository.reverseGeocode(52.5200, 13.4050)

        assertEquals(result1, result2)
        // API should only be called once
        coVerify(exactly = 1) { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `reverseGeocode caches based on rounded coordinates`() = runTest {
        val response = NominatimResponse(
            displayName = "Location",
            address = NominatimAddress(road = "Road", city = "City")
        )
        coEvery { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) } returns Response.success(response)

        // Coordinates that round to the same cache key (4 decimal places)
        // "%.4f".format(52.52001) = "52.5200" and "%.4f".format(52.52004) = "52.5200"
        val result1 = repository.reverseGeocode(52.52001, 13.40501)
        val result2 = repository.reverseGeocode(52.52004, 13.40504)

        assertEquals(result1, result2)
        coVerify(exactly = 1) { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `reverseGeocode makes new request for different coordinates`() = runTest {
        val response1 = NominatimResponse(
            displayName = "Location 1",
            address = NominatimAddress(road = "Road 1", city = "City 1")
        )
        val response2 = NominatimResponse(
            displayName = "Location 2",
            address = NominatimAddress(road = "Road 2", city = "City 2")
        )
        coEvery { nominatimApi.reverseGeocode(eq(52.52), any(), any(), any(), any()) } returns Response.success(response1)
        coEvery { nominatimApi.reverseGeocode(eq(48.14), any(), any(), any(), any()) } returns Response.success(response2)

        val result1 = repository.reverseGeocode(52.52, 13.40)
        val result2 = repository.reverseGeocode(48.14, 11.58)

        assertEquals("Road 1, City 1", result1)
        assertEquals("Road 2, City 2", result2)
        coVerify(exactly = 2) { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) }
    }

    // === Error Handling Tests ===

    @Test
    fun `reverseGeocode returns null on API failure`() = runTest {
        coEvery { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) } returns Response.error(
            404,
            okhttp3.ResponseBody.create(null, "Not found")
        )

        val result = repository.reverseGeocode(0.0, 0.0)

        assertNull(result)
    }

    @Test
    fun `reverseGeocode returns null on exception`() = runTest {
        coEvery { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) } throws RuntimeException("Network error")

        val result = repository.reverseGeocode(0.0, 0.0)

        assertNull(result)
    }

    @Test
    fun `reverseGeocode returns null when response body is null`() = runTest {
        coEvery { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) } returns Response.success(null)

        val result = repository.reverseGeocode(55.0, 15.0)

        assertNull(result)
    }

    // === Edge Cases ===

    @Test
    fun `reverseGeocode handles empty address fields`() = runTest {
        // When all address fields are null, formatAddress returns null
        // and we fall back to displayName
        val address = NominatimAddress(
            road = null,
            city = null
        )
        val response = NominatimResponse(
            displayName = "Fallback,Location,Here",
            address = address
        )
        coEvery { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) } returns Response.success(response)

        val result = repository.reverseGeocode(56.0, 16.0)

        // Falls back to displayName
        assertEquals("Fallback, Location, Here", result)
    }

    @Test
    fun `reverseGeocode handles address with only city`() = runTest {
        val address = NominatimAddress(
            city = "JustCity"
        )
        val response = NominatimResponse(
            displayName = "JustCity, Country",
            address = address
        )
        coEvery { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) } returns Response.success(response)

        val result = repository.reverseGeocode(57.0, 17.0)

        assertEquals("JustCity", result)
    }

    @Test
    fun `reverseGeocode handles negative coordinates`() = runTest {
        val response = NominatimResponse(
            displayName = "Southern Location",
            address = NominatimAddress(road = "Southern Road", city = "SouthCity")
        )
        coEvery { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) } returns Response.success(response)

        val result = repository.reverseGeocode(-33.8688, 151.2093) // Sydney

        assertEquals("Southern Road, SouthCity", result)
    }

    @Test
    fun `reverseGeocode handles coordinates at origin`() = runTest {
        val response = NominatimResponse(
            displayName = "Gulf of Guinea,Atlantic Ocean",
            address = null
        )
        coEvery { nominatimApi.reverseGeocode(any(), any(), any(), any(), any()) } returns Response.success(response)

        val result = repository.reverseGeocode(0.0, 0.0)

        assertEquals("Gulf of Guinea, Atlantic Ocean", result)
    }
}
