package com.haruchi.today.data

/**
 * One photo attached to a session/receipt.
 *
 * The PRD keeps photos in app-private storage (permission 0 — system photo picker only).
 * [fileName] points at a JPEG inside that storage. When it is `null` the UI renders a
 * calm gradient placeholder — this is how the design shows a session that has no photo
 * yet (e.g. right after completion, before the user picks one).
 *
 * @param fileName  file name inside [PhotoStorage], or null for the gradient placeholder
 * @param aspect    width / height of the cropped photo — decides card orientation
 * @param toneIndex index into the design's gradient palette (placeholder + subtle tint)
 */
data class PhotoRef(
    val fileName: String? = null,
    val aspect: Float = 1f,
    val toneIndex: Int = 0,
)
