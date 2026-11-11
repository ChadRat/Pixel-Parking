package com.example.ui.easteregg

import androidx.compose.ui.graphics.Color

/**
 * Types of Capybara Cars available to drive in the 2D Universe Easter Egg game.
 */
enum class CapyCarType(
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val wheelColor: Color
) {
    HAPPY_CABRIO(
        title = "Red Cabriolet",
        subtitle = "Happy smiling capybara enjoying the sunny breeze",
        primaryColor = Color(0xFFE53935),
        secondaryColor = Color(0xFFC62828),
        wheelColor = Color(0xFFCFD8DC)
    ),
    COOL_SUPERCAR(
        title = "Yellow Supercar",
        subtitle = "Cool capybara wearing dark sunglasses in a low supercar",
        primaryColor = Color(0xFFFDD835),
        secondaryColor = Color(0xFFF9A825),
        wheelColor = Color(0xFF212121)
    ),
    LUXURY_SUV(
        title = "Green Luxury SUV",
        subtitle = "Calm capybara cruising through nature in a lifted SUV",
        primaryColor = Color(0xFF2E7D32),
        secondaryColor = Color(0xFF1B5E20),
        wheelColor = Color(0xFF78909C)
    ),
    DRIFT_TUNER(
        title = "Blue Drift Tuner",
        subtitle = "Intense racer capybara drifting with rear wing & tire smoke",
        primaryColor = Color(0xFF1E88E5),
        secondaryColor = Color(0xFF0D47A1),
        wheelColor = Color(0xFFFFB300)
    )
}
