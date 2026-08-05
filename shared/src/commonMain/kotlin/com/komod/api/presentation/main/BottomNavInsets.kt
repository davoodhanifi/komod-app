package com.komod.api.presentation.main

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * The measured height of the floating [BottomNavigationBar] (including any system
 * navigation-bar/safe-area inset it already applies), as observed by [MainScaffold].
 *
 * Screens hosted inside [MainScaffold] read this to pad their scrollable content so the
 * last row/item isn't obscured by the overlay bar, without hardcoding its height.
 */
val LocalBottomNavBarHeight = compositionLocalOf { 0.dp }
