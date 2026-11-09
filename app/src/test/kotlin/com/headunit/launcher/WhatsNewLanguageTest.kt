package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsNewLanguageTest {
    private val notes = "### Neu\n- Sprache: Deutsch\n  EN: Language: English\n- Nur Deutsch: ohne Übersetzung"

    @Test fun germanDropsEnglishLines() =
        assertEquals("### Neu\n- Sprache: Deutsch\n- Nur Deutsch: ohne Übersetzung", Updater.pickLanguage(notes, german = true))

    @Test fun englishReplacesTheGermanEntry() =
        assertEquals("### Neu\n- Language: English\n- Nur Deutsch: ohne Übersetzung", Updater.pickLanguage(notes, german = false))
}
