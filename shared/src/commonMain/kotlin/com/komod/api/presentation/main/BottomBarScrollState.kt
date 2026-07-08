package com.komod.api.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.grid.LazyGridState

/**
 * Manages the visibility state of bottom navigation bar labels based on scroll behavior.
 * When scrolling down, labels are hidden immediately (Instagram-like behavior).
 * When scrolling up, labels are shown immediately.
 */
class BottomBarScrollState {
    private var lastScrollPosition by mutableFloatStateOf(0f)
    private var _isExpanded by mutableFloatStateOf(1f) // 1f = expanded, 0f = collapsed
    
    val isExpanded: Float
        get() = _isExpanded
    
    /**
     * Update scroll position and determine if labels should be shown
     * @param currentScroll Current scroll position
     */
    fun updateScroll(currentScroll: Float) {
        val delta = currentScroll - lastScrollPosition
        
        // Immediate response to scroll direction
        when {
            // Scrolling down - hide labels immediately
            delta > 0 && currentScroll > 10f -> {
                _isExpanded = 0f
            }
            // Scrolling up - show labels immediately
            delta < 0 -> {
                _isExpanded = 1f
            }
            // At the very top - always show labels
            currentScroll <= 10f -> {
                _isExpanded = 1f
            }
        }
        
        lastScrollPosition = currentScroll
    }
    
    /**
     * Force expand the labels (e.g., when navigating to a new screen)
     */
    fun expand() {
        _isExpanded = 1f
        lastScrollPosition = 0f
    }
    
    /**
     * Reset to initial state
     */
    fun reset() {
        lastScrollPosition = 0f
        _isExpanded = 1f
    }
}

/**
 * Track scroll changes for regular ScrollState
 */
@Composable
fun rememberBottomBarScrollBehavior(
    scrollState: ScrollState,
    bottomBarScrollState: BottomBarScrollState
): State<Float> {
    return remember(scrollState) {
        derivedStateOf {
            val currentScroll = scrollState.value.toFloat()
            bottomBarScrollState.updateScroll(currentScroll)
            bottomBarScrollState.isExpanded
        }
    }
}

/**
 * Track scroll changes for LazyGridState
 */
@Composable
fun rememberBottomBarScrollBehavior(
    lazyGridState: LazyGridState,
    bottomBarScrollState: BottomBarScrollState
): State<Float> {
    return remember(lazyGridState) {
        derivedStateOf {
            val firstVisibleItemIndex = lazyGridState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = lazyGridState.firstVisibleItemScrollOffset
            val currentScroll = (firstVisibleItemIndex * 1000f) + firstVisibleItemScrollOffset
            
            bottomBarScrollState.updateScroll(currentScroll)
            bottomBarScrollState.isExpanded
        }
    }
}
