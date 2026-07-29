package week11.st560151.finalproject.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandGreen,
    onPrimary = BrandBlack,
    secondary = BrandGreenDark,
    onSecondary = Color.White,
    tertiary = BrandGreenLight,
    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = BrandBlack,
    onPrimary = Color.White,
    secondary = BrandGreenDark,
    onSecondary = Color.White,
    secondaryContainer = BrandGreenLight,
    onSecondaryContainer = BrandBlack,
    tertiary = BrandGreen,
    onTertiary = BrandBlack,
    background = BackgroundLight,
    onBackground = BrandBlack,
    surface = SurfaceLight,
    onSurface = BrandBlack,
    outline = OutlineLight,
    error = ErrorRed
)

/**
 * Dynamic color is intentionally left out: SplitBill has its own
 * green/black brand palette that we don't want overridden by Material You.
 */
@Composable
fun SplitBillTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
