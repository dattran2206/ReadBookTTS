package com.vietreader.tts.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Universal paper-themed color palette for Wordwave.
 * Seamlessly adapts all screens and components to match the chosen reading paper texture:
 * - IVORY (Trắng ngà): Refined, warm, classic cream paper.
 * - SEPIA (Giấy cổ Sepia): Vintage warm parchment with antique brown ink and leather accents.
 * - NIGHT (Tối dịu mắt): Soft obsidian paper reducing eye fatigue during nighttime reading.
 */
data class VietReaderPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val primaryFixed: Color,
    val primaryFixedDim: Color,
    val onPrimaryFixed: Color,
    val onPrimaryFixedVariant: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceDim: Color,
    val surfaceBright: Color,
    val outline: Color,
    val outlineVariant: Color,
    val readingInk: Color,
    val readingInkMuted: Color,
    val highlightBg: Color,
    val highlightBorder: Color
)

val IvoryPalette = VietReaderPalette(
    primary = Color(0xFF903F00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB45309),
    onPrimaryContainer = Color(0xFFFFF1EB),
    primaryFixed = Color(0xFFFFDBCA),
    primaryFixedDim = Color(0xFFFFB68E),
    onPrimaryFixed = Color(0xFF331200),
    onPrimaryFixedVariant = Color(0xFF763300),
    secondary = Color(0xFF545F73),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E0F8),
    onSecondaryContainer = Color(0xFF586377),
    tertiary = Color(0xFF006443),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF007F57),
    onTertiaryContainer = Color(0xFFCCFFE2),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF1F1B17),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF1F1B17),
    surfaceVariant = Color(0xFFEAE1DA),
    onSurfaceVariant = Color(0xFF564338),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCF2EB),
    surfaceContainer = Color(0xFFF6ECE6),
    surfaceContainerHigh = Color(0xFFF0E6E0),
    surfaceContainerHighest = Color(0xFFEAE1DA),
    surfaceDim = Color(0xFFE2D8D2),
    surfaceBright = Color(0xFFFFF8F5),
    outline = Color(0xFF897267),
    outlineVariant = Color(0xFFDDC1B3),
    readingInk = Color(0xFF44403C),
    readingInkMuted = Color(0xFF57534E),
    highlightBg = Color(0xFFFEF3C7),
    highlightBorder = Color(0xFFB45309)
)

val SepiaPalette = VietReaderPalette(
    primary = Color(0xFF7D3700),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF99490A),
    onPrimaryContainer = Color(0xFFFFF3E8),
    primaryFixed = Color(0xFFE8C8AC),
    primaryFixedDim = Color(0xFFD6A984),
    onPrimaryFixed = Color(0xFF331200),
    onPrimaryFixedVariant = Color(0xFF682C00),
    secondary = Color(0xFF5D5447),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5D7C2),
    onSecondaryContainer = Color(0xFF52483C),
    tertiary = Color(0xFF1E603E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF287950),
    onTertiaryContainer = Color(0xFFD0F5DF),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF4E8D1),
    onBackground = Color(0xFF33271E),
    surface = Color(0xFFF4E8D1),
    onSurface = Color(0xFF33271E),
    surfaceVariant = Color(0xFFDFCEAB),
    onSurfaceVariant = Color(0xFF6B5848),
    surfaceContainerLowest = Color(0xFFFAF2E4),
    surfaceContainerLow = Color(0xFFECE0C6),
    surfaceContainer = Color(0xFFE3D4B6),
    surfaceContainerHigh = Color(0xFFDAC7A6),
    surfaceContainerHighest = Color(0xFFD1BD97),
    surfaceDim = Color(0xFFC7B48E),
    surfaceBright = Color(0xFFF4E8D1),
    outline = Color(0xFF968169),
    outlineVariant = Color(0xFFC7B69C),
    readingInk = Color(0xFF33271E),
    readingInkMuted = Color(0xFF5A493D),
    highlightBg = Color(0xFFEBD4A7),
    highlightBorder = Color(0xFF99490A)
)

val NightPalette = VietReaderPalette(
    primary = Color(0xFFFFB68E),
    onPrimary = Color(0xFF502200),
    primaryContainer = Color(0xFF743200),
    onPrimaryContainer = Color(0xFFFFDBCA),
    primaryFixed = Color(0xFF46210A),
    primaryFixedDim = Color(0xFF5E2E10),
    onPrimaryFixed = Color(0xFFFFDBCA),
    onPrimaryFixedVariant = Color(0xFFFFB68E),
    secondary = Color(0xFF919DAE),
    onSecondary = Color(0xFF161F2C),
    secondaryContainer = Color(0xFF323B4A),
    onSecondaryContainer = Color(0xFFD1DCED),
    tertiary = Color(0xFF4ED698),
    onTertiary = Color(0xFF003823),
    tertiaryContainer = Color(0xFF005236),
    onTertiaryContainer = Color(0xFFBEF7D9),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191613),
    onBackground = Color(0xFFEAE1DA),
    surface = Color(0xFF191613),
    onSurface = Color(0xFFEAE1DA),
    surfaceVariant = Color(0xFF302B26),
    onSurfaceVariant = Color(0xFFB5A9A0),
    surfaceContainerLowest = Color(0xFF13110E),
    surfaceContainerLow = Color(0xFF211D1A),
    surfaceContainer = Color(0xFF28231F),
    surfaceContainerHigh = Color(0xFF302B26),
    surfaceContainerHighest = Color(0xFF3B3530),
    surfaceDim = Color(0xFF191613),
    surfaceBright = Color(0xFF3B3530),
    outline = Color(0xFF7E7269),
    outlineVariant = Color(0xFF4C433C),
    readingInk = Color(0xFFEAE1DA),
    readingInkMuted = Color(0xFFA59A92),
    highlightBg = Color(0xFF442D15),
    highlightBorder = Color(0xFFFFB68E)
)

