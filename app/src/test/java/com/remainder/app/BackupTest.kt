package com.remainder.app

import com.remainder.app.data.Backup
import com.remainder.app.data.Cadence
import com.remainder.app.data.Category
import com.remainder.app.data.Group
import com.remainder.app.data.Snapshot
import com.remainder.app.data.defaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The backup file.
 *
 * Two things matter here and nothing else does. A backup has to come back
 * exactly as it went in, and a file that is not a backup has to be refused
 * outright rather than half applied over someone's real budget.
 */
class BackupTest {

    private val snapshot = Snapshot(
        paycheck = 2137.42,
        payday = LocalDate.of(2026, 1, 2),
        themeKey = "lavender",
        themeMode = 2,
        categories = listOf(
            Category("mortgage", "🏠", "Mortgage", Group.BILL, 1200.0, Cadence.MONTHLY),
            Category(
                "savings", "💰", "Savings", Group.SAVING, 200.0, Cadence.PER_PAYCHECK,
                saved = 4000.0, target = 20000.0
            ),
            Category(
                "c123", "🐷", "Piggy bank", Group.SAVING, 25.0, Cadence.PER_PAYCHECK,
                saved = 60.0, target = 500.0, enabled = false, custom = true
            )
        )
    )

    @Test
    fun `a backup comes back exactly as it went in`() {
        val restored = Backup.parse(Backup.toJson(snapshot))
        assertNotNull(restored)
        requireNotNull(restored)

        assertEquals(snapshot.paycheck, restored.paycheck, 0.0001)
        assertEquals(snapshot.payday, restored.payday)
        assertEquals(snapshot.themeKey, restored.themeKey)
        assertEquals(snapshot.themeMode, restored.themeMode)
        assertEquals(snapshot.categories, restored.categories)
    }

    @Test
    fun `savings goals survive the round trip`() {
        val restored = requireNotNull(Backup.parse(Backup.toJson(snapshot)))
        val savings = restored.categories.first { it.id == "savings" }
        assertEquals(4000.0, savings.saved, 0.0001)
        assertEquals(20000.0, savings.target, 0.0001)
        assertTrue(savings.hasGoal)
    }

    @Test
    fun `a turned off custom category keeps both of those facts`() {
        val restored = requireNotNull(Backup.parse(Backup.toJson(snapshot)))
        val piggy = restored.categories.first { it.id == "c123" }
        assertEquals(false, piggy.enabled)
        assertEquals(true, piggy.custom)
    }

    @Test
    fun `the default budget survives the round trip too`() {
        val plain = snapshot.copy(categories = defaultCategories())
        val restored = requireNotNull(Backup.parse(Backup.toJson(plain)))
        assertEquals(defaultCategories(), restored.categories)
    }

    // ---- Refusing things that are not backups -----------------------------

    @Test
    fun `nonsense is refused rather than parsed into an empty budget`() {
        assertNull(Backup.parse(""))
        assertNull(Backup.parse("not json at all"))
        assertNull(Backup.parse("[1, 2, 3]"))
    }

    @Test
    fun `some other app's json is refused`() {
        assertNull(Backup.parse("""{"app":"SomethingElse","categories":[]}"""))
    }

    @Test
    fun `a backup with no categories left in it is refused`() {
        assertNull(Backup.parse("""{"app":"Remainder","version":1,"categories":[]}"""))
    }

    /**
     * The app was called JBudget before it was called Remainder, and the file
     * format did not change with the name. A backup written by the old build is
     * a backup of this app, and refusing it would mean the rename quietly ate
     * every budget anyone had saved.
     */
    @Test
    fun `a backup written by JBudget still restores`() {
        val text = """
            {
              "app": "JBudget",
              "version": 1,
              "savedOn": "2026-09-01",
              "paycheck": 1840.5,
              "payday": "2026-09-04",
              "themeKey": "mint",
              "themeMode": 2,
              "categories": [
                {"id":"rent","icon":"🏠","name":"Rent","group":"BILL",
                 "amount":1300.0,"cadence":"MONTHLY","saved":0.0,"target":0.0,
                 "enabled":true,"custom":false}
              ]
            }
        """.trimIndent()

        val restored = requireNotNull(Backup.parse(text))
        assertEquals(1840.5, restored.paycheck, 0.0001)
        assertEquals("mint", restored.themeKey)
        assertEquals("Rent", restored.categories.single().name)
        assertEquals(1300.0, restored.categories.single().amount, 0.0001)
    }

