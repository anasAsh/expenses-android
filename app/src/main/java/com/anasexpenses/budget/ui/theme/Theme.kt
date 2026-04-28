package com.anasexpenses.budget.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SeedGreen = Color(0xFF1B5E20)
private val SeedGreenLight = Color(0xFF4CAF50)

private val LightColors = lightColorScheme(
    primary = SeedGreen,
    secondary = SeedGreenLight,
    tertiary = SeedGreenLight,
)

@Composable
fun BudgetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
