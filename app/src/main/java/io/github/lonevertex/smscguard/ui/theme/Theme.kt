package io.github.lonevertex.smscguard.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricTeal,
    onPrimary = Color(0xFF00373A),
    primaryContainer = ElectricTealContainer,
    onPrimaryContainer = ElectricTealOnContainer,
    secondary = WarmAmber,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = WarmAmberContainer,
    onSecondaryContainer = WarmAmberOnContainer,
    tertiary = Color(0xFF80D4FA),
    onTertiary = Color(0xFF003548),
    background = DarkNavyBackground,
    onBackground = TextPrimaryDark,
    surface = DarkNavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkNavyElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkNavyStroke,
    outlineVariant = DarkNavyStrokeSubtle,
    error = StatusError,
    errorContainer = StatusErrorContainer,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00696F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9DF0F7),
    onPrimaryContainer = Color(0xFF002022),
    secondary = Color(0xFF825500),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB3),
    onSecondaryContainer = Color(0xFF291800),
    background = Color(0xFFFBFDFE),
    onBackground = Color(0xFF191C1D),
    surface = Color(0xFFFBFDFE),
    onSurface = Color(0xFF191C1D),
    surfaceVariant = Color(0xFFDAE4E5),
    onSurfaceVariant = Color(0xFF3F484A),
    outline = Color(0xFF6F797A)
)

@Composable
fun SmscGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
