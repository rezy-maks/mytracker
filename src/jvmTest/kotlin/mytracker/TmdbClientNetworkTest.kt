package mytracker

import kotlinx.coroutines.runBlocking
import mytracker.domain.MediaSearchRateLimitException
import java.io.IOException
import java.net.CookieHandler
import java.net.Authenticator
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Duration
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Flow
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TmdbClientNetworkTest {
    @Test
    fun rateLimitResponseBecomesDedicatedException() {
        val client = TmdbClient("test-token", StubHttpClient(statusCode = 429))

        val exception = assertFailsWith<MediaSearchRateLimitException> {
            runBlocking { client.searchDirect("Title", "Фильм") }
        }

        assertTrue(exception.message.orEmpty().contains("ограничил число запросов"))
    }

    @Test
    fun unauthorizedResponseExplainsInvalidToken() {
        val client = TmdbClient("test-token", StubHttpClient(statusCode = 401))

        val exception = assertFailsWith<IOException> {
            runBlocking { client.searchDirect("Title", "Фильм") }
        }

        assertTrue(exception.message.orEmpty().contains("отклонил токен"))
    }

    @Test
    fun otherHttpErrorsKeepTheirStatusCode() {
        val client = TmdbClient("test-token", StubHttpClient(statusCode = 503))

        val exception = assertFailsWith<IOException> {
            runBlocking { client.searchDirect("Title", "Фильм") }
        }

        assertTrue(exception.message.orEmpty().contains("503"))
    }

    @Test
    fun ioFailureIsWrappedWithConnectionContextAndOriginalCause() {
        val networkFailure = IOException("connection refused")
        val client = TmdbClient("test-token", StubHttpClient(failure = networkFailure))

        val exception = assertFailsWith<IOException> {
            runBlocking { client.searchDirect("Title", "Фильм") }
        }

        assertTrue(exception.message.orEmpty().contains("Не удалось подключиться к TMDB"))
        assertTrue(generateSequence(exception.cause) { it.cause }.any { it === networkFailure })
    }

    @Test
    fun missingTokenFailsClearlyWithoutMakingARequest() {
        val httpClient = StubHttpClient(statusCode = 200, body = """{"results":[]}""")
        val client = TmdbClient(null, httpClient)

        val exception = assertFailsWith<IllegalStateException> {
            runBlocking { client.searchDirect("Title", "Фильм") }
        }

        assertTrue(exception.message.orEmpty().contains("TMDB_API_READ_ACCESS_TOKEN"))
        assertEquals(0, httpClient.requestCount)
    }

    @Test
    fun unsupportedMediaTypeFailsBeforeMakingMetadataRequest() {
        val httpClient = StubHttpClient(statusCode = 200)
        val client = TmdbClient("test-token", httpClient)
        val result = mytracker.domain.MediaSearchResult(
            reference = mytracker.domain.ExternalMediaReference("tmdb", "1", "person"),
            title = "Unsupported",
            releaseYear = null,
            overview = null,
        )

        assertFailsWith<IllegalArgumentException> {
            runBlocking { client.loadMediaMetadata(result) }
        }
        assertEquals(0, httpClient.requestCount)
    }

    @Test
    fun successfulEmptySearchReturnsEmptyResultsForBothMediaTypes() {
        val httpClient = StubHttpClient(statusCode = 200, body = """{"results":[]}""")
        val client = TmdbClient("test-token", httpClient)

        val results = runBlocking { client.searchDirect("Title", "Фильм") }

        assertTrue(results.isEmpty())
        assertEquals(2, httpClient.requestCount)
        assertEquals(
            listOf("/3/search/movie", "/3/search/tv"),
            httpClient.requests.map { it.uri().path },
        )
        assertTrue(httpClient.requests.all { "include_adult=true" in it.uri().query.orEmpty() })
    }

    private class StubHttpClient(
        private val statusCode: Int = 200,
        private val body: String = """{"results":[]}""",
        private val failure: IOException? = null,
    ) : HttpClient() {
        val requests = mutableListOf<HttpRequest>()
        val requestCount: Int get() = requests.size

        override fun cookieHandler(): Optional<CookieHandler> = Optional.empty()
        override fun connectTimeout(): Optional<Duration> = Optional.empty()
        override fun followRedirects(): Redirect = Redirect.NEVER
        override fun proxy(): Optional<ProxySelector> = Optional.empty()
        override fun sslContext(): SSLContext = SSLContext.getDefault()
        override fun sslParameters(): SSLParameters = SSLParameters()
        override fun authenticator(): Optional<Authenticator> = Optional.empty()
        override fun version(): Version = Version.HTTP_1_1
        override fun executor(): Optional<Executor> = Optional.empty()

        override fun <T> send(
            request: HttpRequest,
            responseBodyHandler: HttpResponse.BodyHandler<T>,
        ): HttpResponse<T> {
            requests += request
            failure?.let { throw it }
            val responseInfo = object : HttpResponse.ResponseInfo {
                override fun statusCode(): Int = this@StubHttpClient.statusCode
                override fun headers(): HttpHeaders = HttpHeaders.of(emptyMap()) { _, _ -> true }
                override fun version(): Version = Version.HTTP_1_1
            }
            val subscriber = responseBodyHandler.apply(responseInfo)
            subscriber.onSubscribe(
                object : Flow.Subscription {
                    override fun request(n: Long) = Unit
                    override fun cancel() = Unit
                },
            )
            subscriber.onNext(listOf(ByteBuffer.wrap(body.toByteArray(UTF_8))))
            subscriber.onComplete()
            return StubHttpResponse(statusCode, request, subscriber.body.toCompletableFuture().join())
        }

        override fun <T> sendAsync(
            request: HttpRequest,
            responseBodyHandler: HttpResponse.BodyHandler<T>,
        ): CompletableFuture<HttpResponse<T>> = CompletableFuture.failedFuture(
            UnsupportedOperationException("Async requests are not used in this test."),
        )

        override fun <T> sendAsync(
            request: HttpRequest,
            responseBodyHandler: HttpResponse.BodyHandler<T>,
            pushPromiseHandler: HttpResponse.PushPromiseHandler<T>,
        ): CompletableFuture<HttpResponse<T>> = CompletableFuture.failedFuture(
            UnsupportedOperationException("Async requests are not used in this test."),
        )
    }

    private class StubHttpResponse<T>(
        private val responseStatus: Int,
        private val sentRequest: HttpRequest,
        private val responseBody: T,
    ) : HttpResponse<T> {
        override fun statusCode(): Int = responseStatus
        override fun request(): HttpRequest = sentRequest
        override fun previousResponse(): Optional<HttpResponse<T>> = Optional.empty()
        override fun headers(): HttpHeaders = HttpHeaders.of(emptyMap()) { _, _ -> true }
        override fun body(): T = responseBody
        override fun sslSession(): Optional<SSLSession> = Optional.empty()
        override fun uri(): URI = sentRequest.uri()
        override fun version(): HttpClient.Version = HttpClient.Version.HTTP_1_1
    }
}
