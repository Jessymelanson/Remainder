package com.remainder.app.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The fourteen colours a look is actually made of.
 *
 * Written out rather than generated from a seed. A generated palette is fine
 * for a utility and wrong for this one: the point of the pink is that it is a
 * particular, deliberate pink, and handing that to an algorithm gets you
 * whatever it feels like producing that day.
 */
private data class Tones(
    val primary: Long,
    val onPrimary: Long,
    val container: Long,
    val onContainer: Long,
    val accent: Long,
    val accentContainer: Long,
    val onAccentContainer: Long,
    val bg: Long,
    val surface: Long,
    val surfaceVariant: Long,
    val onSurface: Long,
    val onSurfaceVariant: Long,
    val outline: Long,
    val outlineVariant: Long
)

/**
 * Every slot Material fills in, set explicitly.
 *
 * The surface container family matters more than it looks. Leaving those to
 * their defaults means any component that reaches for one gets a colour out of
 * Material's baseline purple, and a single stray lilac card in a pink app
 * reads as a bug rather than a shade.
 */
private fun scheme(dark: Boolean, t: Tones): ColorScheme = if (dark) {
    darkColorScheme(
        primary = Color(t.primary),
        onPrimary = Color(t.onPrimary),
        primaryContainer = Color(t.container),
        onPrimaryContainer = Color(t.onContainer),
        secondary = Color(t.accent),
        onSecondary = Color(t.onPrimary),
        secondaryContainer = Color(t.accentContainer),
        onSecondaryContainer = Color(t.onAccentContainer),
        tertiary = Color(t.accent),
        onTertiary = Color(t.onPrimary),
        tertiaryContainer = Color(t.accentContainer),
        onTertiaryContainer = Color(t.onAccentContainer),
        background = Color(t.bg),
        onBackground = Color(t.onSurface),
        surface = Color(t.surface),
        onSurface = Color(t.onSurface),
        surfaceVariant = Color(t.surfaceVariant),
        onSurfaceVariant = Color(t.onSurfaceVariant),
        surfaceContainerLowest = Color(t.bg),
        surfaceContainerLow = Color(t.surface),
        surfaceContainer = Color(t.surface),
        surfaceContainerHigh = Color(t.surfaceVariant),
        surfaceContainerHighest = Color(t.surfaceVariant),
        surfaceDim = Color(t.bg),
        surfaceBright = Color(t.surfaceVariant),
        inverseSurface = Color(t.onSurface),
        inverseOnSurface = Color(t.surface),
        outline = Color(t.outline),
        outlineVariant = Color(t.outlineVariant),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFFFDAD6)
    )
} else {
    lightColorScheme(
        primary = Color(t.primary),
        onPrimary = Color(t.onPrimary),
        primaryContainer = Color(t.container),
        onPrimaryContainer = Color(t.onContainer),
        secondary = Color(t.accent),
        onSecondary = Color(t.onPrimary),
        secondaryContainer = Color(t.accentContainer),
        onSecondaryContainer = Color(t.onAccentContainer),
        tertiary = Color(t.accent),
        onTertiary = Color(t.onPrimary),
        tertiaryContainer = Color(t.accentContainer),
        onTertiaryContainer = Color(t.onAccentContainer),
        background = Color(t.bg),
        onBackground = Color(t.onSurface),
        surface = Color(t.surface),
        onSurface = Color(t.onSurface),
        surfaceVariant = Color(t.surfaceVariant),
        onSurfaceVariant = Color(t.onSurfaceVariant),
        surfaceContainerLowest = Color(t.surface),
        surfaceContainerLow = Color(t.surface),
        surfaceContainer = Color(t.surface),
        surfaceContainerHigh = Color(t.surfaceVariant),
        surfaceContainerHighest = Color(t.surfaceVariant),
        surfaceDim = Color(t.surfaceVariant),
        surfaceBright = Color(t.surface),
        inverseSurface = Color(t.onSurface),
        inverseOnSurface = Color(t.surface),
        outline = Color(t.outline),
        outlineVariant = Color(t.outlineVariant),
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410E0B)
    )
}

/** A named look, in both a light and a dark version. */
data class Palette(
    val key: String,
    val icon: String,
    val name: String,
    val blurb: String,
    /** Shown in the picker, so a swatch does not need its own theme. */
    val dot: Color,
    val light: ColorScheme,
    val dark: ColorScheme
)

