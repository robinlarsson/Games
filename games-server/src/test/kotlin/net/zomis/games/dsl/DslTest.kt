package net.zomis.games.dsl

import net.zomis.games.common.Point
import net.zomis.games.dsl.impl.GameImpl
import net.zomis.games.dsl.impl.GameSetupImpl
import net.zomis.games.impl.ttt.DslTTT
import net.zomis.games.impl.ttt.TTOptions
import net.zomis.tttultimate.TTPlayer
import net.zomis.tttultimate.games.TTController
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.random.Random

class DslTest {

    @Test
    fun config() {
        val setup = GameSetupImpl(DslTTT.game)
        Assertions.assertEquals(TTOptions::class, setup.configClass())
    }

    private fun createGame(): GameImpl<TTController> {
        val setup = GameSetupImpl(DslTTT.game)
        val game = setup.createGame(2, TTOptions(3, 3, 3))
        Assertions.assertNotNull(game)
        return game
    }

    @Test
    fun wrongActionType() {
        val game = createGame()
        val act = game.actions.type("play") // should be <Point>
        Assertions.assertNotNull(act)
        Assertions.assertThrows(ClassCastException::class.java) {
            val action = act!!.createAction(0, 42)
            act.perform(action)
        }
    }

    @Test
    fun wrongActionName() {
        val game = createGame()
        val act = game.actions.type("missing")
        Assertions.assertNull(act)
    }

    @Test
    fun allowedActions() {
        val game = createGame()
        Assertions.assertEquals(9, game.actions.type("play")!!.availableActions(0, null).count())
    }

    @Test
    fun makeAMove() {
        val game = createGame()
        val actions = game.actions.type("play")!!
        val action = actions.createActionFromSerialized(0, Point(1, 2))
        actions.perform(action)
        Assertions.assertEquals(TTPlayer.X, game.model.game.getSub(1, 2)!!.wonBy)
    }

    @Test
    fun currentPlayerChanges() {
        val game = createGame()
        Assertions.assertEquals(0, game.view(0)["currentPlayer"])
        val actionType = game.actions.type("play")!!
        val action = actionType.availableActions(0, null).toList().random()
        actionType.perform(action)
        Assertions.assertEquals(1, game.view(0)["currentPlayer"])
    }

    @Test
    fun finishGame() {
        val game = createGame()
        var counter = 0
        val random = Random(23)
        while (!game.isGameOver() && counter < 20) {
            val playerIndex = counter % 2
            val actionType = game.actions.type("play")!!
            val availableActions = actionType.availableActions(playerIndex, null)
            Assertions.assertFalse(availableActions.none(), "Game is not over but no available actions after $counter actions")
            val action = availableActions.toList().random(random)
            actionType.perform(action)
            counter++
        }
        Assertions.assertEquals(2, game.eliminationCallback.eliminations().distinctBy { it.playerIndex }.size)
    }

}
