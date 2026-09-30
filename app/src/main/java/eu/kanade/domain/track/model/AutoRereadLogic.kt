package eu.kanade.domain.track.model

internal object AutoRereadLogic {
    enum class Action { NONE, PROMPT, START }

    fun action(completed: Boolean, behavior: AutoTrackState): Action = when {
        !completed -> Action.NONE
        behavior == AutoTrackState.ALWAYS -> Action.START
        behavior == AutoTrackState.ASK -> Action.PROMPT
        else -> Action.NONE
    }

    fun <T> episodesToReset(
        orderedEpisodes: List<T>,
        currentEpisodeId: Long?,
        resetMode: AutoRereadResetMode,
        idOf: (T) -> Long?,
    ): List<T> = when (resetMode) {
        AutoRereadResetMode.RESET_TO_ZERO -> orderedEpisodes
        AutoRereadResetMode.RESET_TO_CURRENT_EPISODE -> {
            val index = orderedEpisodes.indexOfFirst { idOf(it) == currentEpisodeId }
            if (index > 0) orderedEpisodes.take(index) else emptyList()
        }
    }

    fun resetProgress(resetMode: AutoRereadResetMode, currentEpisodeNumber: Double?): Double = when (resetMode) {
        AutoRereadResetMode.RESET_TO_ZERO -> 0.0
        AutoRereadResetMode.RESET_TO_CURRENT_EPISODE -> currentEpisodeNumber ?: 0.0
    }
}
