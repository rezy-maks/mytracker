package mytracker

import mytracker.domain.MediaEntry
import mytracker.domain.ExternalMediaReference
import mytracker.domain.SeasonProgress
import mytracker.domain.ViewingNote
import mytracker.domain.WatchStatus
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/** Stores the complete catalog in the current user's application data directory. */
internal class CatalogStorage(
    internal val filePath: Path = Path.of(
        System.getProperty("user.home"),
        ".mytracker",
        "catalog.dat",
    ),
) {
    private companion object {
        const val FILE_VERSION = 8
        const val LEGACY_FILE_VERSION = 7
        const val MAX_ITEMS = 100_000
        const val MAX_STRING_BYTES = 10 * 1024 * 1024
    }

    fun load(): List<MediaEntry> {
        if (Files.notExists(filePath)) return emptyList()

        val (version, entries) = DataInputStream(Files.newInputStream(filePath).buffered()).use { input ->
            val version = input.readInt()
            check(version == FILE_VERSION || version == LEGACY_FILE_VERSION) {
                "Unsupported catalog file version $version; expected $FILE_VERSION"
            }
            val count = input.readCount()
            val entries = List(count) {
                MediaEntry(
                    id = input.readString(),
                    title = input.readString(),
                    categoryId = input.readString(),
                    externalReference = if (input.readBoolean()) {
                        ExternalMediaReference(
                            provider = input.readString(),
                            id = input.readString(),
                            mediaType = input.readString(),
                        )
                    } else {
                        null
                    },
                    posterUrl = input.readNullableString(),
                    releaseYear = input.readNullableInt(),
                    productionCountries = List(input.readCount()) { input.readString() },
                    status = WatchStatus.valueOf(input.readString()),
                    rating = input.readNullableInt(),
                    notes = input.readString(),
                    viewingNotes = List(input.readCount()) { input.readViewingNote() },
                    seasons = List(input.readCount()) {
                        val seasonNumber = input.readInt()
                        SeasonProgress(
                            seasonNumber = seasonNumber,
                            name = input.readNullableString(),
                            episodeCount = input.readNullableInt(),
                            firstAirDate = input.readNullableString(),
                            rating = if (version == FILE_VERSION) input.readNullableInt() else null,
                            notes = if (version == FILE_VERSION) input.readString() else {
                                List(input.readCount()) { input.readViewingNote() }
                                ""
                            },
                        )
                    },
                )
            }
            version to entries
        }
        if (version == LEGACY_FILE_VERSION) save(entries)
        return entries
    }

    fun save(entries: List<MediaEntry>) {
        Files.createDirectories(filePath.parent)
        val temporaryFile = filePath.resolveSibling("${filePath.fileName}.tmp")
        val backupFile = filePath.resolveSibling("${filePath.fileName}.bak")
        try {
            DataOutputStream(Files.newOutputStream(temporaryFile).buffered()).use { output ->
                output.writeInt(FILE_VERSION)
                output.writeCount(entries.size)
                entries.forEach { entry ->
                    output.writeString(entry.id)
                    output.writeString(entry.title)
                    output.writeString(entry.categoryId)
                    output.writeBoolean(entry.externalReference != null)
                    entry.externalReference?.let { reference ->
                        output.writeString(reference.provider)
                        output.writeString(reference.id)
                        output.writeString(reference.mediaType)
                    }
                    output.writeNullableString(entry.posterUrl)
                    output.writeNullableInt(entry.releaseYear)
                    output.writeCount(entry.productionCountries.size)
                    entry.productionCountries.forEach { country -> output.writeString(country) }
                    output.writeString(entry.status.name)
                    output.writeNullableInt(entry.rating)
                    output.writeString(entry.notes)
                    output.writeCount(entry.viewingNotes.size)
                    entry.viewingNotes.forEach { note -> output.writeViewingNote(note) }
                    output.writeCount(entry.seasons.size)
                    entry.seasons.forEach { season ->
                        output.writeInt(season.seasonNumber)
                        output.writeNullableString(season.name)
                        output.writeNullableInt(season.episodeCount)
                        output.writeNullableString(season.firstAirDate)
                        output.writeNullableInt(season.rating)
                        output.writeString(season.notes)
                    }
                }
            }
            try {
                Files.move(temporaryFile, filePath, ATOMIC_MOVE, REPLACE_EXISTING)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                val hadOriginal = Files.exists(filePath)
                if (hadOriginal) Files.copy(filePath, backupFile, REPLACE_EXISTING)
                try {
                    Files.move(temporaryFile, filePath, REPLACE_EXISTING)
                    Files.deleteIfExists(backupFile)
                } catch (failure: Throwable) {
                    if (hadOriginal && Files.exists(backupFile)) {
                        try {
                            Files.move(backupFile, filePath, REPLACE_EXISTING)
                        } catch (restoreFailure: Throwable) {
                            failure.addSuppressed(restoreFailure)
                        }
                    }
                    throw failure
                }
            }
        } finally {
            Files.deleteIfExists(temporaryFile)
        }
    }

    private fun DataInputStream.readCount(): Int = readInt().also {
        require(it in 0..MAX_ITEMS) { "Invalid item count in catalog file" }
    }

    private fun DataOutputStream.writeCount(count: Int) {
        require(count in 0..MAX_ITEMS) { "Too many items to save in catalog" }
        writeInt(count)
    }

    private fun DataInputStream.readString(): String {
        val size = readInt()
        require(size in 0..MAX_STRING_BYTES) { "Invalid string size in catalog file" }
        val bytes = ByteArray(size)
        readFully(bytes)
        return bytes.toString(UTF_8)
    }

    private fun DataOutputStream.writeString(value: String) {
        val bytes = value.toByteArray(UTF_8)
        require(bytes.size <= MAX_STRING_BYTES) { "Catalog text is too large to save" }
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readNullableInt(): Int? = if (readBoolean()) readInt() else null

    private fun DataInputStream.readNullableString(): String? = if (readBoolean()) readString() else null

    private fun DataOutputStream.writeNullableInt(value: Int?) {
        writeBoolean(value != null)
        if (value != null) writeInt(value)
    }

    private fun DataOutputStream.writeNullableString(value: String?) {
        writeBoolean(value != null)
        if (value != null) writeString(value)
    }

    private fun DataInputStream.readViewingNote(): ViewingNote {
        val date = readString()
        val text = readString()
        return ViewingNote(date = date, text = text)
    }

    private fun DataOutputStream.writeViewingNote(note: ViewingNote) {
        writeString(note.date)
        writeString(note.text)
    }
}
