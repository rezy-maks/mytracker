package mytracker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import java.io.DataOutputStream
import java.nio.file.Files
import mytracker.domain.MediaEntry
import mytracker.domain.ExternalMediaReference
import mytracker.domain.SeasonProgress
import mytracker.domain.ViewingNote
import mytracker.domain.WatchStatus

class CatalogStorageTest {
    @Test
    fun loadReturnsEmptyListWhenFileDoesNotExist() = withStorage { storage, _ ->
        assertEquals(emptyList(), storage.load())
    }

    @Test
    fun saveAndLoadPreserveEveryEntryFieldAndNestedValue() = withStorage { storage, _ ->
        val entries = listOf(
            MediaEntry(
                id = "entry-1",
                title = "Атака титанов",
                categoryId = "Аниме",
                externalReference = ExternalMediaReference("tmdb", "1429", "tv"),
                status = WatchStatus.WATCHED,
                rating = 5,
                notes = "Финал понравился",
                viewingNotes = listOf(
                    ViewingNote(date = "2026-11-17", text = "Пересмотреть финал"),
                    ViewingNote(date = "2026-11-18", text = "Короткая заметка"),
                ),
                seasons = listOf(
                    SeasonProgress(
                        seasonNumber = 4,
                        viewingNotes = listOf(
                            ViewingNote(date = "2026-11-19", text = "Последний сезон"),
                        ),
                    ),
                    SeasonProgress(seasonNumber = 1),
                ),
            ),
            MediaEntry(
                id = "entry-2",
                title = "A very long note",
                categoryId = "Фильм",
                notes = "заметка ".repeat(12_000),
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
    fun loadRejectsUnsupportedFileVersion() = withStorage { storage, path ->
        Files.createDirectories(path.parent)
        DataOutputStream(Files.newOutputStream(path)).use { it.writeInt(999) }

        assertFailsWith<IllegalStateException> { storage.load() }
    }

    @Test
    fun loadFailureLeavesOriginalFileUntouched() = withStorage { storage, path ->
        val originalBytes = byteArrayOf(0, 0, 0, 4, 0, 0, 0, 1, 1)
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

    @Test
    fun loadMigratesLegacyViewingNoteRatings() = withStorage { storage, path ->
        DataOutputStream(Files.newOutputStream(path)).use { output ->
            output.writeInt(1)
            output.writeInt(1)
            output.writeFixtureString("legacy")
            output.writeFixtureString("Старое произведение")
            output.writeFixtureString("Фильм")
            output.writeFixtureString(WatchStatus.WATCHED.name)
            output.writeBoolean(false) // No entry rating.
            output.writeFixtureString("")
            output.writeInt(1) // One viewing note.
            output.writeFixtureString("2026-11-17")
            output.writeFixtureString("Старая заметка")
            output.writeBoolean(true) // Legacy note rating, discarded during migration.
            output.writeInt(5)
            output.writeInt(0) // No seasons.
        }

        val migrated = listOf(
            MediaEntry(
                id = "legacy",
                title = "Старое произведение",
                categoryId = "Фильм",
                status = WatchStatus.WATCHED,
                viewingNotes = listOf(ViewingNote("2026-11-17", "Старая заметка")),
            ),
        )
        assertEquals(migrated, storage.load())

        storage.save(migrated)
        assertEquals(migrated, storage.load())
    }

    @Test
    fun loadMigratesLegacySeasonRatings() = withStorage { storage, path ->
        DataOutputStream(Files.newOutputStream(path)).use { output ->
            output.writeInt(2)
            output.writeInt(1)
            output.writeFixtureString("legacy-season")
            output.writeFixtureString("Сериал")
            output.writeFixtureString("Сериал")
            output.writeFixtureString(WatchStatus.WATCHED.name)
            output.writeBoolean(false) // No entry rating.
            output.writeFixtureString("")
            output.writeInt(0) // No entry notes.
            output.writeInt(1) // One season.
            output.writeInt(2)
            output.writeBoolean(true) // Legacy season rating, discarded during migration.
            output.writeInt(4)
            output.writeInt(0) // No season notes.
        }

        val migrated = listOf(
            MediaEntry(
                id = "legacy-season",
                title = "Сериал",
                categoryId = "Сериал",
                status = WatchStatus.WATCHED,
                seasons = listOf(SeasonProgress(seasonNumber = 2)),
            ),
        )
        assertEquals(migrated, storage.load())

        storage.save(migrated)
        assertEquals(migrated, storage.load())
    }

    @Test
    fun loadMigratesVersionThreeCatalogAndPreservesSeasonNotes() = withStorage { storage, path ->
        DataOutputStream(Files.newOutputStream(path)).use { output ->
            output.writeInt(3)
            output.writeInt(1)
            output.writeFixtureString("legacy-v3")
            output.writeFixtureString("Сериал третьей версии")
            output.writeFixtureString("Сериал")
            output.writeFixtureString(WatchStatus.WANT_TO_WATCH.name)
            output.writeBoolean(false)
            output.writeFixtureString("")
            output.writeInt(0) // No entry notes.
            output.writeInt(1) // One season without a rating field in v3.
            output.writeInt(2)
            output.writeInt(1) // One season note.
            output.writeFixtureString("2026-11-17")
            output.writeFixtureString("Заметка сезона")
        }

        val expected = listOf(
            MediaEntry(
                id = "legacy-v3",
                title = "Сериал третьей версии",
                categoryId = "Сериал",
                seasons = listOf(
                    SeasonProgress(
                        seasonNumber = 2,
                        viewingNotes = listOf(ViewingNote("2026-11-17", "Заметка сезона")),
                    ),
                ),
            ),
        )
        assertEquals(expected, storage.load())
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
