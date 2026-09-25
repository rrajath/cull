package com.rrajath.cull.core.datastore

import com.rrajath.cull.ui.component.SourceMode
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsExportTest {

    private fun export(
        sourceMode: String = SourceMode.Hybrid.name,
        longPressThresholdMs: Int = 220,
        accentHue: Int = 0,
        groupingWindowMinutes: Int = 2,
    ) = SettingsExport(
        sourceMode = sourceMode,
        immichUrl = "https://immich.example.com",
        longPressThresholdMs = longPressThresholdMs,
        dryRun = false,
        mirrorDeletes = false,
        darkTheme = true,
        accentHue = accentHue,
        groupingWindowMinutes = groupingWindowMinutes,
    )

    @Test
    fun `valid export is unchanged`() {
        val original = export()
        assertEquals(original, original.sanitized())
    }

    @Test
    fun `all valid source modes survive sanitization`() {
        SourceMode.entries.forEach { mode ->
            assertEquals(mode.name, export(sourceMode = mode.name).sanitized().sourceMode)
        }
    }

    @Test
    fun `unknown source mode falls back to Hybrid`() {
        assertEquals(SourceMode.Hybrid.name, export(sourceMode = "EvilMode").sanitized().sourceMode)
        assertEquals(SourceMode.Hybrid.name, export(sourceMode = "").sanitized().sourceMode)
    }

    @Test
    fun `long press threshold is clamped to UI range`() {
        assertEquals(80, export(longPressThresholdMs = -5000).sanitized().longPressThresholdMs)
        assertEquals(800, export(longPressThresholdMs = Int.MAX_VALUE).sanitized().longPressThresholdMs)
        assertEquals(220, export(longPressThresholdMs = 220).sanitized().longPressThresholdMs)
    }

    @Test
    fun `accent hue is clamped to palette range`() {
        assertEquals(0, export(accentHue = -1).sanitized().accentHue)
        assertEquals(7, export(accentHue = 99).sanitized().accentHue)
        assertEquals(3, export(accentHue = 3).sanitized().accentHue)
    }

    @Test
    fun `grouping window is clamped to UI range`() {
        assertEquals(1, export(groupingWindowMinutes = 0).sanitized().groupingWindowMinutes)
        assertEquals(30, export(groupingWindowMinutes = 100000).sanitized().groupingWindowMinutes)
        assertEquals(2, export(groupingWindowMinutes = 2).sanitized().groupingWindowMinutes)
    }
}