private fun palette(
    key: String, icon: String, name: String, blurb: String,
    light: Tones, dark: Tones
) = Palette(
    key, icon, name, blurb,
    dot = Color(light.primary),
    light = scheme(false, light),
    dark = scheme(true, dark)
)

/**
 * The looks on offer, pink first and pink by default.
 *
 * The alternatives exist so the app is usable by someone who does not want a
 * pink phone in a meeting, not because the pink is a placeholder.
 */
val PALETTES: List<Palette> = listOf(

    palette(
        "pink", "🌸", "Cotton Candy",
        "Soft pink. The one the app was designed in.",
        light = Tones(
            primary = 0xFFD81B75, onPrimary = 0xFFFFFFFF,
            container = 0xFFFFD9E7, onContainer = 0xFF52082F,
            accent = 0xFF9C5BB8, accentContainer = 0xFFF3DDFB,
            onAccentContainer = 0xFF37104A,
            bg = 0xFFFFF4F8, surface = 0xFFFFFFFF, surfaceVariant = 0xFFFCE5EE,
            onSurface = 0xFF3A222D, onSurfaceVariant = 0xFF7C5568,
            outline = 0xFFCFA5B8, outlineVariant = 0xFFF0D3DF
        ),
        dark = Tones(
            primary = 0xFFFFA3C7, onPrimary = 0xFF52082F,
            container = 0xFF7A2450, onContainer = 0xFFFFD9E7,
            accent = 0xFFDFB6F0, accentContainer = 0xFF562C6B,
            onAccentContainer = 0xFFF3DDFB,
            bg = 0xFF1B1017, surface = 0xFF24161E, surfaceVariant = 0xFF35222C,
            onSurface = 0xFFF6E2EB, onSurfaceVariant = 0xFFCFA9BB,
            outline = 0xFF9A7485, outlineVariant = 0xFF4A3540
        )
    ),

    palette(
        "strawberry", "🍓", "Strawberry",
        "Warmer and bolder, still very much pink.",
        light = Tones(
            primary = 0xFFE0245E, onPrimary = 0xFFFFFFFF,
            container = 0xFFFFDCE1, onContainer = 0xFF5A0A1C,
            accent = 0xFFE5793A, accentContainer = 0xFFFFE0CC,
            onAccentContainer = 0xFF4A2008,
            bg = 0xFFFFF5F5, surface = 0xFFFFFFFF, surfaceVariant = 0xFFFCE4E6,
            onSurface = 0xFF3D2226, onSurfaceVariant = 0xFF7D555B,
            outline = 0xFFCBA0A6, outlineVariant = 0xFFF1D2D6
        ),
        dark = Tones(
            primary = 0xFFFF9EAE, onPrimary = 0xFF5A0A1C,
            container = 0xFF82203A, onContainer = 0xFFFFDCE1,
            accent = 0xFFFFB784, accentContainer = 0xFF6B3B18,
            onAccentContainer = 0xFFFFE0CC,
            bg = 0xFF1B1012, surface = 0xFF241619, surfaceVariant = 0xFF362226,
            onSurface = 0xFFF7E1E4, onSurfaceVariant = 0xFFD0A8AE,
            outline = 0xFF9B7379, outlineVariant = 0xFF4B3438
        )
    ),

    palette(
        "lavender", "💜", "Lavender",
        "Purple, for when pink is a bit much today.",
        light = Tones(
            primary = 0xFF7B4FBF, onPrimary = 0xFFFFFFFF,
            container = 0xFFEADDFF, onContainer = 0xFF2C0E5C,
            accent = 0xFFC060A8, accentContainer = 0xFFFFD9F1,
            onAccentContainer = 0xFF45103A,
            bg = 0xFFF8F4FF, surface = 0xFFFFFFFF, surfaceVariant = 0xFFEDE4F7,
            onSurface = 0xFF2C2536, onSurfaceVariant = 0xFF675B7A,
            outline = 0xFFAFA1C4, outlineVariant = 0xFFDED3EC
        ),
        dark = Tones(
            primary = 0xFFCFB4FF, onPrimary = 0xFF2C0E5C,
            container = 0xFF513088, onContainer = 0xFFEADDFF,
            accent = 0xFFF0AEDB, accentContainer = 0xFF6B2A5C,
            onAccentContainer = 0xFFFFD9F1,
            bg = 0xFF131020, surface = 0xFF1B1728, surfaceVariant = 0xFF2A2338,
            onSurface = 0xFFE9E2F5, onSurfaceVariant = 0xFFB8AECC,
            outline = 0xFF857B98, outlineVariant = 0xFF3D3550
        )
    ),

    palette(
        "mint", "🌿", "Mint",
        "Calm green. Easy on the eyes for a long sit down.",
        light = Tones(
            primary = 0xFF128A6E, onPrimary = 0xFFFFFFFF,
            container = 0xFFB9F0E0, onContainer = 0xFF00382A,
            accent = 0xFF4A8FBF, accentContainer = 0xFFD0E8FA,
            onAccentContainer = 0xFF0C3450,
            bg = 0xFFF1FBF7, surface = 0xFFFFFFFF, surfaceVariant = 0xFFDCF0E8,
            onSurface = 0xFF1F2E29, onSurfaceVariant = 0xFF557067,
            outline = 0xFF99B5AC, outlineVariant = 0xFFCDE5DC
        ),
        dark = Tones(
            primary = 0xFF6EDCC0, onPrimary = 0xFF00382A,
            container = 0xFF0B5A47, onContainer = 0xFFB9F0E0,
            accent = 0xFF9CCDF0, accentContainer = 0xFF23506E,
            onAccentContainer = 0xFFD0E8FA,
            bg = 0xFF0D1714, surface = 0xFF14201C, surfaceVariant = 0xFF1F2E29,
            onSurface = 0xFFDCEFE8, onSurfaceVariant = 0xFFA6C2B9,
            outline = 0xFF718D84, outlineVariant = 0xFF33453F
        )
    ),

    palette(
        "blueberry", "🫐", "Blueberry",
        "Cool blue. The quiet one.",
        light = Tones(
            primary = 0xFF3457B2, onPrimary = 0xFFFFFFFF,
            container = 0xFFDBE3FF, onContainer = 0xFF001A5C,
            accent = 0xFF7B5AC7, accentContainer = 0xFFE7DDFF,
            onAccentContainer = 0xFF2A1060,
            bg = 0xFFF3F6FF, surface = 0xFFFFFFFF, surfaceVariant = 0xFFE1E6F5,
            onSurface = 0xFF232636, onSurfaceVariant = 0xFF5B6076,
            outline = 0xFFA2A9C0, outlineVariant = 0xFFD3D9EA
        ),
        dark = Tones(
            primary = 0xFFAFC3FF, onPrimary = 0xFF001A5C,
            container = 0xFF1F3E8F, onContainer = 0xFFDBE3FF,
            accent = 0xFFCBB6FF, accentContainer = 0xFF4A2F8C,
            onAccentContainer = 0xFFE7DDFF,
            bg = 0xFF0E1120, surface = 0xFF161A2A, surfaceVariant = 0xFF232839,
            onSurface = 0xFFE2E5F2, onSurfaceVariant = 0xFFB0B6CB,
            outline = 0xFF7C8399, outlineVariant = 0xFF383E52
        )
    )
)

