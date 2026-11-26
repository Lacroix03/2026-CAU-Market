package com.cau.campusmarket.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FigmaLightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = PrimaryBlueSoftBg,
    onPrimaryContainer = PrimaryBlue,
    secondary = SelectedChipBg,
    onSecondary = Color.White,
    background = PageBackground,
    onBackground = TextPrimary,
    surface = PageBackground,
    onSurface = TextPrimary,
    surfaceVariant = SoftGrayBg,
    onSurfaceVariant = TextSecondary,
    error = DangerRed,
    onError = Color.White,
    outline = BorderGray
)

@Composable
fun CauCampusMarketTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FigmaLightColorScheme,
        typography = CauTypography,
        content = content
    )
}
