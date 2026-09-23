@file:OptIn(ExperimentalMaterial3Api::class)

package com.remainder.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * The outcome of a backup or a restore.
 *
 * Typed rather than a bare string, because the screen was deciding whether to
 * colour it green or red by looking at the first word, and "Backed up" is not
 * "Restored", so a successful backup was reported in the error colour.
 */
data class FileMessage(val text: String, val ok: Boolean)

@Composable
fun ThemeScreen(
    state: BudgetViewModel.State,
    onTheme: (String) -> Unit,
    onMode: (Int) -> Unit,
    onReset: () -> Unit,
    onRestoreDefaults: () -> Unit,
    onBackUp: () -> Unit,
    onRestore: () -> Unit,
    /** Set after a backup or restore, so the screen can say what happened. */
    fileMessage: FileMessage?,
    onBack: () -> Unit
) {
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var confirmRestore by rememberSaveable { mutableStateOf(false) }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            icon = { Text("📥", style = MaterialTheme.typography.headlineMedium) },
            title = { Text("Restore from a backup?") },
            text = {
                Text(
                    "This replaces your paycheck, your payday, every category and " +
                        "every savings goal with what is in the file. What you have " +
                        "now is written over, so back it up first if you want to " +
                        "keep it."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    onRestore()
                }) { Text("Pick a file") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text("Cancel") }
            }
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            icon = { Text("🧹", style = MaterialTheme.typography.headlineMedium) },
            title = { Text("Start over?") },
            text = {
                Text(
                    "This clears your paycheck and every amount, including your " +
                        "savings balances and goals, and puts all ten of the " +
                        "original categories back. Anything you added yourself is " +
                        "removed. Your payday and your chosen theme are kept."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onReset()
                    confirmReset = false
                }) {
                    Text("Clear everything", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Keep it") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            item {
                Text(
                    "Pick a colour. It changes the whole app straight away, and " +
                        "green for money kept and red for money missing stay the " +
                        "same whichever you choose.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            for (palette in PALETTES) {
                item(key = palette.key) {
                    PaletteRow(
                        palette = palette,
                        selected = palette.key == state.themeKey,
                        onClick = { onTheme(palette.key) }
                    )
                }
            }

            item {
                SectionHeader("Light or dark")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        0 to "Follow phone",
                        1 to "Light",
                        2 to "Dark"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = state.themeMode == mode,
                            onClick = { onMode(mode) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            item {
                SectionHeader("Backup")
                Text(
                    "Remainder keeps your budget in its own private storage, which " +
                        "means uninstalling the app or losing the phone takes it " +
                        "with it. A backup is a plain file you can put wherever you " +
                        "keep things safe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VSpace(12)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onBackUp) { Text("Back up") }
                    OutlinedButton(onClick = { confirmRestore = true }) { Text("Restore") }
                }

                if (fileMessage != null) {
                    VSpace(10)
                    Text(
                        fileMessage.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (fileMessage.ok) Money.good() else MaterialTheme.colorScheme.error
                    )
                }

                VSpace(10)
                Text(
                    "The file is readable JSON, so the numbers stay yours even if " +
                        "this app is not around. Nothing is uploaded: the phone's " +
                        "own file picker decides where it goes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.removedCount > 0) {
                item {
                    SectionHeader("Deleted categories")
                    Text(
                        "You have deleted ${state.removedCount} of the standard " +
                            (if (state.removedCount == 1) "category" else "categories") +
                            ". They stay gone until you ask for them back, and " +
                            "they come back empty rather than with old amounts in " +
                            "them.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    VSpace(10)
                    OutlinedButton(onClick = onRestoreDefaults) {
                        Text("Bring them back")
                    }
                }
            }

            item {
                SectionHeader("Start over")
                Text(
                    "Clears your paycheck and every amount, and puts the original " +
                        "ten categories back.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VSpace(10)
                OutlinedButton(onClick = { confirmReset = true }) {
                    Text("Clear my budget", color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                SectionHeader("About")
                Text(
                    "Remainder 1.0.0. Everything is worked out on this phone and kept " +
                        "in the app's own storage. There is no account, no server, " +
                        "and no internet permission, so nothing can leave even by " +
                        "accident. A backup goes exactly where you put it and " +
                        "nowhere else.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VSpace(24)
            }
        }
    }
}

@Composable
private fun PaletteRow(palette: Palette, selected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            // The swatch is drawn from the palette's own colours rather than
            // the active theme, so you can see what you are choosing before
            // you choose it.
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(10.dp)
                    )
            ) {
                listOf(
                    palette.light.primary,
                    palette.light.primaryContainer,
                    palette.light.tertiary,
                    palette.dark.primary
                ).forEach { colour ->
                    Box(Modifier.size(width = 14.dp, height = 40.dp).background(colour))
                }
            }

            Box(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${palette.icon}  ${palette.name}",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    palette.blurb,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selected) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
