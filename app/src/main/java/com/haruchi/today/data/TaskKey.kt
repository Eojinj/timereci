package com.haruchi.today.data

/**
 * How two task names are decided to mean the same task. There is no task ID in the schema —
 * sessions carry a free-text label — so identity is the label itself, trimmed and case-folded.
 * Kept in one place because both stats roll-up and the todo queue depend on agreeing exactly.
 */
object TaskKey {
    fun of(label: String): String = label.trim().lowercase()
}
