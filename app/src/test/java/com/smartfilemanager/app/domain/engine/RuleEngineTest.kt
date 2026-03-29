package com.smartfilemanager.app.domain.engine

import android.net.Uri
import com.smartfilemanager.app.data.entity.ConditionEntity
import com.smartfilemanager.app.domain.model.ScannedFile
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleEngineTest {

    private lateinit var engine: RuleEngine

    @Before
    fun setup() {
        engine = RuleEngine()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun makeFile(
        name: String = "video.mp4",
        path: String = "/DCIM/Camera/",
        sizeBytes: Long = 10_000_000L,
        durationMs: Long = 60_000L,
        lastModified: Long = System.currentTimeMillis()
    ): ScannedFile = ScannedFile(
        id = 1L,
        uri = mockk(relaxed = true),
        name = name,
        path = path,
        sizeBytes = sizeBytes,
        durationMs = durationMs,
        lastModified = lastModified,
        width = null,
        height = null
    )

    private fun cond(field: String, operator: String, value: String, unit: String? = null) =
        ConditionEntity(ruleId = 0, field = field, operator = operator, value = value, unit = unit)

    private val DAY_MS = 24L * 60 * 60 * 1000

    // ── Duration ─────────────────────────────────────────────────────────────

    @Test
    fun `duration lt 60 matches 30s video`() {
        val file = makeFile(durationMs = 30_000L)
        assertTrue(engine.matches(file, cond("duration", "lt", "60")))
    }

    @Test
    fun `duration lt 60 does not match 90s video`() {
        val file = makeFile(durationMs = 90_000L)
        assertFalse(engine.matches(file, cond("duration", "lt", "60")))
    }

    @Test
    fun `duration gt 60 matches 90s video`() {
        val file = makeFile(durationMs = 90_000L)
        assertTrue(engine.matches(file, cond("duration", "gt", "60")))
    }

    // ── Age ───────────────────────────────────────────────────────────────────

    @Test
    fun `age gt 25 days matches 30-day-old file`() {
        val file = makeFile(lastModified = System.currentTimeMillis() - 30 * DAY_MS)
        assertTrue(engine.matches(file, cond("age", "gt", "25")))
    }

    @Test
    fun `age gt 25 days does not match 10-day-old file`() {
        val file = makeFile(lastModified = System.currentTimeMillis() - 10 * DAY_MS)
        assertFalse(engine.matches(file, cond("age", "gt", "25")))
    }

    // ── Size ─────────────────────────────────────────────────────────────────

    @Test
    fun `size lt 10 MB matches 5 MB file`() {
        val file = makeFile(sizeBytes = 5L * 1_048_576L)
        assertTrue(engine.matches(file, cond("size", "lt", "10", "MB")))
    }

    @Test
    fun `size gt 1 GB does not match 500 MB file`() {
        val file = makeFile(sizeBytes = 500L * 1_048_576L)
        assertFalse(engine.matches(file, cond("size", "gt", "1", "GB")))
    }

    @Test
    fun `size unit comparison is case-insensitive`() {
        val file = makeFile(sizeBytes = 5L * 1_048_576L)
        // "mb" lowercase stored in DB should work the same as "MB"
        assertTrue(engine.matches(file, cond("size", "lt", "10", "mb")))
    }

    // ── Extension ────────────────────────────────────────────────────────────

    @Test
    fun `extension mp4 matches video dot mp4`() {
        val file = makeFile(name = "video.mp4")
        assertTrue(engine.matches(file, cond("extension", "eq", ".mp4")))
    }

    @Test
    fun `extension mp4 without dot prefix also matches`() {
        val file = makeFile(name = "video.mp4")
        assertTrue(engine.matches(file, cond("extension", "eq", "mp4")))
    }

    @Test
    fun `extension mp4 does not match video dot mkv`() {
        val file = makeFile(name = "video.mkv")
        assertFalse(engine.matches(file, cond("extension", "eq", ".mp4")))
    }

    // ── File name ────────────────────────────────────────────────────────────

    @Test
    fun `file_name contains temp matches temp_clip dot mp4`() {
        val file = makeFile(name = "temp_clip.mp4")
        assertTrue(engine.matches(file, cond("file_name", "contains", "temp")))
    }

    @Test
    fun `file_name not_contains temp does not match temp_clip dot mp4`() {
        val file = makeFile(name = "temp_clip.mp4")
        assertFalse(engine.matches(file, cond("file_name", "not_contains", "temp")))
    }

    @Test
    fun `file_name contains is case-insensitive`() {
        val file = makeFile(name = "TEMP_CLIP.mp4")
        assertTrue(engine.matches(file, cond("file_name", "contains", "temp")))
    }

    // ── Directory ────────────────────────────────────────────────────────────

    @Test
    fun `directory contains DCIM matches file in DCIM path`() {
        val file = makeFile(path = "/DCIM/Camera/")
        assertTrue(engine.matches(file, cond("directory", "contains", "DCIM")))
    }

    @Test
    fun `directory not_contains Downloads excludes Download files`() {
        val file = makeFile(path = "/storage/emulated/0/Download/")
        assertFalse(engine.matches(file, cond("directory", "not_contains", "Download")))
    }

    // ── AND logic ────────────────────────────────────────────────────────────

    @Test
    fun `AND logic requires both conditions to match`() {
        val shortSmall = makeFile(durationMs = 30_000L, sizeBytes = 5L * 1_048_576L)
        val shortLarge = makeFile(durationMs = 30_000L, sizeBytes = 500L * 1_048_576L)
        val longSmall  = makeFile(durationMs = 120_000L, sizeBytes = 5L * 1_048_576L)

        val conditions = listOf(
            cond("duration", "lt", "60"),
            cond("size", "lt", "50", "MB")
        )

        val result = engine.evaluate(listOf(shortSmall, shortLarge, longSmall), conditions, "AND")

        assertEquals(1, result.size)
        assertTrue(result[0] === shortSmall)
    }

    // ── OR logic ─────────────────────────────────────────────────────────────

    @Test
    fun `OR logic matches files satisfying either condition`() {
        val shortFile     = makeFile(durationMs = 30_000L,  sizeBytes = 500L * 1_048_576L)
        val smallFile     = makeFile(durationMs = 120_000L, sizeBytes = 5L * 1_048_576L)
        val longLargeFile = makeFile(durationMs = 120_000L, sizeBytes = 500L * 1_048_576L)

        val conditions = listOf(
            cond("duration", "lt", "60"),
            cond("size", "lt", "50", "MB")
        )

        val result = engine.evaluate(listOf(shortFile, smallFile, longLargeFile), conditions, "OR")

        assertEquals(2, result.size)
        assertTrue(result.any { it === shortFile })
        assertTrue(result.any { it === smallFile })
    }

    // ── Empty conditions ─────────────────────────────────────────────────────

    @Test
    fun `empty conditions returns all files unchanged`() {
        val files = listOf(makeFile(name = "a.mp4"), makeFile(name = "b.mp4"), makeFile(name = "c.mp4"))
        val result = engine.evaluate(files, emptyList(), "AND")
        assertEquals(3, result.size)
    }

    @Test
    fun `empty conditions with OR logic also returns all files`() {
        val files = listOf(makeFile(name = "a.mp4"), makeFile(name = "b.mp4"))
        val result = engine.evaluate(files, emptyList(), "OR")
        assertEquals(2, result.size)
    }

    // ── Zero duration ────────────────────────────────────────────────────────

    @Test
    fun `file with durationMs 0 is treated as 0 seconds — gt 0 does not match`() {
        val file = makeFile(durationMs = 0L)
        assertFalse(engine.matches(file, cond("duration", "gt", "0")))
    }

    @Test
    fun `file with durationMs 0 matches duration lt 60`() {
        val file = makeFile(durationMs = 0L)
        assertTrue(engine.matches(file, cond("duration", "lt", "60")))
    }

    // ── Invalid / unknown ────────────────────────────────────────────────────

    @Test
    fun `unknown field returns false`() {
        val file = makeFile()
        assertFalse(engine.matches(file, cond("mime_type", "eq", "video/mp4")))
    }

    @Test
    fun `non-numeric value for duration returns false`() {
        val file = makeFile(durationMs = 30_000L)
        assertFalse(engine.matches(file, cond("duration", "lt", "not_a_number")))
    }
}