    /**
     * Yearly was a cadence once, and a backup from then still exists.
     *
     * The loader in Store converts it - a yearly figure divided by twelve, kept
     * as monthly - and this reader did not, so the same file restored through
     * Settings turned a $260 annual renewal into $260 *every month*. Thirteen
     * times the real cost, applied without a word, inside the one feature whose
     * entire job is to be trusted with the numbers.
     *
     * Checked against a figure that divides cleanly, so a failure here is a
     * real mistake rather than an argument about rounding.
     */
    @Test
    fun `a yearly amount from an old backup restores as its monthly share`() {
        val text = """
            {
              "app": "JBudget",
              "version": 1,
              "paycheck": 1840.5,
              "payday": "2026-09-04",
              "categories": [
                {"id":"renewal","icon":"🚗","name":"Breakdown cover","group":"BILL",
                 "amount":240.0,"cadence":"YEARLY","saved":0.0,"target":0.0,
                 "enabled":true,"custom":true}
              ]
            }
        """.trimIndent()

        val restored = requireNotNull(Backup.parse(text))
        val category = restored.categories.single()

        assertEquals(Cadence.MONTHLY, category.cadence)
        assertEquals(20.0, category.amount, 0.0001)
        // And the yearly cost it came from is unchanged, which is the check
        // that the conversion went the right way round.
        assertEquals(240.0, category.perYear, 0.0001)
    }

    /**
     * A file that is a backup but has a broken field should still restore.
     * Refusing the whole thing over one bad number would lose a budget over a
     * typo.
     */
    @Test
    fun `a broken field falls back instead of losing the whole file`() {
        val text = """
            {
              "app": "Remainder",
              "version": 1,
              "paycheck": -50,
              "payday": "not-a-date",
              "themeKey": "",
              "themeMode": 99,
              "categories": [
                {"id":"x","name":"X","group":"NOPE","amount":"bad","cadence":"WEEKLY"}
              ]
            }
        """.trimIndent()

        val restored = requireNotNull(Backup.parse(text))
        assertEquals(0.0, restored.paycheck, 0.0001)
        assertEquals("pink", restored.themeKey)
        assertEquals(2, restored.themeMode)

        val only = restored.categories.single()
        assertEquals(Group.LIVING, only.group)
        assertEquals(Cadence.MONTHLY, only.cadence)
        assertEquals(0.0, only.amount, 0.0001)
    }

    /**
     * A backup made after deleting the mortgage line has to restore without
     * it. The loader merges missing defaults back in, so the restore has to
     * say which ones were deliberately gone or it hands them straight back.
     */
    @Test
    fun `defaults missing from a backup are recorded as deleted`() {
        val kept = defaultCategories().filterNot { it.id == "mortgage" || it.id == "car" }
        val removed = Backup.removedDefaultIds(kept)
        assertEquals(setOf("mortgage", "car"), removed)
    }

    @Test
    fun `a complete backup marks nothing as deleted`() {
        assertTrue(Backup.removedDefaultIds(defaultCategories()).isEmpty())
    }

    @Test
    fun `a category the user added does not count as a default`() {
        val mine = defaultCategories() + Category(
            "c1", "🐷", "Piggy", Group.SAVING, 10.0, Cadence.PER_PAYCHECK, custom = true
        )
        assertTrue(Backup.removedDefaultIds(mine).isEmpty())
    }

    @Test
    fun `deleting everything records every default as deleted`() {
        assertEquals(10, Backup.removedDefaultIds(emptyList()).size)
    }

    @Test
    fun `the file name carries the date it was made`() {
        assertEquals(
            "remainder-backup-2026-09-06.json",
            Backup.fileName(LocalDate.of(2026, 9, 6))
        )
    }
}
