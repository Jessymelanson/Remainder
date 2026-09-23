package com.remainder.app.ui

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Money on screen.
 *
 * Formatting goes through the phone's own currency settings rather than a
 * hardcoded dollar sign, so the app is right for whoever is holding it.
 */
object Cash {

    /**
     * The formatters, rebuilt if the phone's locale changes under us.
     *
     * These are cached because `money` is called for every row on every
     * recomposition, and building a `NumberFormat` each time would be real
     * work in a hot path. But caching them in an `object` means they are made
     * once for the life of the process and keep whatever locale was in force at
     * that instant - so changing the phone's language or region left every
     * figure in the old currency format until the app was force stopped.
     *
     * No lock. The worst a race can do here is build the same formatter twice
     * and keep one of them, and both are correct; a lock on this path would
     * cost more than the bug it prevents.
     */
    @Volatile
    private var cached: Formats? = null

    private class Formats(val locale: Locale) {
        val currency: NumberFormat = NumberFormat.getCurrencyInstance(locale).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
        val whole: NumberFormat = NumberFormat.getCurrencyInstance(locale).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
    }

    private fun formats(): Formats {
        val now = Locale.getDefault()
        val held = cached
        if (held != null && held.locale == now) return held
        return Formats(now).also { cached = it }
    }

    fun money(value: Double): String = formats().currency.format(tidy(value, CENT))

    /** For big headline figures where the cents are noise. */
    fun rounded(value: Double): String = formats().whole.format(tidy(value, DOLLAR))

    /**
     * Zero, sign and all, for anything too small to show.
     *
     * Splitting a monthly bill three ways leaves remainders like -5.7e-14, and
     * the formatter keeps the minus sign on a value it rounds to nothing, so a
     * plan balanced to the penny read "-$0.00 left".
     */
    private fun tidy(value: Double, below: Double): Double =
        if (abs(value) < below) 0.0 else value

    private const val CENT = 0.005
    private const val DOLLAR = 0.5

    /** A deduction, always written as a subtraction. */
    fun minus(value: Double): String = "- " + formats().currency.format(abs(value))

    /** What the user typed, for putting back into an input field. */
    fun editable(value: Double): String =
        if (value <= 0.0) "" else String.format(Locale.US, "%.2f", value).removeSuffix(".00")

    /**
     * A fraction as a percentage, without lying at either end.
     *
     * Rounding alone reported real money as "0%": saving $10 out of a $2,000
     * paycheck is half a percent, which rounds to nothing, so the line sat at
     * 0% no matter how many times someone checked it. The same rounding turned
     * 99.6% of a savings goal into "100%", which says finished when it is not.
     * Only an actual zero prints 0%, and only an actual whole prints 100%.
     */
    fun percent(fraction: Double): String {
        val pct = fraction * 100
        return when {
            pct <= 0.0 -> "0%"
            pct < 1.0 -> "<1%"
            pct < 100.0 && pct.roundToInt() >= 100 -> "99%"
            else -> "${pct.roundToInt()}%"
        }
    }

    /**
     * Reads whatever was typed into an amount field.
     *
     * Strips currency symbols, spaces and thousands separators so pasting
     * "$1,250.00" out of a banking app does the obvious thing instead of
     * quietly becoming zero.
     */
    fun parse(input: String): Double {
        val kept = input.filter { it.isDigit() || it == '.' || it == ',' }
        if (kept.isEmpty()) return 0.0

        // Which of . and , is the decimal point depends on where you live, and
        // the number keypad gives whichever the phone is set to. Stripping
        // commas unconditionally turned "2000,50" into 200050 for most of
        // Europe, which is a hundredfold error in an app about money.
        //
        // The last separator wins, and it is only a decimal point if one or
        // two digits follow it. Three digits after it is a thousands group in
        // any locale, and nobody writes money to three decimal places.
        val separator = maxOf(kept.lastIndexOf('.'), kept.lastIndexOf(','))
        val trailing = kept.length - separator - 1
        val isDecimal = separator >= 0 && trailing in 1..2 &&
            kept.substring(separator + 1).all { it.isDigit() }

        val normalised = if (isDecimal) {
            kept.take(separator).filter { it.isDigit() } + "." + kept.substring(separator + 1)
        } else {
            kept.filter { it.isDigit() }
        }

        val value = normalised.toDoubleOrNull() ?: return 0.0
        return if (value.isFinite() && value >= 0) value else 0.0
    }
}
