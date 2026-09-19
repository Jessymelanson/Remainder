package com.remainder.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.remainder.app.data.Backup
import com.remainder.app.data.Budget
import com.remainder.app.data.Cadence
import com.remainder.app.data.Category
import com.remainder.app.data.Group
import com.remainder.app.data.Paydays
import com.remainder.app.data.Snapshot
import com.remainder.app.data.Store
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BudgetViewModel(app: Application) : AndroidViewModel(app) {

    data class State(
        val paycheck: Double = 0.0,
        val categories: List<Category> = emptyList(),
        val themeKey: String = "pink",
        val themeMode: Int = 1,
        /** Any real payday. Every other one is a fortnight from it. */
        val payday: LocalDate = LocalDate.now(),
        /** False while the app is still guessing the payday. */
        val paydaySet: Boolean = false,
        /** How many built in categories have been deleted, so they can be offered back. */
        val removedCount: Int = 0,
        /** The month being budgeted. Defaults to the one you are in. */
        val month: YearMonth = YearMonth.now(),
        /**
         * Bumped only by a reset. The paycheck field seeds its text from this
         * rather than from the number, because keying on the number means
         * typing a leading zero erases itself the instant it parses to zero.
         */
        val revision: Int = 0
    ) {
        /** The paydays that actually land in [month]. Two, or sometimes three. */
        val paydayDates: List<LocalDate> get() = Paydays.inMonth(month, payday)

        val paydaysThisMonth: Int get() = paydayDates.size.coerceAtLeast(1)

        /** Single source of every number on every screen. */
        val budget: Budget get() = Budget(paycheck, categories, paydaysThisMonth)

        val isThisMonth: Boolean get() = month == YearMonth.now()

        /** The months this year that catch a third payday, for planning ahead. */
        val threePaydayMonths: List<YearMonth>
            get() = Paydays.threePaydayMonths(month.year, payday)

        /** Savings pots that have a target set. */
        val goals: List<Category> get() = categories.filter { it.enabled && it.hasGoal }

        val totalSaved: Double get() = goals.sumOf { it.saved }
        val totalTarget: Double get() = goals.sumOf { it.target }

        /**
         * When the [paychecks]th payday from today lands.
         *
         * Counted in real paydays rather than by adding months, so the date it
         * gives is a day money actually arrives.
         */
        fun goalDate(paychecks: Int): LocalDate =
            Paydays.nth(payday, LocalDate.now(), paychecks)
    }

    private val store = Store(app)

    /** Distinguishes two categories added in the same millisecond. */
    private var nextCustom = 0

    private val _state = MutableStateFlow(
        State(
            paycheck = store.paycheck,
            categories = store.loadCategories(),
            themeKey = store.themeKey,
            themeMode = store.themeMode,
            payday = store.payday,
            paydaySet = store.paydaySet,
            removedCount = store.removedDefaults.size
        )
    )
    val state: StateFlow<State> = _state.asStateFlow()

    fun isOnboarded(): Boolean = store.onboarded

    fun markOnboarded() {
        store.onboarded = true
    }

    fun setPaycheck(value: Double) {
        store.paycheck = value
        _state.update { it.copy(paycheck = value) }
    }

    /** Everything the amount dialog can change, written once. */
    fun saveCategory(
        id: String,
        amount: Double,
        cadence: Cadence,
        saved: Double,
        target: Double
    ) = edit(id) {
        it.copy(amount = amount, cadence = cadence, saved = saved, target = target)
    }

    fun setAmount(id: String, amount: Double) = edit(id) { it.copy(amount = amount) }

    fun setCadence(id: String, cadence: Cadence) = edit(id) { it.copy(cadence = cadence) }

    fun setEnabled(id: String, enabled: Boolean) = edit(id) { it.copy(enabled = enabled) }

    fun rename(id: String, name: String, icon: String) =
        edit(id) { it.copy(name = name.trim().ifBlank { it.name }, icon = icon) }

    private fun edit(id: String, change: (Category) -> Category) {
        _state.update { s ->
            val next = s.categories.map { if (it.id == id) change(it) else it }
            store.saveCategories(next)
            s.copy(categories = next)
        }
    }

    fun addCustom(icon: String, name: String, group: Group, amount: Double, cadence: Cadence) {
        val clean = name.trim().ifBlank { "New category" }
        _state.update { s ->
            val next = s.categories + Category(
                // Unique by construction rather than by being fast enough. The
                // clock alone was very nearly safe - two taps inside one
                // millisecond is not a thing a person does - but "very nearly"
                // is doing real work here: two categories sharing an id edit
                // each other silently, because every write matches on id and
                // changes both. The counter costs nothing and removes the
                // argument.
                id = "c${System.currentTimeMillis()}-${nextCustom++}",
                icon = icon,
                name = clean,
                group = group,
                amount = amount,
                cadence = cadence,
                custom = true
            )
            store.saveCategories(next)
            s.copy(categories = next)
        }
    }

    /**
     * Removes a category outright, built in or not.
     *
     * A built in one also gets its id written to the removed list, because the
     * loader merges any missing default back in so that an app update can add
     * a category without losing anyone's data. Without the note, deleting the
     * mortgage line would appear to work and then quietly undo itself on the
     * next launch.
     *
     * Turning a category off is the other, gentler option and is still there.
     * That one keeps the amount so a paid off bill can come back later.
     */
    fun delete(id: String) {
        val current = _state.value
        val target = current.categories.firstOrNull { it.id == id } ?: return

        val next = current.categories.filterNot { it.id == id }
        if (!target.custom) {
            store.removedDefaults = store.removedDefaults + id
        }
        store.saveCategories(next)
        _state.update {
            it.copy(categories = next, removedCount = store.removedDefaults.size)
        }
    }

    /** Puts every deleted built in category back, empty and ready to fill in. */
    fun restoreDefaults() {
        store.removedDefaults = emptySet()
        val next = store.loadCategories()
        store.saveCategories(next)
        _state.update { it.copy(categories = next, removedCount = 0) }
    }

    fun setPayday(date: LocalDate) {
        store.payday = date
        _state.update { it.copy(payday = date, paydaySet = true) }
    }

    /** Steps to another month, so a three payday month can be seen coming. */
    fun stepMonth(by: Long) {
        _state.update { it.copy(month = it.month.plusMonths(by)) }
    }

    fun thisMonth() {
        _state.update { it.copy(month = YearMonth.now()) }
    }

    /** The whole budget as a JSON string, ready to be written to a file. */
    fun exportJson(): String {
        val s = _state.value
        return Backup.toJson(
            Snapshot(
                paycheck = s.paycheck,
                payday = s.payday,
                categories = s.categories,
                themeKey = s.themeKey,
                themeMode = s.themeMode
            )
        )
    }

    /**
     * Replaces the budget from a backup file. False if the file was not one.
     *
     * Nothing is written until the file has parsed completely, so picking the
     * wrong file leaves the current budget exactly as it was.
     */
    fun importJson(text: String): Boolean {
        val snapshot = Backup.parse(text) ?: return false

        store.paycheck = snapshot.paycheck
        store.payday = snapshot.payday
        store.saveCategories(snapshot.categories)
        store.themeKey = snapshot.themeKey
        store.themeMode = snapshot.themeMode

        // Any standard category the backup does not contain was deleted before
        // it was made, so it is marked deleted here too. Without this the
        // loader would merge those categories straight back in on the next
        // launch, and a restore would quietly hand back the very lines the
        // person had removed. It also has to be cleared for the ones the backup
        // does contain, or a deletion on this phone would outlive the restore.
        store.removedDefaults = Backup.removedDefaultIds(snapshot.categories)

        _state.update {
            it.copy(
                paycheck = snapshot.paycheck,
                payday = snapshot.payday,
                paydaySet = true,
                categories = snapshot.categories,
                removedCount = store.removedDefaults.size,
                themeKey = snapshot.themeKey,
                themeMode = snapshot.themeMode,
                revision = it.revision + 1
            )
        }
        return true
    }

    fun setTheme(key: String) {
        store.themeKey = key
        _state.update { it.copy(themeKey = key) }
    }

    fun setThemeMode(mode: Int) {
        store.themeMode = mode
        _state.update { it.copy(themeMode = mode) }
    }

    /** Clears every amount and puts the original ten categories back. */
    fun resetAmounts() {
        store.reset()
        _state.update {
            it.copy(
                paycheck = store.paycheck,
                categories = store.loadCategories(),
                removedCount = 0,
                revision = it.revision + 1
            )
        }
    }
}
