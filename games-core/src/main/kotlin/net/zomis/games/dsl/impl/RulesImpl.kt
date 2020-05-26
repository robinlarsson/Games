package net.zomis.games.dsl.impl

import net.zomis.games.PlayerEliminations
import net.zomis.games.dsl.*

class GameRulesContext<T : Any>(
    val model: T,
    val replayable: ReplayState,
    val eliminations: PlayerEliminations
): GameRules<T> {
    private val views = mutableListOf<Pair<String, ViewScope<T>.() -> Any?>>()
//    private val logger = KLoggers.logger(this)
    private val globalRules = GameRuleList<T>(model, replayable, eliminations)
    private val ruleList = mutableMapOf<String, GameActionRuleContext<T, Any>>()

    override val allActions: GameAllActionsRule<T>
        get() = globalRules

    override fun <A : Any> action(actionType: ActionType<A>): GameActionRule<T, A> {
        return ruleList.getOrPut(actionType.name) {
            GameActionRuleContext(model, replayable, eliminations, actionType, globalRules) as GameActionRuleContext<T, Any>
        } as GameActionRule<T, A>
    }

    override fun view(key: String, value: ViewScope<T>.() -> Any?) {
        this.views.add(key to value)
    }

    override fun gameStart(onStart: GameStartScope<T>.() -> Unit) {
        onStart.invoke(GameStartContext(model, replayable))
    }

    fun view(context: GameViewContext<T>) {
        views.forEach {
            val key = it.first
            val function = it.second
            val result = function(context)
            context.value(key) { result }
        }
    }

    fun actionTypes(): Set<String> {
        return this.ruleList.keys.toSet()
    }

    fun actionType(actionType: String): ActionTypeImplEntry<T, Any, Actionable<T, Any>>? {
        return this.ruleList[actionType].let {
            if (it != null) { ActionTypeImplEntry(model, replayable, it.actionDefinition, it) } else null
        }
    }
}

class GameStartContext<T : Any>(override val game: T, override val replayable: ReplayableScope) : GameStartScope<T>
class GameActionContext<T : Any, A : Any>(
    override val action: Actionable<T, A>,
    override val replayable: ReplayableScope,
    override val eliminations: PlayerEliminations,
    override val game: T
): ActionRuleScope<T, A>

class GameRuleList<T : Any>(
    val model: T,
    val replayable: ReplayableScope,
    val eliminations: PlayerEliminations
): GameAllActionsRule<T> {
    val after = mutableListOf<ActionRuleScope<T, Any>.() -> Unit>()
    val allowed = mutableListOf<ActionRuleScope<T, Any>.() -> Boolean>()

    override fun after(rule: ActionRuleScope<T, Any>.() -> Unit) { this.after.add(rule) }
    override fun requires(rule: ActionRuleScope<T, Any>.() -> Boolean) { this.allowed.add(rule) }
}

class ActionOptionsContext<T : Any>(override val game: T, override val playerIndex: Int) : ActionOptionsScope<T>
class ActionRuleContext<T : Any, A : Any>(
    override val game: T,
    override val action: Actionable<T, A>,
    override val eliminations: PlayerEliminations,
    override val replayable: ReplayableScope
): ActionRuleScope<T, A>

class GameActionRuleContext<T : Any, A : Any>(
    val model: T,
    val replayable: ReplayState,
    val eliminations: PlayerEliminations,
    val actionDefinition: ActionType<A>,
    val globalRules: GameRuleList<T>
): GameActionRule<T, A>, GameLogicActionType<T, A, Actionable<T, A>> {
    override val actionType: String = actionDefinition.name

    val effects = mutableListOf<ActionRuleScope<T, A>.() -> Unit>()
    val after = mutableListOf<ActionRuleScope<T, A>.() -> Unit>()
    val allowed = mutableListOf<ActionRuleScope<T, A>.() -> Boolean>()
    private var availableActionsEvaluator: (ActionOptionsScope<T>.() -> Iterable<A>)? = null

    override fun after(rule: ActionRuleScope<T, A>.() -> Unit) { this.after.add(rule) }
    override fun effect(rule: ActionRuleScope<T, A>.() -> Unit) { this.effects.add(rule) }
    override fun requires(rule: ActionRuleScope<T, A>.() -> Boolean) { this.allowed.add(rule) }

    override fun options(rule: ActionOptionsScope<T>.() -> Iterable<A>) {
        this.availableActionsEvaluator = rule
    }

    override fun forceUntil(rule: ActionRuleScope<T, A>.() -> Boolean) {
        globalRules.allowed.add {
            action.actionType == actionType || rule(this as ActionRuleScope<T, A>)
        }
    }

    // GameLogicActionType implementation below

    override fun availableActions(playerIndex: Int): Iterable<Actionable<T, A>> {
        val context = ActionOptionsContext(model, playerIndex)
        val evaluator = this.availableActionsEvaluator
        return if (evaluator == null) {
            require(this.actionDefinition.parameterType == Unit::class) {
                "Actions of type ${actionDefinition.parameterType} needs to specify a list of allowed parameters"
            }
            listOf(createAction(playerIndex, Unit as A)).filter { actionAllowed(it) }
        } else evaluator(context).map { createAction(playerIndex, it) }.filter { this.actionAllowed(it) }
    }

    override fun actionAllowed(action: Actionable<T, A>): Boolean {
        val context = ActionRuleContext(model, action, eliminations, replayable)
        return globalRules.allowed.all { it.invoke(context as ActionRuleScope<T, Any>) } && allowed.all { it(context) }
    }

    override fun replayAction(action: Actionable<T, A>, state: Map<String, Any>?) {
        if (state != null) {
            replayable.setReplayState(state)
        }
        this.performAction(action)
    }

    override fun performAction(action: Actionable<T, A>) {
        val context = GameActionContext(action, replayable, eliminations, model)
        this.effects.forEach { it.invoke(context) }
        this.after.forEach { it.invoke(context) }
        this.globalRules.after.forEach { it.invoke(context as ActionRuleScope<T, Any>) }
    }

    override fun createAction(playerIndex: Int, parameter: A): Action<T, A>
        = Action(model, playerIndex, actionType, parameter)

}