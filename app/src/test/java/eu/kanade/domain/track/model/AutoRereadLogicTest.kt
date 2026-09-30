package eu.kanade.domain.track.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AutoRereadLogicTest {
    @Test
    fun `completed entries honor on ask and off choices`() {
        AutoRereadLogic.action(completed = true, AutoTrackState.ALWAYS) shouldBe AutoRereadLogic.Action.START
        AutoRereadLogic.action(completed = true, AutoTrackState.ASK) shouldBe AutoRereadLogic.Action.PROMPT
        AutoRereadLogic.action(completed = true, AutoTrackState.NEVER) shouldBe AutoRereadLogic.Action.NONE
        AutoRereadLogic.action(completed = false, AutoTrackState.ALWAYS) shouldBe AutoRereadLogic.Action.NONE
    }

    @Test
    fun `reset modes preserve historical episode boundaries and progress`() {
        val episodes = listOf(5L, 4L, 3L, 2L, 1L)

        AutoRereadLogic.episodesToReset(
            episodes,
            currentEpisodeId = 3L,
            AutoRereadResetMode.RESET_TO_CURRENT_EPISODE,
            idOf = { it },
        ) shouldBe listOf(5L, 4L)
        AutoRereadLogic.episodesToReset(
            episodes,
            currentEpisodeId = 3L,
            AutoRereadResetMode.RESET_TO_ZERO,
            idOf = { it },
        ) shouldBe episodes
        AutoRereadLogic.resetProgress(AutoRereadResetMode.RESET_TO_CURRENT_EPISODE, 3.0) shouldBe 3.0
        AutoRereadLogic.resetProgress(AutoRereadResetMode.RESET_TO_ZERO, 3.0) shouldBe 0.0
    }
}
