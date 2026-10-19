package com.example.schedule.presentation.components

import androidx.compose.ui.graphics.Color

data class AppPalette(
    val background: Color, val text: Color, val dayButton: Color,
    val card: Color, val currentCard: Color, val border: Color
)

fun appPalette(dark: Boolean) = if (dark) AppPalette(
    background = Color(0xFF24282D),
    text = Color(0xFFE8EDF2),
    dayButton = Color(0xFF353D46),
    card = Color(0xFF303840),
    currentCard = Color(0xFF426D87),
    border = Color(0xFF46515B)
) else AppPalette(
    // Neutral, slightly warm background; blue is reserved for accents/highlights.
    background = Color(0xFFF7F7F4),
    text = Color(0xFF29343D),
    dayButton = Color(0xFFE9EFF4),
    card = Color(0xFFFFFFFF),
    currentCard = Color(0xFFB9DCEB),
    border = Color(0xFFD9E0E5)
)
