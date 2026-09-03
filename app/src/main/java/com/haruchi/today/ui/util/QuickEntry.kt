package com.haruchi.today.ui.util

/** Matches "빨래 20" or "빨래 20분" → label "빨래", minutes 20 — typed shorthand for quick entry. */
private val QUICK_ENTRY_REGEX = Regex("""^(.*\S)\s+(\d{1,3})\s*분?$""")

/** Matches a bare "20" or "20분" → no label, just the minutes. */
private val BARE_MINUTES_REGEX = Regex("""^(\d{1,3})\s*분?$""")

object QuickEntry {
    /** Splits "<label> <minutes>[분]" into (label, minutes); minutes is null when it doesn't match.
     * A bare number (just "20") is read as minutes with an empty label. */
    fun parse(raw: String): Pair<String, Int?> {
        val trimmed = raw.trim()
        BARE_MINUTES_REGEX.find(trimmed)?.let { m ->
            m.groupValues[1].toIntOrNull()?.takeIf { it in 1..300 }?.let { return "" to it }
        }
        val match = QUICK_ENTRY_REGEX.find(trimmed) ?: return trimmed to null
        val minutes = match.groupValues[2].toIntOrNull()?.takeIf { it in 1..300 } ?: return trimmed to null
        return match.groupValues[1] to minutes
    }
}
