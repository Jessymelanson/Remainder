package com.remainder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remainder.app.data.Backup
import com.remainder.app.ui.AddCategoryDialog
import com.remainder.app.ui.AmountDialog
import com.remainder.app.ui.BreakdownScreen
import com.remainder.app.ui.BudgetScreen
import com.remainder.app.ui.BudgetViewModel
import com.remainder.app.ui.FileMessage
import com.remainder.app.ui.RemainderTheme
import com.remainder.app.ui.PaydayPickerDialog
import com.remainder.app.ui.monthName
import com.remainder.app.ui.monthYear
import com.remainder.app.ui.ThemeScreen
import com.remainder.app.ui.WelcomeScreen
import com.remainder.app.ui.paletteFor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // No enableEdgeToEdge(), matching every other app in the family.
        // Drawing behind transparent system bars is what left the regions
        // under them showing whatever the compositor still had there - the
        // previous screen, or the light pink launch background on a dark
        // theme - for a frame after a screen change.
        super.onCreate(savedInstanceState)
        setContent { Root() }
    }
}

private enum class Tab(val label: String) {
    Budget("Budget"), Breakdown("Breakdown")
}

private fun iconFor(tab: Tab): ImageVector = when (tab) {
    Tab.Budget -> Icons.Filled.AccountBalanceWallet
    Tab.Breakdown -> Icons.AutoMirrored.Filled.ListAlt
}

@Composable
private fun Root() {
    val vm: BudgetViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Mode 0 follows the phone, 1 forces light, 2 forces dark.
    val dark = when (state.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    RemainderTheme(palette = paletteFor(state.themeKey), dark = dark) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            var welcomed by remember { mutableStateOf(vm.isOnboarded()) }
            if (!welcomed) {
                WelcomeScreen(onContinue = {
                    vm.markOnboarded()
                    welcomed = true
                })
                return@Surface
            }

            // Saveable, so turning the phone does not throw someone out of
            // Settings or a half typed dialog: rotation rebuilds the activity,
            // and a plain remember goes with it.
            var tab by rememberSaveable { mutableStateOf(Tab.Budget) }
            var themeOpen by rememberSaveable { mutableStateOf(false) }
            var fileMessage by remember { mutableStateOf<FileMessage?>(null) }

            // The system file picker, so backups go wherever the user keeps
            // things and the app needs no storage permission to write one.
            val backUp = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                fileMessage = runCatching {
                    // "wt", not the default "w". Picking an existing backup to
                    // overwrite is the normal way to keep one file rather than a
                    // folder of dated ones, and plain "w" is not documented to
                    // truncate - most providers do not. A budget that has got
                    // smaller since the last backup then writes fewer bytes than
                    // the file already holds, the tail of the old JSON survives
                    // past the new closing brace, and the result is a file that
                    // looks fine, sits there for months, and refuses to restore.
                    context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        out.write(vm.exportJson().toByteArray())
                    } ?: error("could not open the file")
                    FileMessage("Backed up. Keep that file somewhere safe.", ok = true)
                }.getOrElse { FileMessage("Could not write the backup file.", ok = false) }
            }

            val restore = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri == null) return@rememberLauncherForActivityResult
                fileMessage = runCatching {
                    val text = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() }
                        ?: error("could not read the file")
                    if (vm.importJson(text)) {
                        FileMessage("Restored. Your budget is back.", ok = true)
                    } else {
                        FileMessage(
                            "That file is not a Remainder backup. Nothing was changed.",
                            ok = false
                        )
                    }
                }.getOrElse {
                    FileMessage("Could not read that file. Nothing was changed.", ok = false)
                }
            }
            var editing by rememberSaveable { mutableStateOf<String?>(null) }
            var adding by rememberSaveable { mutableStateOf(false) }
            var pickingPayday by rememberSaveable { mutableStateOf(false) }

            // The category being edited is held by id and looked up fresh each
            // time, so a dialog left open across a change is never editing a
            // stale copy of the row.
            val target = editing?.let { id -> state.categories.firstOrNull { it.id == id } }
            if (target != null) {
                AmountDialog(
                    category = target,
                    paydaysThisMonth = state.paydaysThisMonth,
                    monthLabel = monthName(state.month),
                    goalDate = { monthYear(state.goalDate(it)) },
                    onSave = { amount, cadence, saved, goal ->
                        vm.saveCategory(target.id, amount, cadence, saved, goal)
                        editing = null
                    },
                    onDelete = {
                        vm.delete(target.id)
                        editing = null
                    },
                    onSetEnabled = { vm.setEnabled(target.id, it) },
                    onDismiss = { editing = null }
                )
            }

            if (pickingPayday) {
                PaydayPickerDialog(
                    current = state.payday,
                    onPick = {
                        vm.setPayday(it)
                        pickingPayday = false
                    },
                    onDismiss = { pickingPayday = false }
                )
            }

            if (adding) {
                AddCategoryDialog(
                    onAdd = { icon, name, group, amount, cadence ->
                        vm.addCustom(icon, name, group, amount, cadence)
                        adding = false
                    },
                    onDismiss = { adding = false }
                )
            }

            BackHandler(enabled = themeOpen || tab != Tab.Budget) {
                if (themeOpen) themeOpen = false else tab = Tab.Budget
            }

            if (themeOpen) {
                ThemeScreen(
                    state = state,
                    onTheme = vm::setTheme,
                    onMode = vm::setThemeMode,
                    onReset = vm::resetAmounts,
                    onRestoreDefaults = vm::restoreDefaults,
                    onBackUp = {
                        fileMessage = null
                        backUp.launch(Backup.fileName())
                    },
                    onRestore = {
                        fileMessage = null
                        // Some file pickers will not offer a .json file under
                        // the json mime type, so plain text and "any file" are
                        // accepted too rather than showing an empty picker.
                        restore.launch(
                            arrayOf("application/json", "text/plain", "*/*")
                        )
                    },
                    fileMessage = fileMessage,
                    onBack = {
                        themeOpen = false
                        fileMessage = null
                    }
                )
                return@Surface
            }

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = tab == t,
                                onClick = { tab = t },
                                icon = { Icon(iconFor(t), contentDescription = null) },
                                label = { Text(t.label) }
                            )
                        }
                    }
                }
            ) { pad ->
                Box(Modifier.padding(bottom = pad.calculateBottomPadding())) {
                    when (tab) {
                        Tab.Budget -> BudgetScreen(
                            state = state,
                            onPaycheck = vm::setPaycheck,
                            onEdit = { editing = it.id },
                            onAdd = { adding = true },
                            onOpenTheme = { themeOpen = true },
                            onStepMonth = vm::stepMonth,
                            onThisMonth = vm::thisMonth,
                            onChangePayday = { pickingPayday = true },
                            goalDate = { monthYear(state.goalDate(it)) }
                        )

                        Tab.Breakdown -> BreakdownScreen(state)
                    }
                }
            }
        }
    }
}
