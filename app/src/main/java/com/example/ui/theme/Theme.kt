package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = NeonGreen,
  onPrimary = Color.Black,
  secondary = CharcoalSurface,
  onSecondary = WhiteText,
  tertiary = DarkGreen,
  onTertiary = WhiteText,
  background = CharcoalBg,
  onBackground = WhiteText,
  surface = CharcoalSurface,
  onSurface = WhiteText,
  surfaceVariant = CharcoalCard,
  onSurfaceVariant = MutedText,
  outline = BorderColor,
  error = DangerRed,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme by default as per request
  dynamicColor: Boolean = false, // Disable dynamic colors to preserve neon branding
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme // Enforce dark scheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
