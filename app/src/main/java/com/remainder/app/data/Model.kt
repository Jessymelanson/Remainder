package com.remainder.app.data

/**
 * The three passes a paycheck goes through, in the order money actually
 * leaves.
 *
 * The order is the whole point. Bills first because they are already promised
 * and you have no say in them this fortnight. Living costs next because you
 * control them week to week. Savings last in the arithmetic, but treated as
 * non negotiable in the advice, which is the only way saving ever survives
 * contact with a real month.
 */
enum class Group(
    val icon: String,
    val label: String,
    /** For chips and other places a full label will not fit. */
    val short: String,
    val blurb: String
) {
    BILL(
        "🧾", "Bills", "Bills",
        "Fixed costs that arrive whether you like them or not."
    ),
    LIVING(
        "🛒", "Living costs", "Living",
        "Day to day spending. The part you can actually steer."
    ),
    SAVING(
        "🏦", "Savings and goals", "Savings",
        "Money you keep. Paid to yourself like any other bill."
    )
}

/**
 * How often an amount is charged.
 *
 * Only two, because only two things are really going on. Either the cost
 * repeats with your pay, or it repeats with the calendar. A monthly cost has
 * to be covered by whichever paychecks happen to land in that month, and that
 * count is not always the same, which is what [Category.perPaycheck] takes as
 * an argument rather than assuming.
 */
enum class Cadence(val label: String, val short: String) {
    PER_PAYCHECK("Every paycheck", "per check"),
    MONTHLY("Every month", "per month")
}

/** Paychecks in a year when you are paid every two weeks. */
const val PAYCHECKS_PER_YEAR = 26.0

/** Months in a year, for turning a monthly cost into a yearly one. */
const val MONTHS_PER_YEAR = 12.0

data class Category(
    val id: String,
    val icon: String,
    val name: String,
    val group: Group,
    val amount: Double = 0.0,
    val cadence: Cadence = Cadence.MONTHLY,
    /** What is already in this pot. Only meaningful for a savings category. */
    val saved: Double = 0.0,
    /** What you are aiming for. Zero means no goal has been set. */
    val target: Double = 0.0,
    /** Left in the list but not deducted, for a bill you have paid off. */
    val enabled: Boolean = true,
    /** Added by the user, so it can be deleted. The built in ten cannot. */
    val custom: Boolean = false
) {

    /**
     * What this costs out of one paycheck, in a month with [paydaysInMonth]
     * paydays in it.
     *
     * A monthly bill is split across the paychecks that actually arrive that
     * month. In an ordinary two payday month a $1,300 mortgage takes $650 from
     * each check. In a three payday month it takes $433.33, and that is not a
     * discount, it is the same bill spread over one more paycheck, which is
     * exactly why a three payday month feels easy.
     */
    fun perPaycheck(paydaysInMonth: Int): Double =
        if (!enabled) 0.0 else when (cadence) {
            Cadence.PER_PAYCHECK -> amount
            Cadence.MONTHLY -> amount / paydaysInMonth.coerceAtLeast(1)
        }

    /** What this costs over a year, which does not depend on any one month. */
    val perYear: Double
        get() = if (!enabled) 0.0 else when (cadence) {
            Cadence.PER_PAYCHECK -> amount * PAYCHECKS_PER_YEAR
            Cadence.MONTHLY -> amount * MONTHS_PER_YEAR
        }

    /** What this costs in one month, whichever clock it was entered on. */
    fun perMonth(paydaysInMonth: Int): Double =
        if (!enabled) 0.0 else when (cadence) {
            Cadence.PER_PAYCHECK -> amount * paydaysInMonth.coerceAtLeast(1)
            Cadence.MONTHLY -> amount
        }

    /**
     * What this takes out of a typical paycheck across the whole year.
     *
     * The figure for anything projected past this month, like a goal's finish
     * date. A monthly amount is split two ways in most months and three in
     * some, so extrapolating from any single month's slice is wrong in every
     * month: $600 a month towards $6,000 is ten months either way, but read
     * off a two payday month it came out at 20 paychecks and off a three
     * payday month at 30, and the date jumped by months as you paged through
     * them.
     */
    val perPaycheckOnAverage: Double get() = perYear / PAYCHECKS_PER_YEAR

    /** True when the figure taken per paycheck differs from the one entered. */
    val converted: Boolean get() = cadence == Cadence.MONTHLY && enabled && amount > 0.0

    /** Goals only make sense for money you are keeping. */
    val canHaveGoal: Boolean get() = group == Group.SAVING

    val hasGoal: Boolean get() = canHaveGoal && target > 0.0

    /** How this pot is doing, given what goes in from each paycheck. */
    fun goal(perPaycheck: Double): GoalProgress =
        GoalProgress(saved = saved, target = target, perPaycheck = perPaycheck)
}

