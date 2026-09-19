package com.remainder.app

import com.remainder.app.data.Paydays
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Which calendar days a fortnightly wage actually lands on.
 *
 * 2026 is used throughout with an anchor of Friday 2 January, which makes the
 * whole year checkable by hand: paydays fall on 2, 16 and 30 January, then
 * every fourteen days after that.
 */
class PaydaysTest {

    private val anchor: LocalDate = LocalDate.of(2026, 1, 2)

    @Test
    fun `the anchor used by these tests really is a Friday`() {
        assertEquals(DayOfWeek.FRIDAY, anchor.dayOfWeek)
    }

    @Test
    fun `a month with an early first payday catches three`() {
        val january = Paydays.inMonth(YearMonth.of(2026, 1), anchor)
        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 2),
                LocalDate.of(2026, 1, 16),
                LocalDate.of(2026, 1, 30)
            ),
            january
        )
    }

    @Test
    fun `an ordinary month catches two`() {
        val february = Paydays.inMonth(YearMonth.of(2026, 2), anchor)
        assertEquals(
            listOf(LocalDate.of(2026, 2, 13), LocalDate.of(2026, 2, 27)),
            february
        )
    }

    @Test
    fun `every payday is a fortnight after the one before it`() {
        val year = (1..12).flatMap { Paydays.inMonth(YearMonth.of(2026, it), anchor) }
        year.zipWithNext { a, b ->
            assertEquals("consecutive paydays are 14 days apart", 14L, b.toEpochDay() - a.toEpochDay())
        }
    }

    @Test
    fun `a year holds 26 paydays across its twelve months`() {
        val total = (1..12).sumOf { Paydays.countIn(YearMonth.of(2026, it), anchor) }
        assertEquals(26, total)
    }

    @Test
    fun `exactly two months in the year catch a third payday`() {
        val three = Paydays.threePaydayMonths(2026, anchor)
        assertEquals(listOf(YearMonth.of(2026, 1), YearMonth.of(2026, 7)), three)
    }

    @Test
    fun `every month catches two paydays or three, never fewer`() {
        (1..12).forEach { m ->
            val count = Paydays.countIn(YearMonth.of(2026, m), anchor)
            assertTrue("month $m had $count paydays", count == 2 || count == 3)
        }
    }

    /**
     * The anchor is "any payday you can remember", so it has to work whether
     * the user picks one from last month or one coming up next week.
     */
    @Test
    fun `an anchor in the future gives the same answer as one in the past`() {
        val future = LocalDate.of(2030, 1, 4)
        val backdated = future.minusDays(14 * 104)
        assertEquals(
            Paydays.inMonth(YearMonth.of(2026, 6), future),
            Paydays.inMonth(YearMonth.of(2026, 6), backdated)
        )
    }

    @Test
    fun `the first payday on or after a date is found either side of the anchor`() {
        // The anchor itself, when the date asked about is the anchor.
        assertEquals(anchor, Paydays.onOrAfter(anchor, anchor))
        // A day later rolls on to the next one.
        assertEquals(
            LocalDate.of(2026, 1, 16),
            Paydays.onOrAfter(anchor, anchor.plusDays(1))
        )
        // A date before the anchor walks backwards, not forwards.
        assertEquals(
            LocalDate.of(2025, 12, 19),
            Paydays.onOrAfter(anchor, LocalDate.of(2025, 12, 8))
        )
    }

    @Test
    fun `February in a non leap year never catches three`() {
        // 28 days is exactly two fortnights, so a third can never fit.
        (1..28).forEach { day ->
            val a = LocalDate.of(2026, 2, day)
            assertEquals(2, Paydays.countIn(YearMonth.of(2026, 2), a))
        }
    }

    @Test
    fun `the default guess is a Friday on or before today`() {
        val guess = Paydays.defaultAnchor(LocalDate.of(2026, 9, 9))
        assertEquals(DayOfWeek.FRIDAY, guess.dayOfWeek)
        assertEquals(LocalDate.of(2026, 9, 4), guess)

        // And on a Friday it is today rather than a week ago.
        val friday = LocalDate.of(2026, 9, 11)
        assertEquals(friday, Paydays.defaultAnchor(friday))
    }
}
