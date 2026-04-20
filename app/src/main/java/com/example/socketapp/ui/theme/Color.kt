package com.example.socketapp.ui.theme

import androidx.compose.ui.graphics.Color

// Galicia Editorial — tokens M3 (light theme)
val GaliciaPrimary            = Color(0xFFE67B21)
val GaliciaOnPrimary          = Color(0xFFFFFFFF)
val GaliciaPrimaryContainer   = Color(0xFFFBE8D4)
val GaliciaOnPrimaryContainer = Color(0xFF6B3A0F)
val GaliciaSecondary          = Color(0xFF0A0A0A)
val GaliciaOnSecondary        = Color(0xFFFFFFFF)
val GaliciaSecondaryContainer = Color(0xFFF4F4F4)
val GaliciaOnSecondaryContainer = Color(0xFF0A0A0A)
val GaliciaTertiary           = Color(0xFF6B6B6B)
val GaliciaOnTertiary         = Color(0xFFFFFFFF)
val GaliciaTertiaryContainer  = Color(0xFFEBEBEB)
val GaliciaOnTertiaryContainer = Color(0xFF0A0A0A)
val GaliciaBackground         = Color(0xFFFFFFFF)
val GaliciaOnBackground       = Color(0xFF0A0A0A)
val GaliciaSurface            = Color(0xFFFFFFFF)
val GaliciaOnSurface          = Color(0xFF0A0A0A)
val GaliciaSurfaceVariant     = Color(0xFFF4F4F4)
val GaliciaOnSurfaceVariant   = Color(0xFF6B6B6B)
val GaliciaOutline            = Color(0xFFEBEBEB)
val GaliciaOutlineVariant     = Color(0xFFF0F0F0)
val GaliciaError              = Color(0xFFB91C1C)
val GaliciaOnError            = Color(0xFFFFFFFF)
val GaliciaErrorContainer     = Color(0xFFFEE2E2)
val GaliciaOnErrorContainer   = Color(0xFF7F1D1D)

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
val GaliciaAvatarPalette = listOf(
    Color(0xFF5A5A5A), Color(0xFF4F4F4F), Color(0xFF454545), Color(0xFF3B3B3B),
    Color(0xFF5F5F5F), Color(0xFF4A4A4A), Color(0xFF3F3F3F), Color(0xFF353535),
)
val AvatarInitial = Color(0xFFE67B21)