/**
 * A savings pot measured against what it is for.
 *
 * Kept separate from [Category] so the arithmetic can be tested on its own, and
 * because the interesting question is not "how much is in there" but "when does
 * this actually finish", which needs the contribution as well as the balance.
 */
data class GoalProgress(
    val saved: Double,
    val target: Double,
    /** What goes in out of each paycheck. */
    val perPaycheck: Double
) {

    val remaining: Double get() = (target - saved).coerceAtLeast(0.0)

    val fraction: Double
        get() = if (target <= 0.0) 0.0 else (saved / target).coerceIn(0.0, 1.0)

    val reached: Boolean get() = target > 0.0 && remaining < 0.005

    /**
     * Paychecks still to go, or null when nothing is going in.
     *
     * Null rather than infinity, because "never at this rate" is a real answer
     * and has to be said out loud instead of shown as a nonsense date. The
     * result is capped so a huge target and a tiny contribution cannot
     * overflow into a negative number of paychecks.
     */
    val paychecksNeeded: Int?
        get() = when {
            target <= 0.0 -> null
            reached -> 0
            perPaycheck <= 0.0 -> null
            else -> Math.ceil(remaining / perPaycheck)
                .coerceAtMost(MAX_PAYCHECKS.toDouble())
                .toInt()
        }

    /** True when the goal is real but nothing is being put towards it. */
    val stalled: Boolean get() = target > 0.0 && !reached && perPaycheck <= 0.0

    /**
     * What to put in each paycheck to finish within [paychecks] paydays.
     *
     * Rounded up to the nearest five, because the point is to hand back a
     * figure someone would actually choose rather than $384.62, and rounding
     * up means the tidier number cannot land after the date it was asked
     * about.
     */
    fun perPaycheckToFinishIn(paychecks: Int): Double {
        if (remaining <= 0.0 || paychecks <= 0) return 0.0
        return Math.ceil(remaining / paychecks / 5.0) * 5.0
    }

    private companion object {
        /** A century of fortnights. Past this the answer is "not like this". */
        const val MAX_PAYCHECKS = 2600
    }
}

/**
 * The ten asked for, with sensible cadences already chosen.
 *
 * Bills default to monthly because that is how they are billed, and groceries
 * and fuel default to per paycheck because that is how they are spent. Getting
 * these defaults right is most of the difference between someone finishing
 * setup and someone giving up on it.
 */
fun defaultCategories(): List<Category> = listOf(
    Category("phone", "📱", "Phone", Group.BILL, cadence = Cadence.MONTHLY),
    Category("car", "🚗", "Car insurance", Group.BILL, cadence = Cadence.MONTHLY),
    Category("mortgage", "🏠", "Mortgage", Group.BILL, cadence = Cadence.MONTHLY),
    Category("electric", "💡", "Electric", Group.BILL, cadence = Cadence.MONTHLY),
    Category("internet", "🌐", "Internet", Group.BILL, cadence = Cadence.MONTHLY),

    Category("groceries", "🛒", "Groceries", Group.LIVING, cadence = Cadence.PER_PAYCHECK),
    Category("gas", "⛽", "Gas", Group.LIVING, cadence = Cadence.PER_PAYCHECK),

    Category("savings", "💰", "Savings", Group.SAVING, cadence = Cadence.PER_PAYCHECK),
    Category("emergency", "🛟", "Emergency fund", Group.SAVING, cadence = Cadence.PER_PAYCHECK),
    Category("goals", "🎯", "Goals", Group.SAVING, cadence = Cadence.PER_PAYCHECK)
)

/** Emoji offered when naming a category of your own. */
val ICON_CHOICES = listOf(
    "💖", "🐷", "🎁", "🍕", "☕", "🍼", "👶", "🐶", "🐱", "💊",
    "🏥", "🦷", "👓", "💅", "💇", "👗", "👟", "🎮", "🎬", "🎵",
    "📚", "🎓", "✈️", "🏖️", "🎄", "🎂", "🏋️", "🚲", "🚌", "🅿️",
    "🔧", "🧹", "🧺", "🪴", "🐕", "📺", "☂️", "💳", "🏦", "⭐"
)
