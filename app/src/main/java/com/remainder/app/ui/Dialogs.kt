@file:OptIn(ExperimentalLayoutApi::class)

package com.remainder.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.remainder.app.data.Cadence
import com.remainder.app.data.Category
import com.remainder.app.data.Group
import com.remainder.app.data.ICON_CHOICES

/**
 * Edits one category.
 *
 * A dialog rather than an inline field on the list. Twenty live text fields on
 * one scrolling screen is a lot of state to keep straight, and on a phone the
 * keyboard covers most of them the moment you tap one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountDialog(
    category: Category,
    paydaysThisMonth: Int,
    monthLabel: String,
    /** Turns "so many more paychecks" into a month you can picture. */
    goalDate: (paychecks: Int) -> String,
    onSave: (amount: Double, cadence: Cadence, saved: Double, target: Double) -> Unit,
    onDelete: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf(Cash.editable(category.amount)) }
    var cadence by rememberSaveable { mutableStateOf(category.cadence) }
    var savedText by rememberSaveable { mutableStateOf(Cash.editable(category.saved)) }
    var targetText by rememberSaveable { mutableStateOf(Cash.editable(category.target)) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    // Deleting is the one thing here that cannot be undone by reopening the
    // dialog, so it asks. Everything else is just a number you can retype.
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { IconTile(category.icon) },
            title = { Text("Delete ${category.name}?") },
            text = {
                Text(
                    if (category.custom) {
                        "This removes the category and its amount for good."
                    } else {
                        "This removes it from your budget for good. You can bring " +
                            "the standard categories back from Settings if you " +
                            "change your mind."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep it") }
            }
        )
    }

    val amount = Cash.parse(text)
    val saved = Cash.parse(savedText)
    val target = Cash.parse(targetText)
    val preview = Category(
        id = category.id, icon = category.icon, name = category.name,
        group = category.group, amount = amount, cadence = cadence,
        saved = saved, target = target
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { IconTile(category.icon) },
        title = { Text(category.name) },
        text = {
            // Scrollable, because for a savings category this is an amount
            // field, two chips, a conversion note, a switch with two lines of
            // explanation, a heading, two more fields, a progress bar and a
            // sentence about the finish date. M3 gives the text slot a bounded
            // height and no scrolling of its own, so on a short screen - or any
            // screen with the keyboard up - the goal fields and the Save button
            // were simply not reachable.
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Amount") },
                    prefix = { Text(Cash.money(0.0).takeWhile { !it.isDigit() }.trim()) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                VSpace(14)
                Text("How often is this charged?", style = MaterialTheme.typography.labelLarge)
                VSpace(6)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Cadence.entries.forEach { c ->
                        FilterChip(
                            selected = cadence == c,
                            onClick = { cadence = c },
                            label = { Text(c.short) }
                        )
                    }
                }

                // The whole reason cadence exists. Someone entering a monthly
                // mortgage against a fortnightly wage needs to see the number
                // that will actually come out, not be left to trust it. The
                // month is named because the answer genuinely changes: the
                // same bill is split two ways in most months and three in some.
                if (preview.converted) {
                    VSpace(12)
                    Text(
                        "Split across $monthLabel's $paydaysThisMonth paychecks " +
                            "that is ${Cash.money(preview.perPaycheck(paydaysThisMonth))} " +
                            "out of each one, or ${Cash.money(preview.perYear)} a year.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                VSpace(16)
                HorizontalDivider()
                VSpace(12)
                EnableRow(category.enabled, onSetEnabled)

                // Only for money you are keeping. A goal on the electric bill
                // would be meaningless, so the fields are not offered there.
                if (category.canHaveGoal) {
                    VSpace(18)
                    HorizontalDivider()
                    VSpace(14)
                    Text("Savings goal", style = MaterialTheme.typography.titleSmall)
                    VSpace(2)
                    Text(
                        "Optional. Set a target and Remainder works out when you " +
                            "get there at this rate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    VSpace(10)
                    OutlinedTextField(
                        value = savedText,
                        onValueChange = { savedText = it },
                        label = { Text("Already in this pot") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    VSpace(10)
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text("Goal, for example 20000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val progress = preview.goal(preview.perPaycheckOnAverage)
                    if (target > 0.0) {
                        VSpace(12)
                        GoalBar(progress.fraction)
                        VSpace(8)
                        Text(
                            when {
                                progress.reached ->
                                    "Goal reached. ${Cash.money(saved)} of " +
                                        "${Cash.money(target)}."
                                progress.stalled ->
                                    "${Cash.money(progress.remaining)} to go. Put " +
                                        "an amount at the top and Remainder will work " +
                                        "out the date. About " +
                                        Cash.money(progress.perPaycheckToFinishIn(52)) +
                                        " a paycheck would get there in two years."
                                else -> {
                                    val checks = progress.paychecksNeeded ?: 0
                                    "${Cash.money(progress.remaining)} to go. About " +
                                        "$checks more " +
                                        (if (checks == 1) "paycheck" else "paychecks") +
                                        ", so around ${goalDate(checks)}."
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (progress.stalled) {
                                Money.tight()
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(amount, cadence, saved, target) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                // Delete is offered for every category now, built in or not.
                // Hiding it behind "turn off" for the standard ten meant the
                // list could only ever grow, and a mortgage line on a phone
                // belonging to someone who rents is just clutter.
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

/**
 * The gentler alternative to deleting, offered inside the dialog.
 *
 * Turning a category off keeps the amount, which is what you want for a bill
 * that is paid off for now. Deleting is for a category that was never yours.
 */
@Composable
private fun EnableRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (enabled) "Counted in your budget" else "Not counted just now",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                if (enabled) {
                    "Turn it off to stop it coming out, keeping the amount."
                } else {
                    "The amount is kept, it is just not being deducted."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

/** Adds a category of your own, with an emoji to go with it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(
    onAdd: (icon: String, name: String, group: Group, amount: Double, cadence: Cadence) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf(ICON_CHOICES.first()) }
    var group by rememberSaveable { mutableStateOf(Group.BILL) }
    var cadence by rememberSaveable { mutableStateOf(Cadence.MONTHLY) }
    var text by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { IconTile(icon) },
        title = { Text("Add a category") },
        text = {
            // Same reason as the amount dialog above. The icon grid keeps its
            // own bounded height, so it stays measurable inside a scrolling
            // parent rather than asking for infinite space.
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                VSpace(10)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                VSpace(14)
                Text("Which part of the paycheck?", style = MaterialTheme.typography.labelLarge)
                VSpace(6)
                // FlowRow, not Row. Three chips do not fit across a dialog
                // on a phone, and a Row does not wrap: it squeezes the last
                // chip until its label runs one letter per line.
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Group.entries.forEach { g ->
                        FilterChip(
                            selected = group == g,
                            onClick = { group = g },
                            label = { Text("${g.icon} ${g.short}") }
                        )
                    }
                }

                VSpace(12)
                Text("How often?", style = MaterialTheme.typography.labelLarge)
                VSpace(6)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Cadence.entries.forEach { c ->
                        FilterChip(
                            selected = cadence == c,
                            onClick = { cadence = c },
                            label = { Text(c.short) }
                        )
                    }
                }

                VSpace(14)
                Text("Pick an icon", style = MaterialTheme.typography.labelLarge)
                VSpace(6)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp)
                ) {
                    items(ICON_CHOICES) { choice ->
                        Column(
                            Modifier
                                .padding(3.dp)
                                .clickable { icon = choice },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            IconTile(
                                choice,
                                size = 32,
                                background = if (choice == icon) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(icon, name, group, Cash.parse(text), cadence) },
                enabled = name.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
