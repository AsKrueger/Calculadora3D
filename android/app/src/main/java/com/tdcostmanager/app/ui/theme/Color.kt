package com.tdcostmanager.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Paleta «taller técnico»: neutros grafito y un único acento naranja filamento.
// El naranja de los botones es el tono 700 para que el texto blanco tenga contraste suficiente.

internal val LightColors = lightColorScheme(
    primary = Color(0xFFC2410C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE4D3),
    onPrimaryContainer = Color(0xFF5A1E05),
    secondary = Color(0xFF3A4552),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE3EA),
    onSecondaryContainer = Color(0xFF121A23),
    tertiary = Color(0xFF0F766E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCF1EC),
    onTertiaryContainer = Color(0xFF04302B),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFCE4E2),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF4F5F7),
    onBackground = Color(0xFF15191E),
    surface = Color(0xFFF4F5F7),
    onSurface = Color(0xFF15191E),
    surfaceVariant = Color(0xFFE9ECF0),
    onSurfaceVariant = Color(0xFF56606B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEEF0F3),
    surfaceContainerHigh = Color(0xFFE8EBEF),
    surfaceContainerHighest = Color(0xFFE2E6EA),
    outline = Color(0xFF8A939D),
    outlineVariant = Color(0xFFDCE0E5),
    inverseSurface = Color(0xFF141A23),
    inverseOnSurface = Color(0xFFF0F2F5),
    inversePrimary = Color(0xFFFF8A4C),
    // La elevación tonal mezcla este color con el fondo: neutro, para que barras y menús no salgan anaranjados
    surfaceTint = Color(0xFF3A4552)
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A4C),
    onPrimary = Color(0xFF3D1300),
    primaryContainer = Color(0xFF7A2E0B),
    onPrimaryContainer = Color(0xFFFFDBC9),
    secondary = Color(0xFFB8C4D0),
    onSecondary = Color(0xFF22303C),
    secondaryContainer = Color(0xFF34404C),
    onSecondaryContainer = Color(0xFFDDE3EA),
    tertiary = Color(0xFF5ED3C4),
    onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF0B4F48),
    onTertiaryContainer = Color(0xFFB8F0E7),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111417),
    onBackground = Color(0xFFE3E6EA),
    surface = Color(0xFF111417),
    onSurface = Color(0xFFE3E6EA),
    surfaceVariant = Color(0xFF2B3036),
    onSurfaceVariant = Color(0xFFA7B0BA),
    surfaceContainerLowest = Color(0xFF0C0F12),
    surfaceContainerLow = Color(0xFF1A1E22),
    surfaceContainer = Color(0xFF1D2126),
    surfaceContainerHigh = Color(0xFF23282E),
    surfaceContainerHighest = Color(0xFF2B3036),
    outline = Color(0xFF78828C),
    outlineVariant = Color(0xFF343B42),
    inverseSurface = Color(0xFFE3E6EA),
    inverseOnSurface = Color(0xFF2A3038),
    inversePrimary = Color(0xFFC2410C),
    surfaceTint = Color(0xFFB8C4D0)
)

/** Colores de un chip o indicador de estado: fondo y texto. */
data class ToneColors(val container: Color, val content: Color)

/** Significado de un estado, independiente del modo claro u oscuro. */
enum class Tone { Neutral, Info, Progress, Success, Warning, Danger }

internal fun toneColors(tone: Tone, dark: Boolean): ToneColors = if (dark) {
    when (tone) {
        Tone.Neutral -> ToneColors(Color(0xFF2B3036), Color(0xFFC3CBD3))
        Tone.Info -> ToneColors(Color(0xFF16324F), Color(0xFFA9CCF5))
        Tone.Progress -> ToneColors(Color(0xFF5A2A0E), Color(0xFFFFC2A0))
        Tone.Success -> ToneColors(Color(0xFF0E3B2A), Color(0xFF9FE3C2))
        Tone.Warning -> ToneColors(Color(0xFF4A3A0A), Color(0xFFF5D98A))
        Tone.Danger -> ToneColors(Color(0xFF5C1A17), Color(0xFFFFB4AB))
    }
} else {
    when (tone) {
        Tone.Neutral -> ToneColors(Color(0xFFE9ECF0), Color(0xFF3F4852))
        Tone.Info -> ToneColors(Color(0xFFDDEBFB), Color(0xFF184A7E))
        Tone.Progress -> ToneColors(Color(0xFFFFE4D3), Color(0xFF8A2E06))
        Tone.Success -> ToneColors(Color(0xFFD9F2E6), Color(0xFF14593C))
        Tone.Warning -> ToneColors(Color(0xFFFCF0CC), Color(0xFF6B4F00))
        Tone.Danger -> ToneColors(Color(0xFFFCE4E2), Color(0xFF8C1D18))
    }
}

/** Degradado de las acciones principales y del logo: naranja 600 → 500. */
internal val AccentGradient = listOf(Color(0xFFEA580C), Color(0xFFF97316))

/** Campo relleno: fondo y borde en reposo. */
data class FieldColors(val container: Color, val border: Color, val label: Color)

internal fun fieldColors(dark: Boolean): FieldColors = if (dark) {
    FieldColors(container = Color(0xFF1D2126), border = Color(0xFF343B42), label = Color(0xFFA7B0BA))
} else {
    FieldColors(container = Color(0xFFF8FAFC), border = Color(0xFFE2E8F0), label = Color(0xFF334155))
}

/** Colores de las partidas del desglose de un presupuesto (material, máquina, herramienta, mano de obra, luz). */
internal fun breakdownPalette(dark: Boolean): List<Color> = if (dark) {
    listOf(Color(0xFFFF8A4C), Color(0xFF7FB2F0), Color(0xFF5ED3C4), Color(0xFFC3CBD3), Color(0xFFF5C451))
} else {
    listOf(Color(0xFFC2410C), Color(0xFF2F6DB5), Color(0xFF0F766E), Color(0xFF56606B), Color(0xFFC99A06))
}
