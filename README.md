# Remainder

A bi-weekly paycheck budgeter for Android. Type in one paycheck, and it takes
the bills off, then the living costs, then what you are putting away, and tells
you what is actually left.

Install `Remainder-release.apk`.

## The idea

Three passes, in the order money really leaves an account:

| Pass | What goes in it | Why it is in that position |
|---|---|---|
| 🧾 Bills | Phone, car insurance, mortgage, electric, internet | Already promised. No say in them this fortnight. |
| 🛒 Living costs | Groceries, gas | The part you actually steer. |
| 🏦 Savings and goals | Savings, emergency fund, goals | Last in the arithmetic, first in importance. |

You can add categories of your own with any of 40 emoji, in any of the three
passes.

## Your payday drives everything

This is the part most budget apps quietly get wrong. Being paid every 14 days
does not divide into calendar months, so **most months catch two paydays and
twice a year one catches three**.

Set your payday once (any payday you can remember -- Remainder counts fortnights
from it) and every monthly bill is split across the paychecks that actually
arrive that month:

| A $1,200 mortgage | Paydays | Out of each check |
|---|---|---|
| Ordinary month | 2 | **$600.00** |
| Three payday month | 3 | **$400.00** |

Same bill, same year, but every check in a three payday month goes further.
Averaging it away over 26 paychecks would hide the one month in six that is
genuinely easier than the rest.

Step through the months with the arrows on the Budget tab to see which ones
those are, and what the third paycheck is actually worth. Not the whole
paycheck -- its own groceries and fuel still come out of it.

## Savings goals

Any savings category can carry a balance and a target. Say you want $20,000:
put what you already have in the pot and the goal beside it, and the row on the
Budget page grows a progress bar.

Remainder then works out the finish date from **your real paydays**, not from an
approximate number of months:

> $4,000 of $20,000 . 20%
> $16,000 to go, about 80 more paychecks, around September 2029

Change the amount going in and the date moves, which is the thing that makes a
target feel reachable. A goal with a target but nothing going in is called out
as stalled rather than given a fake date, and a monthly contribution follows
the month's payday count like every other monthly amount.

All the goals are added up in one card underneath.

## Removing categories

Any category can be deleted, including the standard ten -- tap it, then
**Delete**. A deleted one stays deleted; Settings offers to bring the standard
ones back if you change your mind, and they come back empty.

The same dialog also has a **turn off** switch, which is the gentler option: it
stops the amount being deducted but keeps the number, for a bill that is paid
off for now.

## Backup

Settings has **Back up** and **Restore**. The backup is a plain, readable JSON
file written through the phone's own file picker, so it goes wherever you keep
things and the app needs no storage permission to write it.

Restoring asks first, and a file that is not a Remainder backup is refused
outright rather than half applied. One broken field inside a real backup falls
back to a sane value instead of losing the whole file.

## The verdict

| | |
|---|---|
| 💚 Under budget | Money left after everything, including savings. |
| 🎯 Balanced | Nothing left, which is the target rather than a failure. |
| 💔 Over budget | The plan spends more than the paycheck holds. Shows the gap. |

Comparisons are made with half a cent of tolerance. A monthly bill split three
ways does not land on a whole cent, and without that tolerance the app would
report being over budget by a rounding error.

## The Breakdown tab

Five numbered steps showing the running balance after **every single
deduction**, so the working is visible rather than just the answer, then advice
built from the numbers you actually entered:

- how your split compares to the 50 / 30 / 20 guide
- what share of your pay you are keeping
- every savings goal with its percentage, what is left, and the month it lands
- three months of cover sized against your own outgoings, and how many
  paychecks it takes to get there at your current rate
- whether this is one of your three payday months, and what the extra check is
  genuinely worth once its own costs come out
- if you are over budget: the size of the gap, the largest thing you still
  control, and the percentage cut that would close it

## Themes

Five palettes, each with a light and a dark version, pink by default:

🌸 Cotton Candy · 🍓 Strawberry · 💜 Lavender · 🌿 Mint · 🫐 Blueberry

Light, dark, or follow the phone. Green for money kept and red for money
missing stay the same in every palette, because whether a fortnight worked is
not a decorative question.

## Privacy

The manifest declares **no permissions at all**, including no internet
permission. Everything is worked out on the phone and kept in the app's own
private storage. Uninstalling takes the data with it.

Android's own cloud backup is off (`allowBackup="false"`), so the budget is not
copied to Google Drive and does not follow a Google account onto a new phone.
That is what makes the sentence above true rather than aspirational, and it is
why Settings has **Back up** and **Restore**: moving the budget is something you
do deliberately, to a file you choose.

## Building

Needs JDK 17 (not 25) and the Android SDK with `platforms;android-36`:

```bash
JAVA_HOME=C:/Users/USER/AppData/Local/Android/jdk-17 \
ANDROID_HOME=C:/Users/USER/AppData/Local/Android/Sdk \
./gradlew.bat assembleDebug assembleRelease
```

Gradle 8.11.1 / AGP 8.9.0 / Kotlin 2.1.0 / compileSdk 36 / minSdk 26, the same
stack as Frontwatch.

## Tests

`Model.kt`, `Budget.kt`, `Paydays.kt`, `Backup.kt` and `Cash.kt` are plain
Kotlin with no Android in them, so the arithmetic is covered by JVM unit tests
that need no device:

```bash
./gradlew.bat testDebugUnitTest
```

84 tests, checked against figures worked out by hand and chosen to divide
cleanly, so a failure is a real mistake rather than a rounding argument.

They cover the running balance after each step, the three verdicts, the
rounding tolerance, over-100% shares, and what a third paycheck is worth. The
payday maths is checked against the whole of 2026 with an anchor of Friday 2
January: 26 paydays across the twelve months, every one exactly a fortnight
after the last, and exactly two months (January and July) catching a third.
February is checked on all 28 possible anchors, since 28 days is exactly two
fortnights and can never fit a third.

Goals are checked for rounding up a part paycheck (promising a goal one payday
early is worse than being a fortnight cautious), for refusing to invent a date
when nothing is going in, and for capping an impossible target instead of
overflowing. Backups are checked to round trip exactly, and to refuse anything
that is not a Remainder backup rather than half applying it, and to record which
standard categories were deleted so a restore does not hand them back. A backup
written before the yearly cadence was dropped is checked to restore as its
monthly share, since reading it literally turned a $240 annual renewal into $240
a month.

Amount parsing is checked in both decimal conventions, since the number keypad
gives whichever separator the phone is set to.
