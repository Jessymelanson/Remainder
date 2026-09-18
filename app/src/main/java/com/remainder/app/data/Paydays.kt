package com.remainder.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Days between paychecks. Bi-weekly, so a fortnight. */
const val PAY_PERIOD_DAYS = 14L

/**
 * Which calendar days you actually get paid on, and how many land in a month.
 *
 * This is the difference between a budget that is roughly right on average and
 * one that is exactly right this month. Being paid every fourteen days does not
 * line up with calendar months: most months catch two paydays, and twice a year
 * a month catches three. Averaging that away over 26 paychecks hides the single
 * most useful fact a fortnightly earner has, which is that some months are
 * genuinely easier than others.
 *
 * Everything here works from one anchor date, any real payday. Every other
 * payday is that date plus or minus a whole number of fortnights.
 */
object Paydays {

    /**
     * A sensible starting guess: the most recent Friday.
     *
     * Used only until the user sets their own, so the app is useful before it
     * has asked for anything. Friday is the most common payday, and being
     * wrong here is visible and one tap to fix.
     */
    fun defaultAnchor(today: LocalDate = LocalDate.now()): LocalDate =
        if (today.dayOfWeek == DayOfWeek.FRIDAY) {
            today
        } else {
            today.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY))
        }

    /** The first payday falling on or after [from]. */
    fun onOrAfter(anchor: LocalDate, from: LocalDate): LocalDate {
        val gap = ChronoUnit.DAYS.between(anchor, from)
        // Ceiling division that behaves for negatives too, so an anchor in the
        // future works exactly as well as one in the past.
        val strides = Math.floorDiv(gap + PAY_PERIOD_DAYS - 1, PAY_PERIOD_DAYS)
        return anchor.plusDays(strides * PAY_PERIOD_DAYS)
    }

    /** Every payday that lands inside [month]. Two of them, or sometimes three. */
    fun inMonth(month: YearMonth, anchor: LocalDate): List<LocalDate> {
        val first = month.atDay(1)
        val last = month.atEndOfMonth()
        val out = mutableListOf<LocalDate>()
        var day = onOrAfter(anchor, first)
        while (!day.isAfter(last)) {
            out += day
            day = day.plusDays(PAY_PERIOD_DAYS)
        }
        return out
    }

    fun countIn(month: YearMonth, anchor: LocalDate): Int = inMonth(month, anchor).size

    /**
     * The date of the [n]th payday on or after [from]. The next one is n = 1.
     *
     * This is what turns "160 more paychecks" into "around March 2032", which
     * is the difference between a number and something a person can picture.
     */
    fun nth(anchor: LocalDate, from: LocalDate, n: Int): LocalDate =
        onOrAfter(anchor, from).plusDays((n.coerceAtLeast(1) - 1) * PAY_PERIOD_DAYS)

    /**
     * The months in [year] that catch a third payday.
     *
     * Worth naming out loud. A third paycheck arrives in a month whose bills
     * two paychecks already cover, so it is the one month people can plan
     * something with, and almost nobody knows which month it is until it turns
     * up and gets spent.
     */
    fun threePaydayMonths(year: Int, anchor: LocalDate): List<YearMonth> =
        (1..12).map { YearMonth.of(year, it) }.filter { countIn(it, anchor) >= 3 }
}
