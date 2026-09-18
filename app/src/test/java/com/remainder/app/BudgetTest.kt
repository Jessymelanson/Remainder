package com.remainder.app

import com.remainder.app.data.Budget
import com.remainder.app.data.Cadence
import com.remainder.app.data.Category
import com.remainder.app.data.Group
import com.remainder.app.data.Verdict
import com.remainder.app.data.defaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The arithmetic, checked against numbers worked out by hand.
 *
 * This is a budgeting app, so a wrong total is not a cosmetic bug. Every
 * figure below was chosen to divide cleanly, which means a failure here is a
 * real mistake rather than a rounding argument.
 */
class BudgetTest {

    private fun bill(id: String, amount: Double, cadence: Cadence = Cadence.MONTHLY) =
        Category(id, "💡", id, Group.BILL, amount, cadence)

    private fun living(id: String, amount: Double) =
        Category(id, "🛒", id, Group.LIVING, amount, Cadence.PER_PAYCHECK)

    private fun saving(id: String, amount: Double) =
        Category(id, "💰", id, Group.SAVING, amount, Cadence.PER_PAYCHECK)

    // ---- Cadence ----------------------------------------------------------

    @Test
    fun `a monthly bill is split across the paydays that month actually has`() {
        val mortgage = bill("mortgage", 1200.0)
        assertEquals(600.0, mortgage.perPaycheck(2), 0.0001)
        assertEquals(400.0, mortgage.perPaycheck(3), 0.0001)
        // The yearly cost does not move. It is the same bill either way.
        assertEquals(14400.0, mortgage.perYear, 0.0001)
    }

    @Test
    fun `a per paycheck amount is taken as entered whatever the month holds`() {
        val g = living("groceries", 200.0)
        assertEquals(200.0, g.perPaycheck(2), 0.0001)
        assertEquals(200.0, g.perPaycheck(3), 0.0001)
        // But it is spent one more time in a three payday month.
        assertEquals(400.0, g.perMonth(2), 0.0001)
        assertEquals(600.0, g.perMonth(3), 0.0001)
        assertEquals(5200.0, g.perYear, 0.0001)
    }

    @Test
    fun `a monthly bill costs the same over a month however many paydays land`() {
        val mortgage = bill("mortgage", 1200.0)
        assertEquals(1200.0, mortgage.perMonth(2), 0.0001)
        assertEquals(1200.0, mortgage.perMonth(3), 0.0001)
    }

    @Test
    fun `a turned off category costs nothing`() {
        val off = bill("phone", 130.0).copy(enabled = false)
        assertEquals(0.0, off.perPaycheck(2), 0.0001)
        assertEquals(0.0, off.perMonth(2), 0.0001)
        assertEquals(0.0, off.perYear, 0.0001)
    }

    @Test
    fun `a nonsense payday count cannot divide by zero`() {
        assertEquals(1200.0, bill("mortgage", 1200.0).perPaycheck(0), 0.0001)
    }

    // ---- The worked example -----------------------------------------------

    /**
     * In an ordinary two payday month:
     *
     *   Paycheck               2000.00
     *   Mortgage 1200/month   - 600.00  ->  1400.00
     *   Phone      60/month   -  30.00  ->  1370.00
     *   Electric  120/month   -  60.00  ->  1310.00
     *   Groceries 200/check   - 200.00  ->  1110.00
     *   Gas       100/check   - 100.00  ->  1010.00
     *   Savings   100/check   - 100.00  ->   910.00
     *   Emergency  50/check   -  50.00  ->   860.00
     *   Goals      25/check   -  25.00  ->   835.00
     */
    private fun worked(paydays: Int) = Budget(
        paycheck = 2000.0,
        categories = listOf(
            bill("mortgage", 1200.0),
            bill("phone", 60.0),
            bill("electric", 120.0),
            living("groceries", 200.0),
            living("gas", 100.0),
            saving("savings", 100.0),
            saving("emergency", 50.0),
            saving("goals", 25.0)
        ),
        paydaysThisMonth = paydays
    )

    @Test
    fun `each stage takes the right amount and leaves the right balance`() {
        val b = worked(2)

        assertEquals(690.0, b.bills.took, 0.0001)
        assertEquals(1310.0, b.bills.leftAfter, 0.0001)

        assertEquals(300.0, b.living.took, 0.0001)
        assertEquals(1010.0, b.living.leftAfter, 0.0001)

        assertEquals(175.0, b.saving.took, 0.0001)
        assertEquals(835.0, b.saving.leftAfter, 0.0001)
    }

