package mytracker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mytracker.domain.ExternalMediaReference
import mytracker.domain.MediaSearchResult
import mytracker.domain.MediaSearchRateLimitException
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets.UTF_8
import java.text.Normalizer
import java.time.Duration
import java.util.Locale

/** Small TMDB search client for the Windows/JVM application. */
internal class TmdbClient(
    private val accessToken: String?,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build(),
) {
    suspend fun searchDirect(query: String, category: String): List<MediaSearchResult> =
        search(query, category, useFallbacks = false)

    suspend fun searchFallbacks(query: String, category: String): List<MediaSearchResult> =
        search(query, category, useFallbacks = true)

    suspend fun loadProductionCountries(result: MediaSearchResult): List<String> = withContext(Dispatchers.IO) {
        val token = accessToken?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("TMDB_API_READ_ACCESS_TOKEN is not configured.")
        val mediaType = result.reference.mediaType
        require(mediaType == "movie" || mediaType == "tv") { "Unsupported TMDB media type: $mediaType" }
        val response = request(
            "https://api.themoviedb.org/3/$mediaType/${result.reference.id}?language=ru-RU",
            token,
        )
        parseProductionCountries(response)
    }

    internal fun parseProductionCountries(body: String): List<String> {
        val details = try {
            Json.parseToJsonElement(body).jsonObject
        } catch (exception: Exception) {
            throw IOException("TMDB returned details in an unexpected format.", exception)
        }
        val productionCountries = details["production_countries"]?.jsonArray.orEmpty()
            .mapNotNull { element ->
                (element as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull
                    ?.takeIf(String::isNotBlank)
            }
        return if (productionCountries.isNotEmpty()) {
            productionCountries
        } else {
            details["origin_country"]?.jsonArray.orEmpty()
                .mapNotNull { element -> element.jsonPrimitive.contentOrNull }
                .map { code -> Locale.forLanguageTag("und-$code").getDisplayCountry(Locale.forLanguageTag("ru")) }
                .filter(String::isNotBlank)
        }
    }

    private suspend fun search(
        query: String,
        category: String,
        useFallbacks: Boolean,
    ): List<MediaSearchResult> = withContext(Dispatchers.IO) {
        val token = accessToken?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException(
                "Не задан токен TMDB. Установите переменную TMDB_API_READ_ACCESS_TOKEN и перезапустите приложение.",
            )
        val normalizedQuery = query.trim().replace(Regex("\\s+"), " ")
        val types = when (category) {
            "Фильм" -> listOf("movie")
            "Сериал" -> listOf("tv")
            else -> listOf("movie", "tv")
        }

        val resultsPerType = if (types.size > 1) 10 else 20
        types.flatMap { type ->
            val results = if (useFallbacks) {
                searchTypeFallbacks(type, normalizedQuery, token)
            } else {
                searchTypeDirect(type, normalizedQuery, token)
            }
            results.take(resultsPerType)
        }
            .distinctBy { it.reference.id to it.reference.mediaType }
            .take(20)
    }

    private fun searchTypeDirect(type: String, query: String, token: String): List<MediaSearchResult> {
        val encodedQuery = URLEncoder.encode(query, UTF_8)
        return parseResults(requestSearch(type, encodedQuery, token), type)
    }

    private fun searchTypeFallbacks(type: String, query: String, token: String): List<MediaSearchResult> {
        for (candidate in queryFallbacks(query)) {
            val encodedQuery = URLEncoder.encode(candidate, UTF_8)
            val results = parseResults(requestSearch(type, encodedQuery, token), type)
            if (results.isNotEmpty()) return results
        }
        return emptyList()
    }

    internal fun queryFallbacks(query: String): List<String> = buildList {
        val normalized = query.trim().replace(Regex("\\s+"), " ")
        val seen = mutableSetOf(normalized)

        fun addCandidate(candidate: String) {
            val value = candidate.trim().replace(Regex("\\s+"), " ")
            if (value.isBlank()) return
            if (seen.add(value)) add(value)

            val yoPositions = value.indices.filter { value[it] in "еёЕЁ" }.take(6)
            yoPositions.forEach { index ->
                val chars = value.toCharArray()
                chars[index] = when (chars[index]) {
                    'е' -> 'ё'
                    'ё' -> 'е'
                    'Е' -> 'Ё'
                    else -> 'Е'
                }
                val singleLetterVariant = String(chars)
                if (seen.add(singleLetterVariant)) add(singleLetterVariant)
            }

            if (yoPositions.size > 1) {
                val allLettersVariant = buildString(value.length) {
                    value.forEach { char ->
                        append(
                            when (char) {
                                'е' -> 'ё'
                                'ё' -> 'е'
                                'Е' -> 'Ё'
                                'Ё' -> 'Е'
                                else -> char
                            },
                        )
                    }
                }
                if (seen.add(allLettersVariant)) add(allLettersVariant)
            }
        }

        addCandidate(normalized)

        val withoutDiacritics = Normalizer.normalize(normalized, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        if (withoutDiacritics != normalized) addCandidate(withoutDiacritics)

        switchKeyboardLayout(normalized)?.let(::addCandidate)
        transliterateLatinToCyrillic(normalized)?.let(::addCandidate)
    }

    private fun switchKeyboardLayout(query: String): String? {
        val cyrillicKeys = "йцукенгшщзхъфывапролджэячсмитьбю."
        val latinKeys = "qwertyuiop[]asdfghjkl;'zxcvbnm,./"
        val cyrillicCount = query.count { it in '\u0400'..'\u04FF' }
        val latinCount = query.count { it in 'a'..'z' || it in 'A'..'Z' }
        if (cyrillicCount == latinCount || cyrillicCount + latinCount == 0) return null

        val source = if (latinCount > cyrillicCount) latinKeys else cyrillicKeys
        val target = if (latinCount > cyrillicCount) cyrillicKeys else latinKeys
        val converted = buildString(query.length) {
            query.forEach { char ->
                val index = source.indexOf(char.lowercaseChar())
                val mapped = if (index >= 0) target[index] else char
                append(if (char.isUpperCase()) mapped.uppercaseChar() else mapped)
            }
        }
        return converted.takeIf { it != query }
    }

    private fun transliterateLatinToCyrillic(query: String): String? {
        if (query.none { it in 'a'..'z' || it in 'A'..'Z' }) return null
        val text = query.lowercase()
        val digraphs = listOf(
            "shch" to 'щ', "yo" to 'ё', "zh" to 'ж', "kh" to 'х',
            "ts" to 'ц', "ch" to 'ч', "sh" to 'ш', "yu" to 'ю', "ya" to 'я',
        )
        val letters = mapOf(
            'a' to "а", 'b' to "б", 'v' to "в", 'g' to "г", 'd' to "д", 'e' to "е",
            'z' to "з", 'i' to "и", 'j' to "й", 'k' to "к", 'l' to "л", 'm' to "м",
            'n' to "н", 'o' to "о", 'p' to "п", 'r' to "р", 's' to "с", 't' to "т",
            'u' to "у", 'f' to "ф", 'h' to "х", 'c' to "ц", 'y' to "ы", 'x' to "кс",
        )
        val result = StringBuilder(text.length)
        var index = 0
        while (index < text.length) {
            val digraph = digraphs.firstOrNull { (latin, _) -> text.startsWith(latin, index) }
            if (digraph != null) {
                result.append(digraph.second)
                index += digraph.first.length
                continue
            }
            val char = text[index]
            result.append(letters[char] ?: char)
            index++
        }
        return result.toString().takeIf { it != text }
    }

    private fun requestSearch(type: String, encodedQuery: String, token: String): String {
        val url = "https://api.themoviedb.org/3/search/$type?query=$encodedQuery&include_adult=false&language=ru-RU&page=1"
        return request(url, token)
    }

    private fun request(url: String, token: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(20))
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .GET()
            .build()

        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString(UTF_8))
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Поиск TMDB был прерван.", exception)
        } catch (exception: IOException) {
            throw IOException("Не удалось подключиться к TMDB: ${exception.message ?: "ошибка сети"}", exception)
        }

        if (response.statusCode() !in 200..299) {
            if (response.statusCode() == 429) {
                throw MediaSearchRateLimitException("TMDB временно ограничил число запросов. Попробуйте позже.")
            }
            val message = when (response.statusCode()) {
                401 -> "TMDB отклонил токен. Проверьте TMDB_API_READ_ACCESS_TOKEN."
                else -> "TMDB вернул ошибку ${response.statusCode()}."
            }
            throw IOException(message)
        }
        return response.body()
    }

    private fun parseResults(body: String, type: String): List<MediaSearchResult> {
        val results = try {
            Json.parseToJsonElement(body).jsonObject["results"]?.jsonArray.orEmpty()
        } catch (exception: Exception) {
            throw IOException("TMDB вернул данные в неожиданном формате.", exception)
        }

        return results.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val id = item.string("id") ?: return@mapNotNull null
            val title = item.string(if (type == "movie") "title" else "name")
                ?.takeIf(String::isNotBlank)
                ?: return@mapNotNull null
            val date = item.string(if (type == "movie") "release_date" else "first_air_date")
            MediaSearchResult(
                reference = ExternalMediaReference(provider = PROVIDER_ID, id = id, mediaType = type),
                title = title,
                releaseYear = date?.take(4)?.toIntOrNull(),
                overview = item.string("overview")?.takeIf(String::isNotBlank),
            )
        }
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    companion object {
        const val PROVIDER_ID = "tmdb"

        fun fromEnvironment(): TmdbClient = TmdbClient(
            accessToken = System.getenv("TMDB_API_READ_ACCESS_TOKEN"),
        )
    }
}
