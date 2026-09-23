@file:OptIn(ExperimentalMaterial3Api::class)

package com.remainder.app.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.remainder.app.data.Budget
import com.remainder.app.data.Cadence
import com.remainder.app.data.Category
import com.remainder.app.data.Group
import com.remainder.app.data.PAYCHECKS_PER_YEAR
import com.remainder.app.data.Stage
import com.remainder.app.data.Verdict

/** Two years of fortnightly paydays, used for suggesting a contribution. */
private const val TWO_YEARS_OF_PAYDAYS = 52

/** One colour per pass, used by the bar, the legend and the section headers. */
@Composable
fun groupColour(group: Group): Color = when (group) {
    Group.BILL -> MaterialTheme.colorScheme.primary
    Group.LIVING -> MaterialTheme.colorScheme.tertiary
    Group.SAVING -> Money.good()
}

@Composable
fun BudgetScreen(
    state: BudgetViewModel.State,
    onPaycheck: (Double) -> Unit,
    onEdit: (Category) -> Unit,
    onAdd: () -> Unit,
    onOpenTheme: () -> Unit,
    onStepMonth: (Long) -> Unit,
    onThisMonth: () -> Unit,
    onChangePayday: () -> Unit,
    goalDate: (Int) -> String
) {
    val budget = state.budget

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Remainder") },
                actions = {
                    IconButton(onClick = onOpenTheme) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { PaycheckCard(state.paycheck, state.revision, onPaycheck) }

            item {
                PaydayCard(
                    state = state,
                    onStepMonth = onStepMonth,
                    onThisMonth = onThisMonth,
                    onChangePayday = onChangePayday
                )
            }

            item { VerdictCard(budget, state) }

            Group.entries.forEach { group ->
                val stage = budget.stage(group)
                val rows = state.categories.filter { it.group == group }

                item(key = "head-${group.name}") {
                    SectionHeader(
                        "${group.icon}  ${group.label}",
                        trailing = Cash.money(stage.took)
                    )
                    Text(
                        group.blurb,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item(key = "card-${group.name}") {
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            rows.forEachIndexed { i, category ->
                                if (i > 0) {
                                    HorizontalDivider(
                                        Modifier.padding(start = 68.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                                CategoryRow(category, budget) { onEdit(category) }
                                if (category.hasGoal && category.enabled) {
                                    GoalStrip(category, budget, goalDate)
                                }
                            }
                            if (rows.isEmpty()) {
                                Text(
                                    "Nothing here yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }

                if (budget.paycheck > 0.0 && !stage.isEmpty) {
                    item(key = "left-${group.name}") { RunningTotal(group, stage) }
                }

                if (group == Group.SAVING && state.goals.isNotEmpty()) {
                    item(key = "goals-total") { GoalsTotal(state) }
                }
            }

            item {
                VSpace(14)
                OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Box(Modifier.size(8.dp))
                    Text("Add your own category")
                }
            }

            item {
                VSpace(10)
                Text(
                    "Everything stays on this phone. There is no account, nothing " +
                        "is uploaded, and Remainder has no permission to send it " +
                        "anywhere even if it wanted to.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VSpace(24)
            }
        }
    }
}

@Composable
private fun PaycheckCard(paycheck: Double, revision: Int, onPaycheck: (Double) -> Unit) {
    // Seeded once, and re-seeded only by a reset. What is typed stays exactly
    // as typed: keying this on the value would rewrite the field mid-entry,
    // and a leading zero would delete itself the moment it parsed to nothing.
    //
    // Saveable rather than remembered, because this card lives in a LazyColumn
    // item and a plain remember is thrown away twice over: once when the row
    // scrolls far enough off screen to be disposed, and again on rotation. Both
    // put back the reformatted parse instead of the characters typed, so a
    // half-entered "1250." came back as "1250" and a deliberate "0" came back
    // as an empty field.
    var text by rememberSaveable(revision) { mutableStateOf(Cash.editable(paycheck)) }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile("💵", background = MaterialTheme.colorScheme.surface)
                Box(Modifier.size(12.dp))
                Column {
                    Text("Your paycheck", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Take home pay, every two weeks",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            VSpace(12)
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    onPaycheck(Cash.parse(it))
                },
                placeholder = { Text("0.00") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.fillMaxWidth()
            )
            if (paycheck > 0.0) {
                VSpace(8)
                Text(
                    "That is ${Cash.money(paycheck * PAYCHECKS_PER_YEAR)} a year, " +
                        "across ${PAYCHECKS_PER_YEAR.toInt()} paychecks.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * The answer to the question the app exists to answer, at the top, in one
 * number, before any of the detail that explains it.
 */
@Composable
private fun VerdictCard(budget: Budget, state: BudgetViewModel.State) {
    val verdict = budget.verdict
    val tint = when (verdict) {
        Verdict.UNDER -> Money.good()
        Verdict.BALANCED -> Money.level()
        Verdict.OVER -> Money.bad()
        Verdict.EMPTY -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (verdict == Verdict.OVER) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(verdict.icon, style = MaterialTheme.typography.headlineMedium)
                Box(Modifier.size(10.dp))
                Text(verdict.label, style = MaterialTheme.typography.titleLarge, color = tint)
            }

            VSpace(6)
            Text(
                when (verdict) {
                    Verdict.EMPTY -> "-"
                    Verdict.OVER -> Cash.money(budget.shortfall) + " short"
                    else -> Cash.money(budget.leftOver) + " left"
                },
                style = MaterialTheme.typography.headlineLarge,
                color = tint
            )

            VSpace(4)
            Text(
                when (verdict) {
                    Verdict.EMPTY ->
                        "Put your paycheck in above and Remainder will do the rest."
                    Verdict.OVER ->
                        "This plan spends more than the paycheck holds. Something " +
                            "has to give, and it is better decided here than at " +
                            "the till."
                    Verdict.BALANCED ->
                        "Every penny has a job and none are left over. This is the " +
                            "target, not a problem."
                    Verdict.UNDER ->
                        "Spare after every bill, every shop and everything you set " +
                            "aside. Give it a job before it finds one."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // The per paycheck figure above is the one you spend against. This
            // is the one that makes a three payday month worth knowing about,
            // because the extra check is only visible across a whole month.
            if (budget.paycheck > 0.0 && verdict != Verdict.EMPTY) {
                // Short only when the verdict says so. A bare "< 0" disagreed
                // with it: splitting monthly bills three ways routinely leaves
                // a balanced plan 0.00000000000006 under, which printed as a red
                // "-$0.00 short" beneath "Balanced to the penny".
                val short = verdict == Verdict.OVER
                VSpace(10)
                Text(
                    "Across ${monthName(state.month)}'s " +
                        "${state.paydaysThisMonth} paydays: " +
                        Cash.money(budget.monthIncome) + " in, " +
                        Cash.money(budget.monthOut) + " out, " +
                        // Unsigned, as the headline is: "short" already says
                        // which way, and "-$200.00 short" reads as a surplus.
                        Cash.money(kotlin.math.abs(budget.monthLeftOver)) +
                        (if (short) " short." else " left."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (short) Money.bad() else Money.good()
                )
            }

            if (budget.paycheck > 0.0) {
                VSpace(14)
                // Each stage is drawn only as far as the paycheck actually
                // stretched, with the excess as one red slice on the end.
                // Drawing the full amounts and then adding the overspend would
                // count it twice and leave the bar disagreeing with itself.
                var room = budget.paycheck
                val fit = { amount: Double ->
                    val take = minOf(amount, room)
                    room -= take
                    take
                }
                ShareBar(
                    parts = listOf(
                        fit(budget.bills.took) to groupColour(Group.BILL),
                        fit(budget.living.took) to groupColour(Group.LIVING),
                        fit(budget.saving.took) to groupColour(Group.SAVING),
                        maxOf(budget.leftOver, 0.0) to MaterialTheme.colorScheme.outlineVariant
                    ),
                    over = budget.shortfall
                )
                VSpace(10)
                LegendRow(
                    groupColour(Group.BILL), "Bills",
                    Cash.money(budget.bills.took),
                    Cash.percent(budget.share(budget.bills.took))
                )
                LegendRow(
                    groupColour(Group.LIVING), "Living costs",
                    Cash.money(budget.living.took),
                    Cash.percent(budget.share(budget.living.took))
                )
                // "Savings" rather than the group's full "Savings and goals".
                // The percentage here is the slice of this paycheck, and next
                // to the word goals it reads as progress towards one, which is
                // a different number entirely and lives further down the page.
                LegendRow(
                    groupColour(Group.SAVING), "Savings",
                    Cash.money(budget.saving.took),
                    Cash.percent(budget.share(budget.saving.took))
                )
                val over = verdict == Verdict.OVER
                LegendRow(
                    if (over) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.outlineVariant,
                    if (over) "Over by" else "Left over",
                    Cash.money(if (over) budget.shortfall else budget.leftOver),
                    Cash.percent(budget.share(kotlin.math.abs(budget.leftOver)))
                )
                VSpace(4)
                Text(
                    "Percentages are the share of this one paycheck. Progress " +
                        "towards a savings goal is further down.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Every goal added up, so the pots read as one pile of savings. */
@Composable
private fun GoalsTotal(state: BudgetViewModel.State) {
    val fraction =
        if (state.totalTarget <= 0.0) 0.0 else state.totalSaved / state.totalTarget

    Card(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏆", style = MaterialTheme.typography.titleLarge)
                Box(Modifier.size(10.dp))
                Text("All your goals", style = MaterialTheme.typography.titleMedium)
            }
            VSpace(10)
            GoalBar(fraction)
            VSpace(8)
            Text(
                "${Cash.money(state.totalSaved)} saved of " +
                    "${Cash.money(state.totalTarget)} across " +
                    "${state.goals.size} " +
                    (if (state.goals.size == 1) "goal" else "goals") +
                    ", which is ${Cash.percent(fraction)} of the way there.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * A savings pot's progress towards what it is for.
 *
 * Sits under the row rather than in a screen of its own, because the whole
 * point is seeing the goal next to the amount that feeds it. Changing the
 * contribution and watching the finish date move is the thing that makes a
 * target feel reachable.
 */
@Composable
private fun GoalStrip(category: Category, budget: Budget, goalDate: (Int) -> String) {
    val progress = category.goal(category.perPaycheckOnAverage)

    Column(Modifier.padding(start = 68.dp, end = 14.dp, bottom = 12.dp)) {
        GoalBar(progress.fraction)
        VSpace(6)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${Cash.money(category.saved)} of ${Cash.money(category.target)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                Cash.percent(progress.fraction),
                style = MaterialTheme.typography.labelLarge,
                color = if (progress.reached) Money.good() else MaterialTheme.colorScheme.primary
            )
        }
        VSpace(3)
        Text(
            when {
                progress.reached -> "🎉  Goal reached"
                // A target with nothing behind it was only being flagged. It
                // now says what would fix it, because the amount needed is the
                // whole question and the app already knows the answer.
                progress.stalled ->
                    "${Cash.money(progress.remaining)} to go, and nothing is going " +
                        "in yet. About " +
                        Cash.money(progress.perPaycheckToFinishIn(TWO_YEARS_OF_PAYDAYS)) +
                        " a paycheck would get there in two years. Tap to set it."
                else -> {
                    val checks = progress.paychecksNeeded ?: 0
                    "${Cash.money(progress.remaining)} to go, about $checks more " +
                        (if (checks == 1) "paycheck" else "paychecks") +
                        ", around ${goalDate(checks)}"
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = when {
                progress.reached -> Money.good()
                progress.stalled -> Money.tight()
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun CategoryRow(category: Category, budget: Budget, onClick: () -> Unit) {
    val faded = !category.enabled
    val onSurface = if (faded) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconTile(
            category.icon,
            background = if (faded) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
        Box(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                category.name,
                style = MaterialTheme.typography.titleSmall,
                color = onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                when {
                    faded -> "Turned off"
                    category.amount <= 0.0 -> "Tap to set an amount"
                    category.cadence == Cadence.PER_PAYCHECK -> "Every paycheck"
                    else -> "${Cash.money(category.amount)} ${category.cadence.short}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(Modifier.size(8.dp))
        Text(
            if (category.enabled && category.amount > 0.0) {
                Cash.minus(budget.slice(category))
            } else {
                "-"
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (faded || category.amount <= 0.0) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                groupColour(category.group)
            }
        )
    }
}

/** The balance that survived one pass, so the arithmetic is visible as you go. */
@Composable
private fun RunningTotal(group: Group, stage: Stage) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            when (group) {
                Group.BILL -> "Left after bills"
                Group.LIVING -> "Left after living costs"
                Group.SAVING -> "Left after saving"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            Cash.money(stage.leftAfter),
            style = MaterialTheme.typography.titleSmall,
            color = if (stage.leftAfter < -Budget.EPSILON) Money.bad() else Money.good()
        )
    }
}
