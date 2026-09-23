@file:OptIn(ExperimentalMaterial3Api::class)

package com.remainder.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.remainder.app.data.Budget
import com.remainder.app.data.Cadence
import com.remainder.app.data.Category
import com.remainder.app.data.GoalProgress
import com.remainder.app.data.Group
import com.remainder.app.data.PAYCHECKS_PER_YEAR
import com.remainder.app.data.Stage
import com.remainder.app.data.Verdict

/**
 * The paycheck explained one step at a time.
 *
 * The main screen answers "am I alright". This one answers "why", and it is
 * written for someone who has never had a budget explained to them, in the
 * order money actually leaves an account rather than the order a spreadsheet
 * would like to add it up.
 */
@Composable
fun BreakdownScreen(state: BudgetViewModel.State) {
    val budget = state.budget
    val month = monthName(state.month)

    Scaffold(
        topBar = { TopAppBar(title = { Text("The breakdown") }) }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (budget.paycheck <= 0.0) {
                item { NothingYet() }
                return@LazyColumn
            }

            item {
                Step(1, "💵", "Start with what came in") {
                    BigLine("Your paycheck", Cash.money(budget.paycheck), Money.level())
                    VSpace(8)
                    Text(
                        "Every figure below is out of this one paycheck. " +
                            "${monthName(state.month)} catches " +
                            "${state.paydaysThisMonth} of them, so " +
                            "${Cash.money(budget.monthIncome)} comes in over the " +
                            "whole month.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (budget.isThreePaydayMonth) {
                        VSpace(6)
                        Text(
                            "This is one of your three payday months. The monthly " +
                                "bills below are split three ways instead of two, " +
                                "which is why each check goes further.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Money.good()
                        )
                    }
                }
            }

            item {
                Step(2, "🧾", "Bills come out first") {
                    Text(
                        "Bills are already promised. You cannot decide not to pay " +
                            "the mortgage this fortnight, so they come out before " +
                            "anything you still have a choice about.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    VSpace(12)
                    StageLines(budget.bills, budget)
                }
            }

            item {
                Step(3, "🛒", "Then the cost of living") {
                    Text(
                        "Groceries and fuel are different from bills. The amount is " +
                            "yours to set, which makes this the first place to look " +
                            "when the numbers do not fit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    VSpace(12)
                    StageLines(budget.living, budget)
                }
            }

            item {
                Step(4, "🏦", "Then pay yourself") {
                    Text(
                        "This is last in the arithmetic and first in importance. " +
                            "Saving whatever happens to be left never works, because " +
                            "something always turns up to spend it on. Treating " +
                            "savings as a bill you owe yourself is the single habit " +
                            "that separates people who build a cushion from people " +
                            "who mean to.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    VSpace(12)
                    StageLines(budget.saving, budget)
                }
            }

            item {
                Step(5, budget.verdict.icon, "Where that leaves you") {
                    val tint = when (budget.verdict) {
                        Verdict.UNDER -> Money.good()
                        Verdict.BALANCED -> Money.level()
                        Verdict.OVER -> Money.bad()
                        Verdict.EMPTY -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    BigLine(
                        if (budget.verdict == Verdict.OVER) "Short by" else "Left over",
                        Cash.money(
                            if (budget.verdict == Verdict.OVER) budget.shortfall
                            else budget.leftOver
                        ),
                        tint
                    )
                    VSpace(10)
                    Text(
                        "${Cash.money(budget.paycheck)} came in and " +
                            "${Cash.money(budget.totalOut)} was spoken for, which is " +
                            "${Cash.percent(budget.share(budget.totalOut))} of the " +
                            "paycheck.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (budget.verdict == Verdict.UNDER) {
                        VSpace(6)
                        Text(
                            "Over a year that leftover adds up to " +
                                "${Cash.money(budget.yearlyLeftOver)}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Money.good()
                        )
                    }
                }
            }

            item { SectionHeader("What a budget adviser would say") }

            // Named rather than left as "twice a year", because knowing it is
            // July is what lets someone plan for it.
            val threeMonths = state.threePaydayMonths.joinToString(" and ") { monthName(it) }
            for (note in advice(budget, month, threeMonths, { monthYear(state.goalDate(it)) })) {
                item(key = note.key) { AdviceCard(note) }
            }

            item {
                VSpace(8)
                Text(
                    "Remainder does arithmetic, not financial advice. It knows what " +
                        "you typed in and nothing else about your circumstances.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VSpace(24)
            }
        }
    }
}

@Composable
private fun NothingYet() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("✨", style = MaterialTheme.typography.headlineLarge)
            VSpace(8)
            Text("Nothing to explain yet", style = MaterialTheme.typography.titleLarge)
            VSpace(6)
            Text(
                "Put your bi-weekly paycheck in on the Budget tab and this page " +
                    "will walk through exactly where it goes, one step at a time.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** A numbered step with a title and whatever it needs to say. */
@Composable
private fun Step(number: Int, icon: String, title: String, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$number",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Box(Modifier.size(10.dp))
                Text(icon, style = MaterialTheme.typography.titleLarge)
                Box(Modifier.size(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            VSpace(12)
            content()
        }
    }
}

@Composable
private fun BigLine(label: String, value: String, tint: Color) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = tint)
    }
}

/**
 * One pass, line by line, with the balance after each deduction.
 *
 * Showing the running balance rather than only a total is the difference
 * between being told the answer and being shown the working. It is also where
 * someone spots the one line that is wrong.
 */
@Composable
private fun StageLines(stage: Stage, budget: Budget) {
    if (stage.isEmpty) {
        Text(
            "Nothing set here yet, so nothing came out.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Row(
        Modifier.fillMaxWidth().padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Starting with",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            Cash.money(stage.startedWith),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    stage.steps.forEach { step ->
        Row(
            Modifier.fillMaxWidth().padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(step.category.icon, style = MaterialTheme.typography.titleMedium)
            Box(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    step.category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Spelling the conversion out is the point. Someone who put in
                // a monthly mortgage needs to see why a different number came
                // out, or they will assume the app is wrong.
                if (step.category.converted) {
                    Text(
                        "${Cash.money(step.category.amount)} " +
                            "${step.category.cadence.short}, split across the " +
                            "${budget.paydaysThisMonth} paychecks this month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(Modifier.size(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Cash.minus(step.taken),
                    style = MaterialTheme.typography.bodyMedium,
                    color = groupColour(stage.group)
                )
                Text(
                    Cash.money(step.leftAfter) + " left",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (step.leftAfter < -Budget.EPSILON) {
                        Money.bad()
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }

    HorizontalDivider(Modifier.padding(vertical = 8.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Took ${Cash.money(stage.took)}, " +
                "${Cash.percent(budget.share(stage.took))} of the paycheck",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            Cash.money(stage.leftAfter),
            style = MaterialTheme.typography.titleSmall,
            color = if (stage.leftAfter < -Budget.EPSILON) Money.bad() else Money.good()
        )
    }
}

/**
 * One piece of plain advice, tied to a number the user actually entered.
 *
 * [key] is what the list is keyed on, and it is separate from [title] because
 * the title is not unique and a LazyColumn key has to be. A note about a goal
 * is titled after the category, and nothing stops somebody having two
 * categories called "Savings" - at which point two cards claim the same key and
 * the Breakdown tab does not render, it throws. The notes about the budget as a
 * whole are one of a kind, so their title serves; the ones about a particular
 * category are keyed on that category's id, which is unique by construction and
 * survives a rename into the bargain.
 */
internal data class Note(
    val icon: String,
    val title: String,
    val body: String,
    val key: String = title
)

@Composable
private fun AdviceCard(note: Note) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(Modifier.padding(14.dp)) {
            Text(note.icon, style = MaterialTheme.typography.headlineSmall)
            Box(Modifier.size(12.dp))
            Column {
                Text(note.title, style = MaterialTheme.typography.titleSmall)
                VSpace(4)
                Text(
                    note.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * A contribution as it was entered: "$600.00 a month" or "$200.00 a paycheck".
 *
 * The finish dates beside it come from the yearly average, so quoting this
 * month's slice would pair a figure with a date it did not produce.
 */
private fun contribution(c: Category, scale: Double = 1.0): String =
    Cash.money(c.amount * scale) +
        if (c.cadence == Cadence.MONTHLY) " a month" else " a paycheck"

/**
 * The advice, built from the numbers rather than picked from a list.
 *
 * Every note here names a figure the user typed in. Generic encouragement is
 * what makes a budgeting app feel like a brochure, and it is also the fastest
 * way to lose someone who is genuinely short this fortnight.
 */
internal fun advice(
    b: Budget,
    month: String,
    threeMonths: String,
    goalDate: (Int) -> String
): List<Note> {
    val notes = mutableListOf<Note>()
    val monthlyOutgoings = b.monthlyOutgoings

    if (b.verdict == Verdict.OVER) {
        val perPaycheck = b.shortfall
        val lever = b.easiestCut
        notes += Note(
            "🚨",
            "You are ${Cash.money(perPaycheck)} short every paycheck",
            "That is ${Cash.money(perPaycheck * PAYCHECKS_PER_YEAR)} a year, and it " +
                "has to come from somewhere: savings you will not actually make, " +
                "or debt. " +
                if (lever != null) {
                    "The largest thing you still control is ${lever.icon} " +
                        "${lever.name} at ${Cash.money(b.slice(lever))} a " +
                        "paycheck. Cutting it by " +
                        "${Cash.percent((perPaycheck / b.slice(lever)).coerceAtMost(1.0))} " +
                        "would close the gap on its own."
                } else {
                    "Every line here is a fixed bill, so the gap has to be closed " +
                        "by changing one of them or by earning more."
                }
        )
        b.biggestBill?.let { bill ->
            notes += Note(
                "🔍",
                "Start with the biggest bill",
                "${bill.icon} ${bill.name} is ${Cash.money(b.slice(bill))} of every " +
                    "paycheck, ${Cash.money(bill.perYear)} a year. Bills feel fixed " +
                    "and often are not: insurance, phone and internet are the three " +
                    "most people can cut in an afternoon by asking for a better rate."
            )
        }
    }

    // The 50/30/20 check, stated as a comparison rather than a rule, because
    // it is a guideline and reporting it as a pass mark is dishonest.
    if (b.anythingEntered) {
        val needs = b.needsRate
        notes += Note(
            "⚖️",
            "The 50 / 30 / 20 comparison",
            "A common guide is half your pay on needs, a fifth put away, and the " +
                "rest for the things you enjoy. Yours is " +
                "${Cash.percent(needs)} on needs and " +
                "${Cash.percent(b.savingRate)} kept. " +
                when {
                    needs > 0.70 ->
                        "Needs above 70% is tight, and it is usually housing rather " +
                            "than anything you did wrong."
                    needs > 0.50 ->
                        "A little over on needs is very normal and not a problem on " +
                            "its own."
                    else ->
                        "That leaves real room to move, which is a good position to " +
                            "be in."
                }
        )
    }

    val savingRate = b.savingRate
    if (b.paycheck > 0.0) {
        notes += when {
            savingRate >= 0.20 -> Note(
                "🌟",
                "You are keeping ${Cash.percent(savingRate)} of your pay",
                "That is at or above the usual target of a fifth. At this rate you " +
                    "put aside ${Cash.money(b.yearlySaved)} a year through savings, " +
                    "emergency fund and goals combined."
            )
            savingRate >= 0.10 -> Note(
                "🌱",
                "You are keeping ${Cash.percent(savingRate)} of your pay",
                "A solid start. Raising it by even " +
                    "${Cash.money(b.paycheck * 0.05)} a paycheck would put another " +
                    "${Cash.money(b.paycheck * 0.05 * PAYCHECKS_PER_YEAR)} away a " +
                    "year without changing anything you can feel."
            )
            else -> Note(
                "🪴",
                "Only ${Cash.percent(savingRate)} of your pay is being kept",
                "Nothing to feel bad about, and worth fixing slowly. Start with an " +
                    "amount so small you will not notice it going, and raise it " +
                    "each time your pay does."
            )
        }
    }

    // Each goal, with a date rather than a vague encouragement.
    b.categories.filter { it.enabled && it.hasGoal }.forEach { c ->
        val g = c.goal(c.perPaycheckOnAverage)
        val key = "goal-${c.id}"
        notes += when {
            g.reached -> Note(
                "🏆",
                "${c.name} is done",
                "${Cash.money(c.saved)} of ${Cash.money(c.target)}. Worth pointing " +
                    "whatever was feeding it at the next thing, because a finished " +
                    "goal quietly turns into spending money otherwise.",
                key = key
            )
            g.stalled -> Note(
                "⏸️",
                "${c.name} is not moving",
                "${Cash.money(g.remaining)} still to go and nothing going in from " +
                    "each paycheck. A target with no contribution behind it is a " +
                    "wish, not a plan.",
                key = key
            )
            else -> {
                val checks = g.paychecksNeeded ?: 0
                val years = checks / PAYCHECKS_PER_YEAR
                Note(
                    "🎯",
                    "${c.name}: ${Cash.percent(g.fraction)} of the way there",
                    "${Cash.money(c.saved)} of ${Cash.money(c.target)}, with " +
                        "${Cash.money(g.remaining)} to go. At " +
                        "${contribution(c)} that is $checks more " +
                        (if (checks == 1) "payday" else "paydays") +
                        ", landing around ${goalDate(checks)}" +
                        (if (years >= 1.0) ", about ${"%.1f".format(years)} years." else ".") +
                        " Adding ${contribution(c, 0.25)} would bring that forward.",
                    key = key
                )
            }
        }
    }

    // The emergency fund, sized against this person's actual outgoings.
    // Skipped entirely if they deleted the category: they made that decision
    // on purpose and an app that keeps bringing it up is nagging.
    val emergency = b.categories.firstOrNull { it.id == "emergency" && it.enabled }
    if (monthlyOutgoings > 0 && emergency != null) {
        val putting = emergency.perPaycheckOnAverage
        val already = emergency.saved
        val cover = monthlyOutgoings * 3

        // Measured against what is already in the pot. Telling someone with
        // five thousand saved that they need the full run from zero is both
        // wrong and discouraging, which is the worst combination.
        val progress = GoalProgress(saved = already, target = cover, perPaycheck = putting)

        notes += when {
            progress.reached -> Note(
                "🛟",
                "You have three months of cover",
                "${Cash.money(already)} put by against about " +
                    "${Cash.money(monthlyOutgoings)} a month of bills and living " +
                    "costs. That is the buffer that turns a car repair into an " +
                    "annoyance. Anything more can go to your other goals."
            )
            putting <= 0.0 -> Note(
                "🛟",
                "The emergency fund is the one to start",
                "Your bills and living costs come to about " +
                    "${Cash.money(monthlyOutgoings)} a month, so three months of " +
                    "cover is roughly ${Cash.money(cover)}." +
                    (if (already > 0) " You have ${Cash.money(already)} of that." else "") +
                    " It exists so a boiler or a car repair is annoying instead of " +
                    "a crisis, and it goes before every other savings goal."
            )
            else -> {
                val checks = progress.paychecksNeeded ?: 0
                val years = checks / PAYCHECKS_PER_YEAR
                Note(
                    "🛟",
                    "Three months of cover is ${Cash.money(cover)}",
                    (if (already > 0) "You have ${Cash.money(already)}, so " else "") +
                        "${Cash.money(progress.remaining)} to go. At " +
                        "${contribution(emergency)} that is about $checks " +
                        (if (checks == 1) "payday" else "paydays") +
                        (if (years >= 1.0) ", roughly ${"%.1f".format(years)} years" else "") +
                        ", landing around ${goalDate(checks)}. Once it is full, point " +
                        "that money at your goals instead."
                )
            }
        }
    }

    // The thing almost nobody budgets for, and the easiest win in the app.
    if (b.thirdPaydayIsWorth > 0.0) {
        notes += if (b.isThreePaydayMonth) {
            Note(
                "🎁",
                "$month is a three payday month",
                "Your monthly bills are covered by two paychecks, so the third one " +
                    "arrives with only its own groceries and fuel claiming it. That " +
                    "makes about ${Cash.money(b.thirdPaydayIsWorth)} genuinely spare " +
                    "this month. Decide where it goes before it decides for you, " +
                    "because an unplanned extra paycheck is the easiest money in the " +
                    "year to lose track of."
            )
        } else {
            Note(
                "📅",
                "Two paydays this month, so nothing spare",
                "Both paychecks are needed to cover the month's bills. " +
                    (if (threeMonths.isEmpty()) {
                        "Twice a year a month catches a third payday, and about " +
                            "${Cash.money(b.thirdPaydayIsWorth)} of it is genuinely spare."
                    } else {
                        "Your three payday months are $threeMonths, and about " +
                            "${Cash.money(b.thirdPaydayIsWorth)} of that extra check " +
                            "is genuinely spare. Worth deciding now where it goes."
                    })
            )
        }
    }

    if (b.verdict == Verdict.UNDER && b.leftOver > 0) {
        notes += Note(
            "🎯",
            "Give the leftover a job",
            "${Cash.money(b.leftOver)} spare is ${Cash.money(b.yearlyLeftOver)} a " +
                "year. Money without a name attached is the money that vanishes, so " +
                "send it somewhere on purpose, even if that somewhere is fun."
        )
    }

    if (b.verdict == Verdict.BALANCED) {
        notes += Note(
            "🎯",
            "Balanced to the penny",
            "Every penny has a job. Keep an eye on it though: a plan with no slack " +
                "in it turns into an overspend the first time a bill goes up."
        )
    }

    return notes
}
