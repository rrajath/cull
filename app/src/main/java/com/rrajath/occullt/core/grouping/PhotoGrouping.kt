package com.rrajath.occullt.core.grouping

import com.rrajath.occullt.core.model.UnifiedPhotoItem
import java.security.MessageDigest

data class PhotoStack(
    val photos: List<UnifiedPhotoItem>,
    val startTime: Long,
    val endTime: Long,
)

/**
 * Groups [photos] into time-window stacks. Photos must be pre-sorted ascending
 * by [timestamp]. A photo joins the current group while its timestamp is within
 * [windowMs] of the group's first photo. Groups with fewer than 2 photos are
 * dropped. Returned stacks are newest-first.
 */
fun groupPhotos(
    photos: List<UnifiedPhotoItem>,
    windowMs: Long,
    timestamp: (UnifiedPhotoItem) -> Long = { it.dateModified },
): List<PhotoStack> {
    if (photos.size < 2) return emptyList()

    val groups = mutableListOf<MutableList<UnifiedPhotoItem>>()
    var currentGroup = mutableListOf(photos[0])

    for (i in 1 until photos.size) {
        val diff = timestamp(photos[i]) - timestamp(currentGroup.first())
        if (diff <= windowMs) {
            currentGroup.add(photos[i])
        } else {
            if (currentGroup.size >= 2) {
                groups.add(currentGroup)
            }
            currentGroup = mutableListOf(photos[i])
        }
    }

    if (currentGroup.size >= 2) {
        groups.add(currentGroup)
    }

    return groups.reversed().map { group ->
        PhotoStack(
            photos = group,
            startTime = timestamp(group.first()),
            endTime = timestamp(group.last()),
        )
    }
}

/**
 * Stable fingerprint of a stack, derived from its photos' ids. Used to
 * persist per-stack "done" state across app restarts even though stacks are
 * regenerated on every visit. Changing the stack's composition (e.g. deleting
 * a photo from it) yields a new key, resetting the stack to not-done.
 */
fun stackKey(photos: List<UnifiedPhotoItem>): String {
    val joined = photos.joinToString("|") { it.id }
    val digest = MessageDigest.getInstance("MD5").digest(joined.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}
