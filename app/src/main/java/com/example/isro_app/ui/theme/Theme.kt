package com.example.isro_app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Surface,
    secondary = SurfaceMuted,
    tertiary = Divider,
    surface = Color(0xFF0F172A),
    onSurface = Surface,
    background = Color(0xFF0F172A),
    onBackground = Surface
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Surface,
    secondary = SurfaceMuted,
    onSecondary = TextPrimary,
    tertiary = Divider,
    surface = Surface,
    onSurface = TextPrimary,
    background = SurfaceMuted,
    onBackground = TextPrimary
)

@Composable
fun ISRO_APPTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
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
        typography = Typography,
        content = content
    )
}