package com.remainder.app

import com.remainder.app.data.Cadence
import com.remainder.app.data.Category
import com.remainder.app.data.GoalProgress
import com.remainder.app.data.Group
import com.remainder.app.data.Paydays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Savings goals: how full a pot is, and when it finishes.
 *
 * The finish date is the part that has to be right. A wrong percentage is
 * cosmetic; a wrong date is someone planning around money that will not be
 * there.
 */
class GoalTest {

    private fun pot(saved: Double, target: Double, perCheck: Double) =
        GoalProgress(saved = saved, target = target, perPaycheck = perCheck)

    @Test
    fun `a twenty thousand goal reports what is left and how long it takes`() {
        // 4000 in, 20000 wanted, 200 a paycheck: 16000 to go, 80 paychecks.
        val g = pot(4000.0, 20000.0, 200.0)
        assertEquals(16000.0, g.remaining, 0.0001)
        assertEquals(0.2, g.fraction, 0.0001)
        assertEquals(80, g.paychecksNeeded)
        assertFalse(g.reached)
        assertFalse(g.stalled)
    }

    /**
     * Part of a paycheck still needs a whole paycheck to arrive, so the count
     * rounds up. Rounding down would promise the goal one payday early.
     */
    @Test
    fun `a part paycheck still counts as a whole one`() {
        assertEquals(3, pot(0.0, 250.0, 100.0).paychecksNeeded)
        assertEquals(1, pot(0.0, 1.0, 100.0).paychecksNeeded)
    }

    @Test
    fun `a finished goal is done rather than one paycheck away`() {
        val g = pot(20000.0, 20000.0, 200.0)
        assertTrue(g.reached)
        assertEquals(0, g.paychecksNeeded)
        assertEquals(0.0, g.remaining, 0.0001)
        assertEquals(1.0, g.fraction, 0.0001)
    }

    @Test
    fun `going past the target is still just finished`() {
        val g = pot(25000.0, 20000.0, 200.0)
        assertTrue(g.reached)
        assertEquals(0.0, g.remaining, 0.0001)
        // Never over 100%, because a bar cannot be more than full.
        assertEquals(1.0, g.fraction, 0.0001)
    }

    /**
     * Nothing going in means no date exists. Null says so, where a very large
     * number would render as a real looking year some time next century.
     */
    @Test
    fun `a goal with nothing going in has no finish date`() {
        val g = pot(4000.0, 20000.0, 0.0)
        assertNull(g.paychecksNeeded)
        assertTrue(g.stalled)
        assertEquals(16000.0, g.remaining, 0.0001)
    }

    @Test
    fun `no target means no goal at all`() {
        val g = pot(4000.0, 0.0, 200.0)
        assertNull(g.paychecksNeeded)
        assertFalse(g.reached)
        assertFalse(g.stalled)
        assertEquals(0.0, g.fraction, 0.0001)
    }

    // ---- Suggesting a contribution ----------------------------------------

    /**
     * A target with nothing going in used to be flagged and left there. The
     * amount needed is the whole question, and the app already knows it.
     */
    @Test
    fun `a stalled goal can say what would fix it`() {
        // 20000 over 52 paydays is 384.61, rounded up to a figure someone
        // would actually pick.
        assertEquals(385.0, pot(0.0, 20000.0, 0.0).perPaycheckToFinishIn(52), 0.0001)
        // Already part way there, so less is needed.
        assertEquals(310.0, pot(4000.0, 20000.0, 0.0).perPaycheckToFinishIn(52), 0.0001)
    }

    @Test
    fun `the suggestion rounds up so the tidy number still lands in time`() {
        val g = pot(0.0, 20000.0, 0.0)
        val suggested = g.perPaycheckToFinishIn(52)
        assertTrue("must not finish late", suggested * 52 >= g.remaining)
    }

