package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ========================================================
// AMAN | أمان - Brand Visual Identity Colors (Official Master V2)
// ========================================================

// 1. Core Official Brand Palette
val AmanTealDark = Color(0xFF087F6E)        // Primary (الرئيسي) - RGB(8, 127, 110)
val AmanTealPrimary = Color(0xFF19899A)     // Secondary (الثانوي) - RGB(25, 137, 154)
val AmanTealAccent = Color(0xFF6EE7B7)      // Accent (التمييز) - RGB(110, 231, 183)
val AmanTealLight = Color(0xFFE9F8F5)       // Light (الخلفيات الناعمة) - RGB(233, 248, 245)
val AmanTealUltraLight = Color(0xFFE9F8F5)
val AmanBgLight = Color(0xFFF7FAF9)         // Background (خلفية التطبيق) - RGB(247, 250, 249)
val SurfaceWhite = Color(0xFFFFFFFF)        // Surface (الأسطح) - RGB(255, 255, 255)
val TextPrimary = Color(0xFF18302D)         // Text Primary (النصوص الأساسية) - RGB(24, 48, 45)
val TextSecondary = Color(0xFF667A77)       // Text Secondary (النصوص الثانوية) - RGB(102, 122, 119)
val BorderSubtle = Color(0xFFDCE9E6)        // Border & Dividers (الحدود والفواصل) - RGB(220, 233, 230)
val BorderField = Color(0xFFDCE9E6)
val InputBg = Color(0xFFFFFFFF)
val DividerColor = Color(0xFFDCE9E6)

// Drawer / In-App Navigation (Used across Customer & Admin In-App Screens)
val AmanDrawerDeep = Color(0xFF087F6E)
val AmanDrawerMid = Color(0xFF19899A)
val AmanDrawerActivePill = Color(0x33FFFFFF)
val AmanDarkSlate = Color(0xFF18302D)
val SurfaceCard = Color(0xFFFFFFFF)
val TextMuted = Color(0xFF8B9E9B)

// 2. Linear Gradients (Exact Brand Specs)
val AmanHeaderGradient = Brush.horizontalGradient(
    listOf(Color(0xFF087F6E), Color(0xFF19899A))
)
val AmanButtonGradient = Brush.horizontalGradient(
    listOf(Color(0xFF087F6E), Color(0xFF19899A))
)
val AmanDrawerGradient = Brush.verticalGradient(
    listOf(Color(0xFF087F6E), Color(0xFF19899A), Color(0xFF6EE7B7))
)
val AmanCardGradient = Brush.verticalGradient(
    listOf(Color(0xFFFFFFFF), Color(0xFFF7FAF9))
)

// 3. Telecom Provider Identity Colors (Yemen Operators)
val ProviderYemenMobile = Color(0xFFDC2626)    // Yemen Mobile Red (77, 78)
val ProviderYemenMobileBg = Color(0xFFFEF2F2)
val ProviderYOU = Color(0xFFEAB308)            // YOU Warm Yellow (73)
val ProviderYOUBg = Color(0xFFFEFCE8)
val ProviderSabaFon = Color(0xFF0284C7)        // SabaFon Sky Blue (71)
val ProviderSabaFonBg = Color(0xFFF0F9FF)
val ProviderY = Color(0xFF7C3AED)              // Y Telecom Purple (70)
val ProviderYBg = Color(0xFFF5F3FF)

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
