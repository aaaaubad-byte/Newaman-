package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ========================================================
// AMAN | أمان - Brand Visual Identity Colors
// ========================================================

// 1. Core Brand Colors
val AmanTealDark = Color(0xFF087F6E)
val AmanTealPrimary = Color(0xFF19899A)
val AmanTealAccent = Color(0xFF6EE7B7)
val AmanTealLight = Color(0xFFE9F8F5)
val AmanTealUltraLight = Color(0xFFE9F8F5)
val AmanBgLight = Color(0xFFF7FAF9)
val AmanDarkSlate = Color(0xFF183B2D)

val AmanDrawerDeep = Color(0xFF087F6E)
val AmanDrawerMid = Color(0xFF19899A)
val AmanDrawerActivePill = Color(0x33FFFFFF)

// 2. Linear Gradients
val AmanHeaderGradient = Brush.horizontalGradient(
    listOf(Color(0xFF004E4B), Color(0xFF006B61))
)
val AmanButtonGradient = Brush.horizontalGradient(
    listOf(Color(0xFF006B61), Color(0xFF00A88F))
)
val AmanDrawerGradient = Brush.verticalGradient(
    listOf(Color(0xFF004E4B), Color(0xFF006B61), Color(0xFF00A88F))
)
val AmanCardGradient = Brush.verticalGradient(
    listOf(Color(0xFFFFFFFF), Color(0xFFFAFCFD))
)

// 3. Telecom Provider Identity Colors
val ProviderYemenMobile = Color(0xFFDC2626) // Crimson Red (77, 78)
val ProviderYemenMobileBg = Color(0xFFFEF2F2)
val ProviderYOU = Color(0xFFEAB308)         // MTN/YOU Warm Yellow (73)
val ProviderYOUBg = Color(0xFFFEFCE8)
val ProviderSabaFon = Color(0xFFEA580C)     // Orange (71)
val ProviderSabaFonBg = Color(0xFFFFF7ED)
val ProviderY = Color(0xFF0284C7)           // Sky Blue (70)
val ProviderYBg = Color(0xFFF0F9FF)

// 4. Status Badges & Pills (Crisp modern tints)
// Active (نشط / محمي)
val StatusActiveDot = Color(0xFF10B981)
val StatusActiveBg = Color(0xFFECFDF5)
val StatusActiveText = Color(0xFF065F46)
val StatusActiveBorder = Color(0xFFA7F3D0)

// Pending (معلق / قيد المراجعة)
val StatusPendingDot = Color(0xFFF59E0B)
val StatusPendingBg = Color(0xFFFFFBEB)
val StatusPendingText = Color(0xFF92400E)
val StatusPendingBorder = Color(0xFFFDE68A)

// Stopped / Rejected (موقوف / غير محمي / مرفوض)
val StatusRejectedDot = Color(0xFFEF4444)
val StatusRejectedBg = Color(0xFFFEF2F2)
val StatusRejectedText = Color(0xFF991B1B)
val StatusRejectedBorder = Color(0xFFFECACA)

// 5. Neutrals & Borders
val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceCard = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF183B2D)
val TextSecondary = Color(0xFF6E7A77)
val TextMuted = Color(0xFF90A4AE)
val BorderSubtle = Color(0xFFDCE9E6)
val BorderField = Color(0xFFDCE9E6)
val InputBg = Color(0xFFFFFFFF)
val DividerColor = Color(0xFFECEFF1)
