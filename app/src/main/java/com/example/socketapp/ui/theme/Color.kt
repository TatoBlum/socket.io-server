package com.example.socketapp.ui.theme

import androidx.compose.ui.graphics.Color

// Editorial tokens M3 (light theme)
val AppPrimary            = Color(0xFFE67B21)
val AppOnPrimary          = Color(0xFFFFFFFF)
val AppPrimaryContainer   = Color(0xFFFBE8D4)
val AppOnPrimaryContainer = Color(0xFF6B3A0F)
val AppSecondary          = Color(0xFF0A0A0A)
val AppOnSecondary        = Color(0xFFFFFFFF)
val AppSecondaryContainer = Color(0xFFF4F4F4)
val AppOnSecondaryContainer = Color(0xFF0A0A0A)
val AppTertiary           = Color(0xFF6B6B6B)
val AppOnTertiary         = Color(0xFFFFFFFF)
val AppTertiaryContainer  = Color(0xFFEBEBEB)
val AppOnTertiaryContainer = Color(0xFF0A0A0A)
val AppBackground         = Color(0xFFFFFFFF)
val AppOnBackground       = Color(0xFF0A0A0A)
val AppSurface            = Color(0xFFFFFFFF)
val AppOnSurface          = Color(0xFF0A0A0A)
val AppSurfaceVariant     = Color(0xFFF4F4F4)
val AppOnSurfaceVariant   = Color(0xFF6B6B6B)
val AppOutline            = Color(0xFFEBEBEB)
val AppOutlineVariant     = Color(0xFFF0F0F0)
val AppError              = Color(0xFFB91C1C)
val AppOnError            = Color(0xFFFFFFFF)
val AppErrorContainer     = Color(0xFFFEE2E2)
val AppOnErrorContainer   = Color(0xFF7F1D1D)

// Semantic — direcciones de precio
val PriceUp   = Color(0xFF16A34A)
// Darker shade for text over light backgrounds; PriceUp is for chart fills/flash.
val PriceUpText  = Color(0xFF15803D)
val PriceDown = Color(0xFFDC2626)
val PriceUpFlash   = Color(0xFFD1FAE5)   // hex explícito (era alpha)
val PriceDownFlash = Color(0xFFFEE2E2)   // hex explícito

// Semantic — estado de conexión (ámbar legible sobre fondo claro)
val StatusWarning = Color(0xFFCA8A04)

// Segmented control — track neutro
val SegmentedTrack = Color(0xFFF4F4F4)

// Card surface — un escalón más claro que SegmentedTrack para que el tab row se diferencie sobre la card
val CardSurface = Color(0xFFFAFAFA)

// Avatars — 8 shades gris neutro editorial
val AvatarPalette = listOf(
    Color(0xFF5A5A5A), Color(0xFF4F4F4F), Color(0xFF454545), Color(0xFF3B3B3B),
    Color(0xFF5F5F5F), Color(0xFF4A4A4A), Color(0xFF3F3F3F), Color(0xFF353535),
)
val AvatarInitial = Color(0xFFE67B21)