val LocalVietReaderPalette = staticCompositionLocalOf { IvoryPalette }

// Dynamic color tokens resolving to the active paper theme across the entire app
val Primary: Color @Composable get() = LocalVietReaderPalette.current.primary
val OnPrimary: Color @Composable get() = LocalVietReaderPalette.current.onPrimary
val PrimaryContainer: Color @Composable get() = LocalVietReaderPalette.current.primaryContainer
val OnPrimaryContainer: Color @Composable get() = LocalVietReaderPalette.current.onPrimaryContainer
val PrimaryFixed: Color @Composable get() = LocalVietReaderPalette.current.primaryFixed
val PrimaryFixedDim: Color @Composable get() = LocalVietReaderPalette.current.primaryFixedDim
val OnPrimaryFixed: Color @Composable get() = LocalVietReaderPalette.current.onPrimaryFixed
val OnPrimaryFixedVariant: Color @Composable get() = LocalVietReaderPalette.current.onPrimaryFixedVariant

val Secondary: Color @Composable get() = LocalVietReaderPalette.current.secondary
val OnSecondary: Color @Composable get() = LocalVietReaderPalette.current.onSecondary
val SecondaryContainer: Color @Composable get() = LocalVietReaderPalette.current.secondaryContainer
val OnSecondaryContainer: Color @Composable get() = LocalVietReaderPalette.current.onSecondaryContainer

val Tertiary: Color @Composable get() = LocalVietReaderPalette.current.tertiary
val OnTertiary: Color @Composable get() = LocalVietReaderPalette.current.onTertiary
val TertiaryContainer: Color @Composable get() = LocalVietReaderPalette.current.tertiaryContainer
val OnTertiaryContainer: Color @Composable get() = LocalVietReaderPalette.current.onTertiaryContainer

val ErrorColor: Color @Composable get() = LocalVietReaderPalette.current.error
val OnErrorColor: Color @Composable get() = LocalVietReaderPalette.current.onError
val ErrorContainer: Color @Composable get() = LocalVietReaderPalette.current.errorContainer
val OnErrorContainer: Color @Composable get() = LocalVietReaderPalette.current.onErrorContainer

val Background: Color @Composable get() = LocalVietReaderPalette.current.background
val OnBackground: Color @Composable get() = LocalVietReaderPalette.current.onBackground
val Surface: Color @Composable get() = LocalVietReaderPalette.current.surface
val OnSurface: Color @Composable get() = LocalVietReaderPalette.current.onSurface
val SurfaceVariant: Color @Composable get() = LocalVietReaderPalette.current.surfaceVariant
val OnSurfaceVariant: Color @Composable get() = LocalVietReaderPalette.current.onSurfaceVariant

val SurfaceContainerLowest: Color @Composable get() = LocalVietReaderPalette.current.surfaceContainerLowest
val SurfaceContainerLow: Color @Composable get() = LocalVietReaderPalette.current.surfaceContainerLow
val SurfaceContainer: Color @Composable get() = LocalVietReaderPalette.current.surfaceContainer
val SurfaceContainerHigh: Color @Composable get() = LocalVietReaderPalette.current.surfaceContainerHigh
val SurfaceContainerHighest: Color @Composable get() = LocalVietReaderPalette.current.surfaceContainerHighest
val SurfaceDim: Color @Composable get() = LocalVietReaderPalette.current.surfaceDim
val SurfaceBright: Color @Composable get() = LocalVietReaderPalette.current.surfaceBright

val Outline: Color @Composable get() = LocalVietReaderPalette.current.outline
val OutlineVariant: Color @Composable get() = LocalVietReaderPalette.current.outlineVariant

// Book cover & indicator accents
val Amber200 = Color(0xFFFDE68A)

// Synchronized Reader tokens
val ReadingInk: Color @Composable get() = LocalVietReaderPalette.current.readingInk
val ReadingInkMuted: Color @Composable get() = LocalVietReaderPalette.current.readingInkMuted
val HighlightBg: Color @Composable get() = LocalVietReaderPalette.current.highlightBg
val HighlightBorder: Color @Composable get() = LocalVietReaderPalette.current.highlightBorder
