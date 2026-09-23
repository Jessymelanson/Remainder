package com.remainder.app

import com.remainder.app.data.Budget
import com.remainder.app.data.Category
import com.remainder.app.data.Group
import com.remainder.app.data.Verdict
import com.remainder.app.ui.Cash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Reading what someone typed into an amount field.
 *
 * Worth its own tests because the number keypad gives whichever decimal
 * separator the phone is set to, and getting it wrong is not a rounding error,
 * it is a factor of a hundred.
 */
class CashTest {

    private fun parse(text: String) = Cash.parse(text)

    @Test
    fun `plain numbers read as themselves`() {
        assertEquals(2000.0, parse("2000"), 0.0001)
        assertEquals(0.0, parse("0"), 0.0001)
        assertEquals(20.5, parse("20.5"), 0.0001)
        assertEquals(1234.56, parse("1234.56"), 0.0001)
    }

    /**
     * The bug this replaced: commas were stripped unconditionally, so a phone
     * set to a comma decimal separator turned 2000,50 into 200050.
     */
    @Test
    fun `a comma decimal separator is a decimal point, not a thousands mark`() {
        assertEquals(2000.50, parse("2000,50"), 0.0001)
        assertEquals(1.23, parse("1,23"), 0.0001)
        assertEquals(0.5, parse("0,5"), 0.0001)
    }

    @Test
    fun `a thousands separator is still a thousands separator`() {
        assertEquals(1234.0, parse("1,234"), 0.0001)
        assertEquals(1234567.0, parse("1,234,567"), 0.0001)
        // The European way round.
        assertEquals(1234.0, parse("1.234"), 0.0001)
    }

    @Test
    fun `both separators together resolve to the last one`() {
        assertEquals(1234.56, parse("1,234.56"), 0.0001)
        assertEquals(1234.56, parse("1.234,56"), 0.0001)
        assertEquals(1234567.89, parse("1,234,567.89"), 0.0001)
    }

    @Test
    fun `pasting from a banking app works`() {
        assertEquals(1250.0, parse("$1,250.00"), 0.0001)
        assertEquals(1250.0, parse("1 250,00 kr"), 0.0001)
        assertEquals(2137.42, parse("USD 2,137.42"), 0.0001)
    }

    @Test
    fun `half typed input does not blow up`() {
        assertEquals(0.0, parse(""), 0.0001)
        assertEquals(0.0, parse("."), 0.0001)
        assertEquals(0.0, parse(","), 0.0001)
        assertEquals(0.0, parse("abc"), 0.0001)
        // Mid typing, having just pressed the separator.
        assertEquals(2000.0, parse("2000."), 0.0001)
        assertEquals(2000.0, parse("2000,"), 0.0001)
    }

    @Test
    fun `nothing negative ever comes back`() {
        assertEquals(1234.0, parse("-1234"), 0.0001)
    }

    /** What goes into the field has to come back out of it unchanged. */
    @Test
    fun `editable and parse round trip`() {
        listOf(0.0, 20.5, 1234.56, 2000.0, 20000.0).forEach { value ->
            assertEquals(value, parse(Cash.editable(value)), 0.0001)
        }
    }

    // ---- Percentages ------------------------------------------------------

    /**
     * The bug this replaced: rounding alone reported real money as 0%, so a
     * savings line someone was genuinely paying into sat at 0% forever.
     */
    @Test
    fun `a real amount is never reported as nothing`() {
        // 10 dollars out of a 2000 dollar paycheck is half a percent.
        assertEquals("<1%", Cash.percent(10.0 / 2000.0))
        assertEquals("<1%", Cash.percent(0.0001))
        assertEquals("<1%", Cash.percent(0.009))
    }

    @Test
    fun `only an actual zero is zero`() {
        assertEquals("0%", Cash.percent(0.0))
    }

    /** The other end of the same rounding: 99.6% of a goal is not finished. */
    @Test
    fun `almost finished never reads as finished`() {
        assertEquals("99%", Cash.percent(0.996))
        assertEquals("99%", Cash.percent(0.999))
        assertEquals("100%", Cash.percent(1.0))
    }

    @Test
    fun `ordinary percentages round normally`() {
        assertEquals("20%", Cash.percent(0.2))
        assertEquals("35%", Cash.percent(0.345))
        assertEquals("50%", Cash.percent(0.5))
        assertEquals("120%", Cash.percent(1.2))
    }

    @Test
    fun `editable drops pointless trailing zeroes but keeps real cents`() {
        assertEquals("", Cash.editable(0.0))
        assertEquals("2000", Cash.editable(2000.0))
        assertEquals("2000.50", Cash.editable(2000.50))
    }

    /**
     * A balanced plan in a three payday month.
     *
     * $500 checks against $400 + $1,100 of monthly bills splits to thirds and
     * lands 5.7e-14 under zero. The verdict rightly calls that balanced, but the
     * formatter kept the sign and the card read "Balanced to the penny" over
     * "-$0.00 left".
     */
    @Test
    fun `a rounding remainder never prints as minus zero`() {
        val b = Budget(
            500.0,
            listOf(
                Category("a", "🏠", "Rent", Group.BILL, 400.0),
                Category("b", "💡", "Other", Group.BILL, 1100.0)
            ),
            paydaysThisMonth = 3
        )
        assertEquals(Verdict.BALANCED, b.verdict)
        assertFalse(Cash.money(b.leftOver), Cash.money(b.leftOver).contains('-'))
        assertFalse(Cash.money(b.monthLeftOver).contains('-'))
        assertFalse(Cash.rounded(-0.2).contains('-'))
        assertEquals(Cash.money(0.0), Cash.money(-0.004))
    }

    @Test
    fun `a real negative keeps its sign`() {
        assertEquals(true, Cash.money(-0.01).contains('-'))
    }
}
