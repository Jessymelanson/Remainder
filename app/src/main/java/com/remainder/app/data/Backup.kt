package com.remainder.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Everything a budget is, as one file.
 *
 * Remainder keeps its data in app private storage, which means uninstalling the
 * app takes the budget with it and a new phone starts from nothing. That is the
 * right default for something holding your wages, but it makes a way out
 * essential rather than a nicety.
 *
 * The file is plain JSON on purpose. It is readable, it can be opened in
 * anything, and if this app ever disappears the numbers are still yours.
 */
data class Snapshot(
    val paycheck: Double,
    val payday: LocalDate,
    val categories: List<Category>,
    val themeKey: String,
    val themeMode: Int
)

object Backup {

    /** Bumped only if the shape changes in a way a reader has to know about. */
    const val VERSION = 1

    /**
     * The names this app has gone by, all of which are still its own backups.
     *
     * It shipped as JBudget before it was called Remainder. The file format did
     * not change with the name, so a backup written by the old one is a backup
     * of this app - and refusing it would mean the rename quietly ate every
     * budget anyone had saved. The tag is a guard against someone picking the
     * wrong file out of their downloads, not a claim about which build wrote
     * it.
     */
    private val KNOWN_APP_NAMES = setOf("Remainder", "JBudget")

    fun fileName(today: LocalDate = LocalDate.now()): String = "remainder-backup-$today.json"

    fun toJson(snapshot: Snapshot): String {
        val categories = JSONArray()
        snapshot.categories.forEach { c ->
            categories.put(
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

        return JSONObject().apply {
            put("app", "Remainder")
            put("version", VERSION)
            put("savedOn", LocalDate.now().toString())
            put("paycheck", snapshot.paycheck)
            put("payday", snapshot.payday.toString())
            put("themeKey", snapshot.themeKey)
            put("themeMode", snapshot.themeMode)
            put("categories", categories)
        }.toString(2)
    }

    /**
     * Reads a backup, or returns null if it is not one.
     *
     * Null rather than a half restored budget. Someone picking the wrong file
     * out of their downloads must get told, not left with a mangled budget and
     * no idea which numbers survived. Anything unreadable inside an otherwise
     * valid file falls back to a sane value rather than throwing the lot away.
     */
    fun parse(text: String): Snapshot? {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: return null
        if (root.optString("app") !in KNOWN_APP_NAMES) return null

        val array = root.optJSONArray("categories") ?: return null
        val categories = mutableListOf<Category>()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val id = o.optString("id")
            if (id.isBlank()) continue

            // Yearly was an option in an earlier version and is not any more.
            // The loader in Store converts it; this did not, so a backup made
            // by that version restored a $260 annual renewal as $260 *every
            // month* - a thirteenfold error, applied silently, to a file whose
            // entire purpose is being trusted. The two readers now agree.
            val storedCadence = o.optString("cadence")
            val yearly = storedCadence == LEGACY_YEARLY
            val amount = positive(o.optDouble("amount", 0.0))

            categories += Category(
                id = id,
                icon = o.optString("icon", "💖"),
                name = o.optString("name", id),
                group = runCatching { Group.valueOf(o.optString("group")) }
                    .getOrDefault(Group.LIVING),
                amount = if (yearly) amount / MONTHS_PER_YEAR else amount,
                cadence = runCatching { Cadence.valueOf(storedCadence) }
                    .getOrDefault(Cadence.MONTHLY),
                saved = positive(o.optDouble("saved", 0.0)),
                target = positive(o.optDouble("target", 0.0)),
                enabled = o.optBoolean("enabled", true),
                custom = o.optBoolean("custom", false)
            )
        }
        // Refused, even though Store treats an empty list as a legitimate thing
        // to *hold* - somebody who deleted all ten categories has a budget of
        // none, and handing them back would be overruling them. Reading a file
        // is not the same question as reading our own storage: a truncated
        // download and a deliberately emptied budget arrive here identical, and
        // between wrongly refusing a file almost nobody has and silently wiping
        // a budget somebody does, refusing is the cheap mistake. The message
        // shown for it is the part worth improving, not this.
        if (categories.isEmpty()) return null

        return Snapshot(
            paycheck = positive(root.optDouble("paycheck", 0.0)),
            payday = runCatching { LocalDate.parse(root.optString("payday")) }
                .getOrElse { Paydays.defaultAnchor() },
            categories = categories,
            themeKey = root.optString("themeKey").ifBlank { "pink" },
            themeMode = root.optInt("themeMode", 1).coerceIn(0, 2)
        )
    }

    /**
     * The standard categories a restored budget does not contain.
     *
     * These have to be recorded as deleted, because the loader merges any
     * missing default back in so an app update can add a category without
     * losing data. Without this a restore would quietly hand back the very
     * lines the person had removed before making the backup.
     */
    fun removedDefaultIds(categories: List<Category>): Set<String> {
        val present = categories.mapTo(mutableSetOf()) { it.id }
        return defaultCategories().map { it.id }.filterNot { it in present }.toSet()
    }

    private fun positive(value: Double): Double =
        if (value.isFinite() && value >= 0) value else 0.0

    /** Written by an earlier version that offered a yearly cadence. */
    private const val LEGACY_YEARLY = "YEARLY"
}
