package com.remainder.app

import com.remainder.app.data.Budget
import com.remainder.app.data.Cadence
import com.remainder.app.data.Category
import com.remainder.app.data.Group
import com.remainder.app.ui.advice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The advice cards, and the one thing about them that can take the tab down.
 *
 * Each card is an item in a LazyColumn with a key, and a LazyColumn key has to
 * be unique — a repeat is not a rendering glitch, it is an exception thrown
 * during layout, so the Breakdown tab does not draw at all. The keys used to be
 * the card titles, and a card about a savings goal is titled after the
 * category: "Savings is not moving". Nothing stops somebody having two
 * categories called Savings, and adding one is four taps.
 *
 * So the rule under test is simply that no two notes ever share a key, and it
 * is checked against the arrangements that used to break it rather than against
 * a tidy budget that never would.
 */
class AdviceKeyTest {

    private fun saving(id: String, name: String, amount: Double, saved: Double, target: Double) =
        Category(
            id = id,
            icon = "💰",
            name = name,
            group = Group.SAVING,
            amount = amount,
            cadence = Cadence.PER_PAYCHECK,
            saved = saved,
            target = target,
            custom = true
        )

    private fun notesFor(categories: List<Category>) =
        advice(
            b = Budget(paycheck = 2_000.0, categories = categories, paydaysThisMonth = 2),
            month = "March",
            threeMonths = "January and July",
            goalDate = { "March 2029" }
        )

    private fun assertKeysUnique(categories: List<Category>, why: String) {
        val keys = notesFor(categories).map { it.key }
        val repeated = keys.groupingBy { it }.eachCount().filterValues { it > 1 }
        assertTrue("$why — repeated keys: $repeated", repeated.isEmpty())
        assertEquals(why, keys.size, keys.toSet().size)
    }

    /**
     * The crash exactly as it arrived: two goals, same name, same state.
     *
     * Both produce the card "Savings is not moving", and keyed on that title
     * they are one key claimed twice.
     */
    @Test
    fun `two stalled goals with the same name do not collide`() {
        assertKeysUnique(
            listOf(
                saving("a", "Savings", amount = 0.0, saved = 100.0, target = 5_000.0),
                saving("b", "Savings", amount = 0.0, saved = 200.0, target = 9_000.0)
            ),
            "two stalled goals sharing a name"
        )
    }

    /** The same, finished rather than stalled: "Savings is done", twice. */
    @Test
    fun `two finished goals with the same name do not collide`() {
        assertKeysUnique(
            listOf(
                saving("a", "Holiday", amount = 50.0, saved = 5_000.0, target = 5_000.0),
                saving("b", "Holiday", amount = 50.0, saved = 900.0, target = 900.0)
            ),
            "two finished goals sharing a name"
        )
    }

    /**
     * And the nastiest of the three, because the title carries a percentage and
     * looks as though it would differ. Two goals at the same fraction round to
     * the same number, so "Car: 50% of the way there" is produced twice.
     */
    @Test
    fun `two goals at the same percentage do not collide`() {
        assertKeysUnique(
            listOf(
                saving("a", "Car", amount = 100.0, saved = 500.0, target = 1_000.0),
                saving("b", "Car", amount = 100.0, saved = 2_000.0, target = 4_000.0)
            ),
            "two goals at the same percentage sharing a name"
        )
    }

    /**
     * A rename must not change a card's identity.
     *
     * Keys are the category id for exactly this reason: keyed on the title, a
     * rename looks to the list like the old card being removed and a different
     * one arriving, which throws away its scroll and animation state mid-edit.
     */
    @Test
    fun `renaming a category does not change its card's key`() {
        val before = notesFor(
            listOf(saving("a", "Holiday", amount = 40.0, saved = 100.0, target = 2_000.0))
        )
        val after = notesFor(
            listOf(saving("a", "Big trip", amount = 40.0, saved = 100.0, target = 2_000.0))
        )

        val goalKey = before.map { it.key }.first { it.startsWith("goal-") }
        assertTrue("the card should keep its key across a rename", goalKey in after.map { it.key })
        assertEquals("goal-a", goalKey)
    }

    /** The ordinary case, so the rule is known to hold on a real budget too. */
    @Test
    fun `a full budget produces no repeated keys`() {
        val categories = com.remainder.app.data.defaultCategories().map {
            when (it.group) {
                Group.BILL -> it.copy(amount = 200.0)
                Group.LIVING -> it.copy(amount = 150.0)
                Group.SAVING -> it.copy(amount = 100.0, saved = 400.0, target = 4_000.0)
            }
        }
        assertKeysUnique(categories, "the ten standard categories, all in use")
    }

    /**
     * Over budget adds two more cards and is the longest the list ever gets, so
     * it is the arrangement most likely to collide by accident.
     */
    @Test
    fun `an over-budget plan produces no repeated keys`() {
        val categories = com.remainder.app.data.defaultCategories().map {
            when (it.group) {
                Group.BILL -> it.copy(amount = 2_000.0)
                Group.LIVING -> it.copy(amount = 400.0)
                Group.SAVING -> it.copy(amount = 200.0, saved = 50.0, target = 10_000.0)
            }
        }
        val budget = Budget(paycheck = 1_000.0, categories = categories, paydaysThisMonth = 2)
        assertTrue("this arrangement should be over budget", budget.shortfall > 0.0)
        assertKeysUnique(categories, "an over-budget plan")
    }
}
