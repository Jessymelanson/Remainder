package com.remainder.app.data

import android.content.Context
import java.time.LocalDate
import org.json.JSONArray
import org.json.JSONObject

/**
 * Everything Remainder remembers, kept in this app's own private storage.
 *
 * Stored as JSON in a single preference rather than a column per category,
 * because the list is user editable and a fixed set of keys would not survive
 * the first custom category. Small enough that the whole thing is read and
 * written at once without anyone noticing.
 */
class Store(context: Context) {

    private val prefs = context.getSharedPreferences("remainder", Context.MODE_PRIVATE)

    // Stored as raw bits because SharedPreferences has no putDouble, and a
    // Float silently rounds amounts this app is supposed to get exactly right.
    var paycheck: Double
        get() = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_PAYCHECK, 0L))
        set(value) = prefs.edit()
            .putLong(KEY_PAYCHECK, java.lang.Double.doubleToRawLongBits(value))
            .apply()

    var themeKey: String
        get() = prefs.getString(KEY_THEME, null) ?: "pink"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    /** 0 follows the phone, 1 forces light, 2 forces dark. */
    var themeMode: Int
        get() = prefs.getInt(KEY_MODE, 1)
        set(value) = prefs.edit().putInt(KEY_MODE, value).apply()

    var onboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    /**
     * Any real payday. Every other one is this date plus or minus fortnights,
     * so which particular payday it is does not matter.
     */
    var payday: LocalDate
        get() {
            val stored = prefs.getLong(KEY_PAYDAY, 0L)
            return if (stored > 0L) LocalDate.ofEpochDay(stored) else Paydays.defaultAnchor()
        }
        set(value) = prefs.edit().putLong(KEY_PAYDAY, value.toEpochDay()).apply()

    /** False while the app is still guessing, so the UI can say so. */
    val paydaySet: Boolean get() = prefs.getLong(KEY_PAYDAY, 0L) > 0L

    /**
     * Built in categories the user has deleted.
     *
     * Without this the merge below would helpfully put them straight back on
     * the next launch, so deleting the mortgage line would appear to work and
     * then quietly undo itself.
     */
    var removedDefaults: Set<String>
        get() = prefs.getStringSet(KEY_REMOVED, emptySet())?.toSet() ?: emptySet()
        // Copied into a new set on the way in. SharedPreferences does not
        // promise anything about a set instance it has been handed, and
        // mutating one already stored is documented as undefined.
        set(value) = prefs.edit().putStringSet(KEY_REMOVED, value.toSet()).apply()

    fun loadCategories(): List<Category> {
        val raw = prefs.getString(KEY_CATEGORIES, null) ?: return defaultCategories()

        // Only a file that will not parse falls back to the defaults. An empty
        // list is a legitimate answer: it means the user deleted everything,
        // and handing them all ten back would be the app overruling them.
        val parsed = runCatching { parse(raw) }.getOrNull() ?: return defaultCategories()

        // A category added in a later version of the app is merged in rather
        // than lost, so an upgrade never silently drops a line from someone's
        // budget. Anything they already have keeps the amount they set, and
        // anything they deliberately deleted stays deleted.
        val known = parsed.mapTo(mutableSetOf()) { it.id }
        val gone = removedDefaults
        val missing = defaultCategories().filter { it.id !in known && it.id !in gone }
        return parsed + missing
    }

    fun saveCategories(categories: List<Category>) {
        val array = JSONArray()
        categories.forEach { c ->
            array.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("icon", c.icon)
                    put("name", c.name)
                    put("group", c.group.name)
                    put("amount", c.amount)
                    put("cadence", c.cadence.name)
                    put("saved", c.saved)
                    put("target", c.target)
                    put("enabled", c.enabled)
                    put("custom", c.custom)
                }
            )
        }
        prefs.edit().putString(KEY_CATEGORIES, array.toString()).apply()
    }

    private fun parse(raw: String): List<Category> {
        val array = JSONArray(raw)
        val out = mutableListOf<Category>()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val id = o.optString("id")
            if (id.isBlank()) continue

            val storedCadence = o.optString("cadence")
            val amount = o.optDouble("amount", 0.0)
                .takeIf { it.isFinite() && it >= 0 } ?: 0.0

            // Yearly used to be an option and is not any more. Falling through
            // to the default would turn a $260 annual renewal into $260 every
            // month, so it is converted rather than reinterpreted.
            val yearly = storedCadence == LEGACY_YEARLY

            out += Category(
                id = id,
                icon = o.optString("icon", "💖"),
                name = o.optString("name", id),
                group = runCatching { Group.valueOf(o.optString("group")) }
                    .getOrDefault(Group.LIVING),
                amount = if (yearly) amount / MONTHS_PER_YEAR else amount,
                cadence = runCatching { Cadence.valueOf(storedCadence) }
                    .getOrDefault(Cadence.MONTHLY),
                saved = o.optDouble("saved", 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0,
                target = o.optDouble("target", 0.0).takeIf { it.isFinite() && it >= 0 } ?: 0.0,
                enabled = o.optBoolean("enabled", true),
                custom = o.optBoolean("custom", false)
            )
        }
        return out
    }

    /**
     * Back to the ten built in categories, with every amount cleared.
     *
     * The removed list goes too, which is what makes this a genuine start over
     * rather than a start over minus whatever was deleted months ago. The
     * payday is deliberately kept: it is a fact about the job, not a budget
     * number, and asking for it again would be busywork.
     */
    fun reset() {
        prefs.edit()
            .remove(KEY_CATEGORIES)
            .remove(KEY_PAYCHECK)
            .remove(KEY_REMOVED)
            .apply()
    }

    private companion object {
        const val KEY_PAYCHECK = "paycheck"
        const val KEY_CATEGORIES = "categories"
        const val KEY_THEME = "theme"
        const val KEY_MODE = "mode"
        const val KEY_ONBOARDED = "onboarded"
        const val KEY_PAYDAY = "payday"
        const val KEY_REMOVED = "removedDefaults"

        /** Written by an earlier version that offered a yearly cadence. */
        const val LEGACY_YEARLY = "YEARLY"
    }
}
