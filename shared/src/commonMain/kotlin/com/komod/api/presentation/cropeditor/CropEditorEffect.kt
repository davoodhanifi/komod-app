package com.komod.api.presentation.cropeditor

sealed interface CropEditorEffect {
    data object CropSaved : CropEditorEffect

    data class SaveFailed(val message: String) : CropEditorEffect
}
