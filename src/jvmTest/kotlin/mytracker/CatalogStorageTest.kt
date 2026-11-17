package mytracker

import java.io.DataOutputStream
import java.io.DataInputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import mytracker.domain.ExternalMediaReference
import mytracker.domain.MediaEntry
import mytracker.domain.SeasonProgress
import mytracker.domain.ViewingNote
import mytracker.domain.WatchStatus

class CatalogStorageTest {
    @Test
    fun loadReturnsEmptyListWhenFileDoesNotExist() = withStorage { storage, _ ->
        assertEquals(emptyList(), storage.load())
    }

    @Test
    fun saveAndLoadPreserveTheUnifiedCatalogFormat() = withStorage { storage, _ ->
        val entries = listOf(
            MediaEntry(
                id = "series",
                title = "Атака титанов",
                categoryId = "Аниме-сериал",
                externalReference = ExternalMediaReference("tmdb", "1429", "tv"),
                posterUrl = "https://image.tmdb.org/t/p/w342/poster.jpg",
                releaseYear = 2013,
                productionCountries = listOf("Япония"),
                status = WatchStatus.WATCHED,
                rating = 5,
                notes = "Рецензия на весь сериал",
                viewingNotes = listOf(ViewingNote("2026-11-17", "Пересмотреть финал")),
                seasons = listOf(
                    SeasonProgress(
                        seasonNumber = 1,
                        name = "Первый сезон",
                        episodeCount = 25,
                        firstAirDate = "2013-04-07",
                        rating = 4,
                        notes = "Рецензия на сезон",
                    ),
                    SeasonProgress(seasonNumber = 2),
                ),
            ),
            MediaEntry(
                id = "movie",
                title = "Фильм",
                categoryId = "Фильм",
            ),
        )

        storage.save(entries)

        assertEquals(entries, storage.load())
    }

    @Test
    fun saveReplacesPreviousCatalogAndCanSaveAnEmptyCatalog() = withStorage { storage, _ ->
        storage.save(listOf(MediaEntry(id = "old", title = "Старое", categoryId = "Фильм")))
        val updated = listOf(MediaEntry(id = "new", title = "Новое", categoryId = "Сериал"))
        storage.save(updated)

        assertEquals(updated, storage.load())

        storage.save(emptyList())
        assertEquals(emptyList(), storage.load())
    }

    @Test
    fun loadRejectsCatalogsOutsideTheSingleSupportedFormat() = withStorage { storage, path ->
        listOf(1, 2, 3, 4, 5, 6, 9, 999).forEach { version ->
            DataOutputStream(Files.newOutputStream(path)).use { output ->
                output.writeInt(version)
                output.writeInt(0)
            }
            val originalBytes = Files.readAllBytes(path)

            val error = assertFailsWith<IllegalStateException> { storage.load() }

            assertTrue(error.message.orEmpty().contains("expected 8"))
            assertEquals(originalBytes.toList(), Files.readAllBytes(path).toList())
        }
    }

    @Test
    fun loadMigratesVersionSevenCatalogAndRewritesItAsVersionEight() = withStorage { storage, path ->
        DataOutputStream(Files.newOutputStream(path)).use { output ->
            output.writeInt(7)
            output.writeInt(1)
            output.writeFixtureString("legacy-v7")
            output.writeFixtureString("Аниме-сериал")
            output.writeFixtureString("Аниме-сериал")
            output.writeBoolean(true)
            output.writeFixtureString("tmdb")
            output.writeFixtureString("123")
            output.writeFixtureString("tv")
            output.writeBoolean(true)
            output.writeFixtureString("https://image.tmdb.org/t/p/w342/poster.jpg")
            output.writeBoolean(true)
            output.writeInt(2024)
            output.writeInt(1)
            output.writeFixtureString("Япония")
            output.writeFixtureString(WatchStatus.WANT_TO_WATCH.name)
            output.writeBoolean(false)
            output.writeFixtureString("")
            output.writeInt(1)
            output.writeFixtureString("2026-11-17")
            output.writeFixtureString("Запись каталога сохранена")
            output.writeInt(1)
            output.writeInt(1)
            output.writeBoolean(true)
            output.writeFixtureString("Первый сезон")
            output.writeBoolean(true)
            output.writeInt(12)
            output.writeBoolean(true)
            output.writeFixtureString("2024-01-15")
            output.writeInt(1)
            output.writeFixtureString("2024-02-01")
            output.writeFixtureString("Старая заметка к сезону")
        }

        val loaded = storage.load()

        assertEquals(
            MediaEntry(
                id = "legacy-v7",
                title = "Аниме-сериал",
                categoryId = "Аниме-сериал",
                externalReference = ExternalMediaReference("tmdb", "123", "tv"),
                posterUrl = "https://image.tmdb.org/t/p/w342/poster.jpg",
                releaseYear = 2024,
                productionCountries = listOf("Япония"),
                status = WatchStatus.WANT_TO_WATCH,
                viewingNotes = listOf(ViewingNote("2026-11-17", "Запись каталога сохранена")),
                seasons = listOf(
                    SeasonProgress(
                        seasonNumber = 1,
                        name = "Первый сезон",
                        episodeCount = 12,
                        firstAirDate = "2024-01-15",
                    ),
                ),
            ),
            loaded.single(),
        )
        assertEquals(8, DataInputStream(Files.newInputStream(path)).use { it.readInt() })
    }

    @Test
    fun loadFailureLeavesOriginalFileUntouched() = withStorage { storage, path ->
        val originalBytes = byteArrayOf(0, 0, 0, 8, 0, 0, 0, 1, 1)
        Files.write(path, originalBytes)

        assertFailsWith<java.io.EOFException> { storage.load() }

        assertEquals(originalBytes.toList(), Files.readAllBytes(path).toList())
    }

    @Test
    fun saveFailureLeavesOriginalFileUntouchedAndRemovesTemporaryFile() = withStorage { storage, path ->
        val originalBytes = "keep this catalog".toByteArray(Charsets.UTF_8)
        Files.write(path, originalBytes)

        assertFailsWith<IllegalArgumentException> {
            storage.save(List(100_001) { MediaEntry(id = "id", title = "title", categoryId = "film") })
        }

        assertEquals(originalBytes.toList(), Files.readAllBytes(path).toList())
        assertEquals(false, Files.exists(path.resolveSibling("${path.fileName}.tmp")))
    }

    private fun withStorage(block: (CatalogStorage, java.nio.file.Path) -> Unit) {
        val directory = Files.createTempDirectory("mytracker-test-")
        val path = directory.resolve("catalog.dat")
        try {
            block(CatalogStorage(path), path)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private fun DataOutputStream.writeFixtureString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }
}