fun paletteFor(key: String): Palette =
    PALETTES.firstOrNull { it.key == key } ?: PALETTES.first()

/**
 * Green for money kept and red for money missing, in every palette.
 *
 * These deliberately ignore the chosen look. Whether a fortnight worked is not
 * a decorative question, and a "you are over budget" that arrives in the same
 * friendly pink as everything else is not doing its job.
 */
object Money {

    @Composable
    @ReadOnlyComposable
    private fun dark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

    /** Money left over. */
    @Composable
    @ReadOnlyComposable
    fun good(): Color = if (dark()) Color(0xFF7BE0A5) else Color(0xFF17794C)

    /** Exactly nothing left, which is the target, not a failure. */
    @Composable
    @ReadOnlyComposable
    fun level(): Color = MaterialTheme.colorScheme.primary

    /** Spent more than came in. */
    @Composable
    @ReadOnlyComposable
    fun bad(): Color = MaterialTheme.colorScheme.error

    /** Cutting it fine, but not past the line. */
    @Composable
    @ReadOnlyComposable
    fun tight(): Color = if (dark()) Color(0xFFF2C14E) else Color(0xFF9A6B00)
}

/** Slightly heavier headings than the default, which reads friendlier. */
private val RemainderTypography: Typography
    get() {
        val base = Typography()
        return base.copy(
            headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
            headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
            headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            labelLarge = base.labelLarge.copy(letterSpacing = 0.3.sp)
        )
    }

@Composable
fun RemainderTheme(
    palette: Palette,
    dark: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (dark) palette.dark else palette.light,
        typography = RemainderTypography,
        content = content
    )
}
