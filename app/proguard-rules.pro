# Frontwatch keeps no reflection-based entry points beyond the Android
# components, which AGP's default rules already cover. R8 is off for the
# sideload release build; turn it on before publishing and re-test the
# scanner, which reads PackageManager metadata rather than app classes.
-dontwarn org.jetbrains.annotations.**
