package com.timereci.focus.ui.util

/** Matches "빨래 20" or "빨래 20분" → label "빨래", minutes 20 — typed shorthand for quick entry. */
private val QUICK_ENTRY_REGEX = Regex("""^(.*\S)\s+(\d{1,3})\s*분?$""")

object QuickEntry {
    /** Splits "<label> <minutes>[분]" into (label, minutes); minutes is null when it doesn't match. */
    fun parse(raw: String): Pair<String, Int?> {
        val trimmed = raw.trim()
        val match = QUICK_ENTRY_REGEX.find(trimmed) ?: return trimmed to null
        val minutes = match.groupValues[2].toIntOrNull()?.takeIf { it in 1..300 } ?: return trimmed to null
        return match.groupValues[1] to minutes
    }
}
