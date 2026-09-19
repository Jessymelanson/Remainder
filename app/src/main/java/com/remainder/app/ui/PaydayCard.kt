@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.remainder.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Date patterns, resolved against the locale in force when they are used.
 *
 * Built per call rather than held in a `val`. A top-level `val` is initialised
 * once for the life of the process and captures whatever the locale was at that
 * instant, so changing the phone's language left every date on this screen in
 * the old one until the app was force stopped - which nobody does, and which
 * looks like the app simply not being translated. Dates are drawn a handful of
 * times per screen, so building the formatter each time costs nothing worth
 * measuring.
 */
private fun dayFormat(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d", Locale.getDefault())

private fun fullFormat(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.getDefault())

private fun monthFormat(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

/**
 * Which month is being budgeted, and how many paydays it actually catches.
 *
 * Being paid every fourteen days does not divide into calendar months, so most
 * months catch two paychecks and twice a year one catches three. That is not
 * trivia: in a three payday month the same monthly bills are covered by an
 * extra paycheck, so every check that month has more left in it. Averaging it
 * away over 26 paychecks would hide the one month in six that is genuinely
 * easier than the rest.
 */
@Composable
fun PaydayCard(
    state: BudgetViewModel.State,
    onStepMonth: (Long) -> Unit,
    onThisMonth: () -> Unit,
    onChangePayday: () -> Unit
) {
    val three = state.paydaysThisMonth >= 3

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (three) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(Modifier.padding(vertical = 12.dp)) {

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onStepMonth(-1) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                }
                Column(
                    Modifier.weight(1f).clickable(onClick = onThisMonth),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        state.month.format(monthFormat()),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        if (state.isThisMonth) "This month" else "Tap to come back to today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onStepMonth(1) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                }
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                VSpace(6)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (three) "🎉" else "📅", style = MaterialTheme.typography.titleLarge)
                    Box(Modifier.size(10.dp))
                    Text(
                        "${state.paydaysThisMonth} paydays" +
                            if (three) " this month" else "",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                VSpace(8)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.paydayDates.forEach { date ->
                        Text(
                            date.format(dayFormat()),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                if (state.paycheck > 0.0) {
                    VSpace(10)
                    Text(
                        if (three) {
                            "Three paychecks land this month, so your monthly bills " +
                                "are split three ways instead of two. Every check " +
                                "this month has more left in it."
                        } else {
                            "Two paychecks land this month, so each one covers half " +
                                "of every monthly bill."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                VSpace(12)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onChangePayday)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Paid every 2 weeks from " + state.payday.format(fullFormat()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!state.paydaySet) {
                            Text(
                                "A guess for now. Tap to set your real payday.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Money.tight()
                            )
                        }
                    }
                    Text(
                        "Change",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Picks any one real payday.
 *
 * Which one does not matter, because every other payday is that date plus or
 * minus a whole number of fortnights. Asking for "a payday" rather than "the
 * first payday of the year" is the difference between a question someone can
 * answer off the top of their head and one they have to go and look up.
 */
@Composable
fun PaydayPickerDialog(
    current: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        // The picker works in UTC, so it has to be read back in
                        // UTC. Reading it in the local zone shifts the date by a
                        // day for anyone west of Greenwich.
                        onPick(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        )
                    } else {
                        onDismiss()
                    }
                }
            ) { Text("Set payday") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        Column {
            Text(
                "Pick any payday",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp)
            )
            Text(
                "Recent or upcoming, it does not matter. Remainder counts " +
                    "fortnights from it to work out the rest.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp)
            )
            DatePicker(state = pickerState, title = null)
        }
    }
}

/** Month name on its own, for advice text elsewhere. */
fun monthName(month: YearMonth): String =
    month.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))

/** "March 2028", for a date a goal is projected to land on. */
fun monthYear(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