    /**
     * The point of the whole payday model. Same paycheck, same bills, but the
     * third payday carries a share of the monthly ones, so every check in that
     * month keeps more.
     */
    @Test
    fun `a third payday leaves more in every check that month`() {
        val two = worked(2)
        val three = worked(3)

        assertEquals(690.0, two.bills.took, 0.0001)
        assertEquals(460.0, three.bills.took, 0.0001)

        // Living costs and savings are per paycheck, so they do not move.
        assertEquals(three.living.took, two.living.took, 0.0001)
        assertEquals(three.saving.took, two.saving.took, 0.0001)

        assertEquals(835.0, two.leftOver, 0.0001)
        assertEquals(1065.0, three.leftOver, 0.0001)
        assertTrue(three.isThreePaydayMonth)
        assertFalse(two.isThreePaydayMonth)
    }

    @Test
    fun `the month totals follow the number of paydays`() {
        val two = worked(2)
        assertEquals(4000.0, two.monthIncome, 0.0001)
        assertEquals(2330.0, two.monthOut, 0.0001)
        assertEquals(1670.0, two.monthLeftOver, 0.0001)

        val three = worked(3)
        assertEquals(6000.0, three.monthIncome, 0.0001)
        assertEquals(2805.0, three.monthOut, 0.0001)
        assertEquals(3195.0, three.monthLeftOver, 0.0001)
    }

    /**
     * A third paycheck is not a whole extra paycheck of spare money. Its own
     * groceries and fuel still come out of it. Saying otherwise would be the
     * app talking someone into overspending.
     */
    @Test
    fun `a third payday is worth the paycheck less its own per check costs`() {
        // 2000 less groceries 200, gas 100, savings 100, emergency 50, goals 25.
        assertEquals(1525.0, worked(3).thirdPaydayIsWorth, 0.0001)
        // And it is the same figure whether or not this month is the lucky one.
        assertEquals(1525.0, worked(2).thirdPaydayIsWorth, 0.0001)
        // It matches the difference the extra payday actually makes.
        assertEquals(
            worked(3).monthLeftOver - worked(2).monthLeftOver,
            worked(2).thirdPaydayIsWorth,
            0.0001
        )
    }

    @Test
    fun `the extra never goes negative when per check costs exceed the pay`() {
        assertEquals(0.0, Budget(100.0, listOf(living("groceries", 500.0)), 3).thirdPaydayIsWorth, 0.0001)
    }

    @Test
    fun `a stage starts where the one before it finished`() {
        val b = worked(2)
        assertEquals(b.paycheck, b.bills.startedWith, 0.0001)
        assertEquals(b.bills.leftAfter, b.living.startedWith, 0.0001)
        assertEquals(b.living.leftAfter, b.saving.startedWith, 0.0001)
    }

    @Test
    fun `every step records the balance that survived it`() {
        val steps = worked(2).bills.steps
        assertEquals(3, steps.size)
        assertEquals(1400.0, steps[0].leftAfter, 0.0001)
        assertEquals(1370.0, steps[1].leftAfter, 0.0001)
        assertEquals(1310.0, steps[2].leftAfter, 0.0001)
    }

    @Test
    fun `left over and total out agree with the paycheck`() {
        val b = worked(2)
        assertEquals(835.0, b.leftOver, 0.0001)
        assertEquals(1165.0, b.totalOut, 0.0001)
        assertEquals(b.paycheck, b.totalOut + b.leftOver, 0.0001)
        assertEquals(Verdict.UNDER, b.verdict)
        assertEquals(0.0, b.shortfall, 0.0001)
    }

    // ---- Verdicts ---------------------------------------------------------

    @Test
    fun `no paycheck means no verdict rather than a false pass`() {
        val b = Budget(0.0, defaultCategories(), 2)
        assertEquals(Verdict.EMPTY, b.verdict)
        assertEquals(0.0, b.leftOver, 0.0001)
    }

    @Test
    fun `spending the paycheck exactly reads as balanced, not over`() {
        val b = Budget(1000.0, listOf(living("groceries", 1000.0)), 2)
        assertEquals(Verdict.BALANCED, b.verdict)
        assertEquals(0.0, b.shortfall, 0.0001)
    }

