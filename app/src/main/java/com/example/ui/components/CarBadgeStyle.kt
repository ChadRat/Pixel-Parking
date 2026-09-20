package com.example.ui.components

/**
 * Visual rendering style for the animated car badges on the Home dashboard.
 * - MATERIAL: Clean, illustrated vector line-art matching Material 3 expressive language.
 * - CINEMATIC: Dynamic 3/4 action view illustrations with motion speed lines and rich depth.
 */
enum class CarBadgeStyle {
    MATERIAL,
    CINEMATIC,
    CAPY
}

/**
 * Variant for the central capybara badge character when CAPY style is enabled.
 * - BABY: Extra cute, chubby baby capybara with sweet closed smiling eyes.
 * - ADULT: Calm, reference sitting capybara with classic posture and horizontal sleepy eyes.
 */
enum class CapyVariant {
    BABY,
    ADULT
}
