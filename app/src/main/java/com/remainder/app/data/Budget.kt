package com.remainder.app.data

import kotlin.math.abs

/** Where a paycheck stands once everything has come out of it. */
enum class Verdict(val icon: String, val label: String) {
    UNDER("💚", "Under budget"),
    BALANCED("🎯", "Balanced to the penny"),
    OVER("💔", "Over budget"),
    EMPTY("✨", "Waiting on your paycheck")
}

/** One deduction, with the balance that survived it. */
data class Step(
    val category: Category,
    val taken: Double,
    val leftAfter: Double
)

/** One of the three passes, with its own running total. */
data class Stage(
    val group: Group,
    val steps: List<Step>,
    val startedWith: Double,
    val took: Double,
    val leftAfter: Double
) {
    val isEmpty: Boolean get() = steps.isEmpty()
}

/**
 * The whole calculation, done once, in the order the money leaves.
 *
 * Everything the screens show is read off this. Nothing recomputes a total of
 * its own, because two places doing the same arithmetic is how a budget app
 * ends up disagreeing with itself.
 *
 * [paydaysThisMonth] is what makes the figures real rather than averaged. A
 * monthly bill is covered by the paychecks that actually arrive that month, so
 * the same mortgage takes a different slice out of a two payday month than a
 * three payday one. The whole budget is therefore for a named month, not for
 * an abstract average fortnight.
 *
 * Money is held as a Double and compared with an epsilon rather than for exact
 * equality. Splitting a monthly bill three ways will not land on a whole cent,
 * and calling that "over budget" would be a lie told by floating point rather
 * than by the numbers.
 */
data class Budget(
    val paycheck: Double,
    val categories: List<Category>,
    /** Paydays landing in the month being budgeted. Two usually, sometimes three. */
    val paydaysThisMonth: Int = 2
) {

    /** What one category takes out of one paycheck this month. */
    fun slice(category: Category): Double = category.perPaycheck(paydaysThisMonth)

    val stages: List<Stage> = buildStages()

    private fun buildStages(): List<Stage> {
        var running = paycheck
        return Group.entries.map { group ->
            val startedWith = running
            val steps = categories
                .filter { it.group == group && it.enabled && it.amount > 0.0 }
                .map { c ->
                    val taken = c.perPaycheck(paydaysThisMonth)
                    running -= taken
                    Step(c, taken, running)
                }
            Stage(
                group = group,
                steps = steps,
                startedWith = startedWith,
                took = startedWith - running,
                leftAfter = running
            )
        }
    }

    fun stage(group: Group): Stage = stages.first { it.group == group }

    val bills: Stage get() = stage(Group.BILL)
    val living: Stage get() = stage(Group.LIVING)
    val saving: Stage get() = stage(Group.SAVING)

    /** What is left of one paycheck when every deduction has been made. */
    val leftOver: Double get() = stages.last().leftAfter

    val totalOut: Double get() = paycheck - leftOver

    val verdict: Verdict
        get() = when {
            paycheck <= 0.0 -> Verdict.EMPTY
            leftOver > EPSILON -> Verdict.UNDER
            leftOver < -EPSILON -> Verdict.OVER
            else -> Verdict.BALANCED
        }

    /** How far past the paycheck the plan goes. Zero unless over budget. */
    val shortfall: Double get() = if (leftOver < -EPSILON) -leftOver else 0.0

    /**
     * A figure as a share of the paycheck.
     *
     * Deliberately not capped at 100%. If bills alone come to 120% of what
     * came in, that is the single most important fact on the screen, and
     * rounding it down to a tidy "100%" would hide it.
     */
    fun share(amount: Double): Double =
        if (paycheck <= 0.0) 0.0 else (amount / paycheck).coerceAtLeast(0.0)

    // ---- The month, rather than the single paycheck -----------------------

    val monthIncome: Double get() = paycheck * paydaysThisMonth
    val monthOut: Double get() = totalOut * paydaysThisMonth
    val monthLeftOver: Double get() = leftOver * paydaysThisMonth

    /** True in the months that catch a third payday. */
    val isThreePaydayMonth: Boolean get() = paydaysThisMonth >= 3

    /**
     * What a third payday is actually worth.
     *
     * The extra paycheck arrives in a month whose monthly bills two paychecks
     * already cover, so only the per paycheck costs come out of it. That
     * difference, not the whole paycheck, is what is genuinely spare.
     */
    val thirdPaydayIsWorth: Double
        get() {
            if (paycheck <= 0.0) return 0.0
            val perCheckCosts = categories
                .filter { it.enabled && it.cadence == Cadence.PER_PAYCHECK }
                .sumOf { it.amount }
            return maxOf(0.0, paycheck - perCheckCosts)
        }

    /**
     * Everything kept rather than spent, as a share of the paycheck.
     *
     * Leftover counts towards it. Money still sitting in the account at the
     * end of the fortnight has not been saved on purpose, but it has not been
     * spent either, and a rate that ignored it would understate how well the
     * fortnight actually went.
     */
    val savingRate: Double
        get() = if (paycheck <= 0.0) 0.0 else {
            ((saving.took + maxOf(leftOver, 0.0)) / paycheck).coerceIn(0.0, 1.0)
        }

    /** Needs, in the 50/30/20 sense: bills plus the cost of living. */
    val needsRate: Double get() = share(bills.took + living.took)

    val yearlyLeftOver: Double get() = leftOver * PAYCHECKS_PER_YEAR
    /**
     * What the savings lines put away over a year.
     *
     * Summed from each line's yearly figure rather than this paycheck's slice
     * times 26: a monthly contribution is split two or three ways depending on
     * the month, so the slice times 26 came to thirteen months of it in most
     * months and under nine in a three payday one.
     */
    val yearlySaved: Double
        get() = categories.filter { it.group == Group.SAVING }.sumOf { it.perYear }

    /**
     * Bills and living costs for a typical month, for sizing an emergency fund.
     *
     * The year's outgoings over twelve, not the month on screen. Per paycheck
     * costs come out three times in a three payday month, so sizing the fund
     * against that month moved "three months of cover" every time someone
     * paged between months, for a buffer meant to last through any of them.
     */
    val monthlyOutgoings: Double
        get() = categories
            .filter { it.group != Group.SAVING }
            .sumOf { it.perYear } / MONTHS_PER_YEAR

    /** Biggest deduction that is not a bill, which is the easiest lever. */
    val easiestCut: Category?
        get() = categories
            .filter { it.enabled && it.amount > 0.0 && it.group != Group.BILL }
            .maxByOrNull { slice(it) }

    val biggestBill: Category?
        get() = categories
            .filter { it.enabled && it.amount > 0.0 && it.group == Group.BILL }
            .maxByOrNull { slice(it) }

    val anythingEntered: Boolean
        get() = categories.any { it.enabled && it.amount > 0.0 }

    companion object {
        /**
         * Half a cent. Anything smaller is rounding, not money, and must not
         * be allowed to read as being over budget.
         */
        const val EPSILON = 0.005

        fun isZero(v: Double): Boolean = abs(v) < EPSILON
    }
}
