package mytracker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TmdbClientTest {
    private val client = TmdbClient(accessToken = null)

    @Test
    fun fallbackSearchIncludesYoVariantForCommonRussianSpelling() {
        val withYo = "\u0417\u0435\u043b\u0451\u043d\u0430\u044f \u043c\u0438\u043b\u044f"
        val withoutYo = "\u0417\u0435\u043b\u0435\u043d\u0430\u044f \u043c\u0438\u043b\u044f"
        assertTrue(withYo in client.queryFallbacks(withoutYo))
        assertTrue(withoutYo in client.queryFallbacks(withYo))
    }

    @Test
    fun fallbackSearchConvertsTextTypedWithWrongKeyboardLayout() {
        val cyrillicTitle = "\u0417\u0435\u043b\u0435\u043d\u0430\u044f \u043c\u0438\u043b\u044f"
        val russianLayoutEnglishTitle = "\u042b\u0435\u0444\u043a \u0426\u0444\u043a\u044b"
        assertTrue(cyrillicTitle in client.queryFallbacks("Ptktyfz vbkz"))
        assertTrue("Star Wars" in client.queryFallbacks(russianLayoutEnglishTitle))
    }

    @Test
    fun fallbackSearchTransliteratesRussianTitlesTypedInLatin() {
        val russianTitle = "\u0437\u0435\u043b\u0451\u043d\u0430\u044f \u043c\u0438\u043b\u044f"
        assertTrue(russianTitle in client.queryFallbacks("Zelenaya milya"))
    }

    @Test
    fun fallbackSearchRemovesLatinDiacritics() {
        assertTrue("Amelie" in client.queryFallbacks("Am\u00e9lie"))
    }

    @Test
    fun productionCountriesUsesTmdbNamesWhenAvailable() {
        val details = """{"production_countries":[{"iso_3166_1":"RU","name":"\u0420\u043e\u0441\u0441\u0438\u044f"}]}"""

        assertEquals(listOf("\u0420\u043e\u0441\u0441\u0438\u044f"), client.parseProductionCountries(details))
    }

    @Test
    fun productionCountriesTranslatesTvOriginCountryCodes() {
        val details = """{"origin_country":["JP"]}"""

        val country = client.parseProductionCountries(details).single()

        assertNotEquals("JP", country)
        assertTrue(country.isNotBlank())
    }
}