    /**
     * Splitting a monthly bill three ways does not land on a whole cent.
     * Without the tolerance this reads as over budget by a fraction of a
     * penny, which would be a lie told by floating point.
     */
    @Test
    fun `a rounding remainder does not read as over budget`() {
        val b = Budget(
            paycheck = 100.0,
            categories = listOf(bill("a", 100.0), bill("b", 100.0), bill("c", 100.0)),
            paydaysThisMonth = 3
        )
        assertTrue("left over should be a rounding remainder", Budget.isZero(b.leftOver))
        assertEquals(Verdict.BALANCED, b.verdict)
    }

    @Test
    fun `overspending reports the gap, not a negative left over dressed up`() {
        val b = Budget(1000.0, listOf(living("groceries", 800.0), saving("savings", 400.0)), 2)
        assertEquals(Verdict.OVER, b.verdict)
        assertEquals(-200.0, b.leftOver, 0.0001)
        assertEquals(200.0, b.shortfall, 0.0001)
    }

    // ---- Rates and shares -------------------------------------------------

    @Test
    fun `shares are measured against the paycheck`() {
        val b = worked(2)
        assertEquals(0.345, b.share(b.bills.took), 0.0001)
        assertEquals(0.15, b.share(b.living.took), 0.0001)
        assertEquals(0.0875, b.share(b.saving.took), 0.0001)
    }

    /**
     * Spending more than came in has to read as more than 100%. Capping it
     * would hide the one fact that matters most on the screen.
     */
    @Test
    fun `a share can exceed all of the paycheck`() {
        val b = Budget(1000.0, listOf(bill("mortgage", 2400.0)), 2)
        assertEquals(1.2, b.share(b.bills.took), 0.0001)
        assertEquals(1.2, b.share(b.totalOut), 0.0001)
    }

    @Test
    fun `share is zero when there is no paycheck to compare against`() {
        assertEquals(0.0, Budget(0.0, listOf(living("groceries", 50.0)), 2).share(50.0), 0.0001)
    }

    @Test
    fun `needs is bills plus living, and saving rate counts what is left over`() {
        val b = worked(2)
        assertEquals(0.495, b.needsRate, 0.0001)
        // 175 put away plus 835 not spent, over 2000.
        assertEquals(0.505, b.savingRate, 0.0001)
    }

    @Test
    fun `an overspend cannot produce a negative saving rate`() {
        assertEquals(0.0, Budget(1000.0, listOf(living("groceries", 1500.0)), 2).savingRate, 0.0001)
    }

    /**
     * The emergency fund is sized against a month of real outgoings, so the
     * per paycheck lines have to be counted once per payday and the monthly
     * ones exactly once.
     */
    @Test
    fun `monthly outgoings count bills once and living costs per payday`() {
        // Bills 1200 + 60 + 120 = 1380. Living 300 a check.
        assertEquals(1380.0 + 600.0, worked(2).monthlyOutgoings, 0.0001)
        assertEquals(1380.0 + 900.0, worked(3).monthlyOutgoings, 0.0001)
    }

    @Test
    fun `savings are not counted as an outgoing to be insured against`() {
        val b = Budget(2000.0, listOf(bill("rent", 1000.0), saving("goals", 500.0)), 2)
        assertEquals(1000.0, b.monthlyOutgoings, 0.0001)
    }

    // ---- Levers -----------------------------------------------------------

    @Test
    fun `the easiest cut is the biggest thing that is not a fixed bill`() {
        val b = worked(2)
        assertEquals("groceries", b.easiestCut?.id)
        assertEquals("mortgage", b.biggestBill?.id)
    }

    @Test
    fun `nothing is suggested to cut when only bills are set`() {
        assertNull(Budget(1000.0, listOf(bill("mortgage", 1300.0)), 2).easiestCut)
    }

    @Test
    fun `an empty budget knows it is empty`() {
        assertFalse(Budget(1000.0, defaultCategories(), 2).anythingEntered)
        assertTrue(worked(2).anythingEntered)
    }

    @Test
    fun `the ten asked for are all present and start at zero`() {
        val ids = defaultCategories().map { it.id }
        assertEquals(
            listOf(
                "phone", "car", "mortgage", "electric", "internet",
                "groceries", "gas", "savings", "emergency", "goals"
            ),
            ids
        )
        assertTrue(defaultCategories().all { it.amount == 0.0 && it.enabled })
    }
}
