# Remainder

A bi-weekly paycheck budgeter for Android. Type in one paycheck, and it takes
the bills off, then the living costs, then what you are putting away, and tells
you what is actually left.

This repository holds the signed APK. The source is not published here.

## The idea

Three passes, in the order money really leaves an account:

| Pass | What goes in it | Why it is in that position |
|---|---|---|
| 🧾 Bills | Phone, car insurance, mortgage, electric, internet | Already promised. No say in them this fortnight. |
| 🛒 Living costs | Groceries, gas | The part you actually steer. |
| 🏦 Savings and goals | Savings, emergency fund, goals | Last in the arithmetic, first in importance. |

You can add categories of your own, with any of 40 emoji, in any of the three
passes.

## Your payday drives everything

This is the part most budget apps quietly get wrong. Being paid every 14 days
does not divide into calendar months, so **most months catch two paydays and
twice a year one catches three**.

Set your payday once - any payday you can remember, Remainder counts fortnights
from it - and every monthly bill is split across the paychecks that actually
arrive that month:

| A $1,200 mortgage | Paydays | Out of each check |
|---|---|---|
| Ordinary month | 2 | **$600.00** |
| Three-payday month | 3 | **$400.00** |

Same bill, same year, but every check in a three-payday month goes further.
Averaging it away over 26 paychecks would hide the one month in six that is
genuinely easier than the rest.

## Verify what you downloaded

| File | SHA-256 |
|---|---|
| `Remainder-release.apk` | `3c26fe02dec67a14c24215d17bb48bb15cbf77b2fed52898d397af9fb6aa0ff3` |

```bash
sha256sum Remainder-release.apk                    # Linux, macOS, git bash
certutil -hashfile Remainder-release.apk SHA256    # Windows
```

## Signing

```
CN=JApps, OU=JFamily, O=JApps, C=CA
753bf85dbbf2cd55862e494733ffe69a14eff96d9c0b8e0883fe347464c7a02f
```

```bash
apksigner verify --print-certs Remainder-release.apk
```

That prints `v1 scheme (JAR signing): false`. It is not missing - with
`minSdk 26`, apksigner only verifies the schemes that platform range uses. Add
`--min-sdk-version 21` and v1, v2 and v3 all verify.

## Requirements

Android 8.0 or later (minSdk 26), built against SDK 36.
