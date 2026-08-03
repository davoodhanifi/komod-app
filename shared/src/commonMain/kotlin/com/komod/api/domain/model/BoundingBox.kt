package com.komod.api.domain.model

/**
 * A crop rectangle in normalized image coordinates (0f..1f), origin at the top-left.
 */
data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    enum class Corner { TopLeft, TopRight, BottomLeft, BottomRight }

    /** Translates the box by a normalized delta, clamped to stay within the image bounds. */
    fun moved(dx: Float, dy: Float): BoundingBox {
        val newX = (x + dx).coerceIn(0f, 1f - width)
        val newY = (y + dy).coerceIn(0f, 1f - height)
        return copy(x = newX, y = newY)
    }

    /**
     * Drags the given [corner] by a normalized delta, keeping the opposite corner fixed and
     * enforcing a minimum size so the box can never collapse or invert.
     */
    fun resized(corner: Corner, dx: Float, dy: Float): BoundingBox = when (corner) {
        Corner.TopLeft -> {
            val newX = (x + dx).coerceIn(0f, x + width - MinFraction)
            val newY = (y + dy).coerceIn(0f, y + height - MinFraction)
            copy(x = newX, y = newY, width = width + (x - newX), height = height + (y - newY))
        }

        Corner.TopRight -> {
            val newY = (y + dy).coerceIn(0f, y + height - MinFraction)
            copy(
                y = newY,
                width = (width + dx).coerceIn(MinFraction, 1f - x),
                height = height + (y - newY),
            )
        }

        Corner.BottomLeft -> {
            val newX = (x + dx).coerceIn(0f, x + width - MinFraction)
            copy(
                x = newX,
                width = width + (x - newX),
                height = (height + dy).coerceIn(MinFraction, 1f - y),
            )
        }

        Corner.BottomRight -> copy(
            width = (width + dx).coerceIn(MinFraction, 1f - x),
            height = (height + dy).coerceIn(MinFraction, 1f - y),
        )
    }

    companion object {
        /** Smallest allowed box dimension, as a fraction of the image size. */
        const val MinFraction = 0.08f

        val FullImage = BoundingBox(x = 0f, y = 0f, width = 1f, height = 1f)
    }
}
