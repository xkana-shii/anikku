package eu.kanade.tachiyomi.ui.player

/** Keep gesture and D-pad seeking inside this episode, never at the end-of-file boundary. */
internal fun boundedSeekTarget(positionSeconds: Double, durationSeconds: Double, offsetSeconds: Int): Int {
    val lastPlayableSecond = durationSeconds.toInt().minus(1).coerceAtLeast(0)
    return (positionSeconds + offsetSeconds).toInt().coerceIn(0, lastPlayableSecond)
}