    @Test
    fun `a finished goal needs nothing more`() {
        assertEquals(0.0, pot(20000.0, 20000.0, 0.0).perPaycheckToFinishIn(52), 0.0001)
        assertEquals(0.0, pot(0.0, 0.0, 0.0).perPaycheckToFinishIn(52), 0.0001)
    }

    @Test
    fun `asking for zero paydays does not divide by zero`() {
        assertEquals(0.0, pot(0.0, 20000.0, 0.0).perPaycheckToFinishIn(0), 0.0001)
    }

    @Test
    fun `an impossible goal is capped rather than overflowing`() {
        val g = pot(0.0, 1_000_000_000.0, 0.01)
        val checks = g.paychecksNeeded
        assertTrue("should be capped, was $checks", checks != null && checks > 0)
        assertEquals(2600, checks)
    }

    // ---- On a Category ----------------------------------------------------

    @Test
    fun `only savings categories can carry a goal`() {
        val savings = Category("s", "💰", "Savings", Group.SAVING, 200.0, Cadence.PER_PAYCHECK)
        val electric = Category("e", "💡", "Electric", Group.BILL, 120.0, Cadence.MONTHLY)
        assertTrue(savings.canHaveGoal)
        assertFalse(electric.canHaveGoal)

        assertTrue(savings.copy(target = 20000.0).hasGoal)
        // A target on a bill is ignored rather than displayed as a goal.
        assertFalse(electric.copy(target = 20000.0).hasGoal)
    }

    @Test
    fun `a savings category with no target is not a goal`() {
        val savings = Category("s", "💰", "Savings", Group.SAVING, 200.0, Cadence.PER_PAYCHECK)
        assertFalse(savings.hasGoal)
    }

    @Test
    fun `the goal reads its contribution from the paycheck slice`() {
        val savings = Category(
            "s", "💰", "Savings", Group.SAVING, 200.0, Cadence.PER_PAYCHECK,
            saved = 4000.0, target = 20000.0
        )
        assertEquals(80, savings.goal(savings.perPaycheck(2)).paychecksNeeded)
    }

    /**
     * A monthly contribution is split across the month's paydays like any
     * other monthly amount, so a three payday month puts in less per check but
     * the same over the month.
     */
    @Test
    fun `a monthly contribution follows the month's payday count`() {
        val savings = Category(
            "s", "💰", "Savings", Group.SAVING, 600.0, Cadence.MONTHLY,
            saved = 0.0, target = 6000.0
        )
        assertEquals(300.0, savings.perPaycheck(2), 0.0001)
        assertEquals(20, savings.goal(savings.perPaycheck(2)).paychecksNeeded)

        assertEquals(200.0, savings.perPaycheck(3), 0.0001)
        assertEquals(30, savings.goal(savings.perPaycheck(3)).paychecksNeeded)
    }

    // ---- Turning paychecks into a date ------------------------------------

    @Test
    fun `the finish date is a real payday, counted in fortnights`() {
        val anchor = LocalDate.of(2026, 1, 2)
        val from = LocalDate.of(2026, 1, 1)

        // The next payday is the second of January.
        assertEquals(LocalDate.of(2026, 1, 2), Paydays.nth(anchor, from, 1))
        // The eightieth is 79 fortnights after that.
        assertEquals(
            LocalDate.of(2026, 1, 2).plusDays(79 * 14),
            Paydays.nth(anchor, from, 80)
        )
        // Which is a Friday, like every other payday here.
        assertEquals(anchor.dayOfWeek, Paydays.nth(anchor, from, 80).dayOfWeek)
    }

    @Test
    fun `asking for a zero or negative payday gives the next one`() {
        val anchor = LocalDate.of(2026, 1, 2)
        val from = LocalDate.of(2026, 1, 1)
        assertEquals(Paydays.nth(anchor, from, 1), Paydays.nth(anchor, from, 0))
        assertEquals(Paydays.nth(anchor, from, 1), Paydays.nth(anchor, from, -5))
    }
}
