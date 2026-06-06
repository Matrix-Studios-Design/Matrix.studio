package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val NeonColorScheme = darkColorScheme(
  primary = NeonYellow,
  onPrimary = DeepBlack,
  secondary = BrightWhite,
  onSecondary = DeepBlack,
  tertiary = SubtleGray,
  background = DeepBlack,
  onBackground = BrightWhite,
  surface = CharcoalButton,
  onSurface = BrightWhite
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color to maintain consistent Yellow-White-Black styling
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = NeonColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
