package com.komod.api.presentation.additem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komod.api.domain.model.BoundingBox
import com.komod.api.platform.ImagePickerLauncher
import com.komod.api.platform.rememberImagePickerLauncher
import com.komod.api.presentation.cropeditor.PreUploadCropScreen
import komod.shared.generated.resources.Res
import komod.shared.generated.resources.arrow_left
import komod.shared.generated.resources.arrow_right
import komod.shared.generated.resources.camera
import komod.shared.generated.resources.gallery
import komod.shared.generated.resources.hanger
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

private val Purple = Color(0xFF7C5CFC)
private val LightLavender = Color(0xFFEDE9FF)
private val DarkText = Color(0xFF111827)
private val GrayText = Color(0xFF6B7280)
private val BorderColor = Color(0xFFE5E7EB)

private fun photoWord(count: Int): String = if (count == 1) "photo" else "photos"

@Composable
fun AddItemScreen(
    viewModel: AddItemViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onShowSnackbar: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val imagePickerLauncher = rememberImagePickerLauncher(
        onResult = { images -> viewModel.onImagesSelected(images) },
    )

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AddItemEffect.SelectionLimitExceeded ->
                    onShowSnackbar("You can upload up to 5 photos at a time.")

                is AddItemEffect.UploadsSucceeded -> {
                    onShowSnackbar(
                        "${effect.count} ${photoWord(effect.count)} uploaded successfully. " +
                            "We'll analyze them in the background.",
                    )
                    onNavigateBack()
                }

                is AddItemEffect.CropFailed ->
                    onShowSnackbar("Couldn't apply a crop, so that photo will upload as originally taken.")
            }
        }
    }

    when (val state = uiState) {
        AddItemUiState.Initial -> AddItemInitialContent(
            launcher = imagePickerLauncher,
            onNavigateBack = onNavigateBack,
        )

        is AddItemUiState.Reviewing -> {
            val activePhoto = state.photos.firstOrNull { it.id == state.activeCropPhotoId }
            val isSinglePhoto = state.photos.size == 1

            if (activePhoto != null) {
                PreUploadCropScreen(
                    photo = activePhoto.original,
                    initialBoundingBox = activePhoto.cropBox ?: BoundingBox.FullImage,
                    title = "Crop Photo",
                    subtitle = if (isSinglePhoto) null else {
                        val index = state.photos.indexOfFirst { it.id == activePhoto.id } + 1
                        "Photo $index of ${state.photos.size}"
                    },
                    primaryButtonLabel = if (isSinglePhoto) "Continue Uploading" else "Save Crop",
                    isSaving = state.savingCropPhotoId == activePhoto.id,
                    onBack = { if (isSinglePhoto) viewModel.reset() else viewModel.closeCrop() },
                    onConfirm = { boundingBox ->
                        if (isSinglePhoto) {
                            viewModel.confirmCropAndContinue(activePhoto.id, boundingBox)
                        } else {
                            viewModel.applyCrop(activePhoto.id, boundingBox)
                        }
                    },
                )
            } else {
                ReviewPhotosScreen(
                    photos = state.photos,
                    onCropClick = { photoId -> viewModel.openCrop(photoId) },
                    onDeleteClick = { photoId -> viewModel.removePhoto(photoId) },
                    onContinueUploading = { viewModel.continueUploading() },
                    onBack = { viewModel.reset() },
                )
            }
        }

        is AddItemUiState.Uploading -> AddItemUploadingContent(
            state = state,
            onNavigateBack = onNavigateBack,
        )

        is AddItemUiState.Error -> AddItemErrorContent(
            state = state,
            onRetry = { viewModel.retryFailed() },
            onChooseAnother = { viewModel.reset() },
            onNavigateBack = onNavigateBack,
        )
    }
}

@Composable
private fun AddItemInitialContent(
    launcher: ImagePickerLauncher,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(text = "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(Res.drawable.arrow_left),
                            contentDescription = "Back",
                            tint = DarkText,
                        )
                    }
                },
            )
        },
        containerColor = Color.White,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Add New Item",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DarkText,
            )
            Text(
                text = "Add up to 5 photos of your clothing items and let AI analyze them",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayText,
                modifier = Modifier.padding(top = 8.dp),
            )
            AddItemHeroCard(modifier = Modifier.padding(top = 24.dp))
            Text(
                text = "Choose a source",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = DarkText,
                modifier = Modifier.padding(top = 24.dp),
            )
            SourceOptionCard(
                icon = {
                    Icon(
                        painter = painterResource(Res.drawable.camera),
                        contentDescription = null,
                        tint = Purple,
                    )
                },
                title = "Take a Photo",
                subtitle = "Use your camera to take a new photo",
                onClick = launcher::launchCamera,
                modifier = Modifier.padding(top = 16.dp),
            )
            SourceOptionCard(
                icon = {
                    Icon(
                        painter = painterResource(Res.drawable.gallery),
                        contentDescription = null,
                        tint = Purple,
                    )
                },
                title = "Choose from Gallery",
                subtitle = "Select up to 5 existing photos from your gallery",
                onClick = launcher::launchGallery,
                modifier = Modifier.padding(top = 12.dp),
            )
            PrivacyNoteCard(modifier = Modifier.padding(top = 24.dp, bottom = 32.dp))
        }
    }
}

@Composable
private fun AddItemHeroCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(LightLavender)
            .padding(20.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Purple.copy(alpha = 0.6f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp),
        )
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = Purple.copy(alpha = 0.35f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
                .size(18.dp),
        )
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(150.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(Res.drawable.hanger),
                    contentDescription = null,
                    tint = Purple,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                        .height(6.dp)
                        .width(120.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Purple.copy(alpha = 0.12f)),
                )
            }
        }
    }
}

@Composable
private fun SourceOptionCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(LightLavender),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = DarkText,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = GrayText,
            )
        }
        Icon(
            painter = painterResource(Res.drawable.arrow_right),
            contentDescription = null,
            tint = GrayText,
        )
    }
}

@Composable
private fun PrivacyNoteCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LightLavender.copy(alpha = 0.7f))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = Purple,
            modifier = Modifier.padding(top = 2.dp),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = "Your privacy matters",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = DarkText,
            )
            Text(
                text = "Your photos are private and secure. Only you can see your wardrobe.",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayText,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun AddItemUploadingContent(
    state: AddItemUiState.Uploading,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = {
                    Text(
                        text = "Uploading",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkText,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(Res.drawable.arrow_left),
                            contentDescription = "Back",
                            tint = DarkText,
                        )
                    }
                },
            )
        },
        containerColor = Color.White,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LightLavender),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.gallery),
                        contentDescription = null,
                        tint = Purple,
                    )
                }
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = "Uploading ${state.total} ${photoWord(state.total)}...",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                    )
                    Text(
                        text = "${state.completed} of ${state.total} completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { state.progress },
                color = Purple,
                trackColor = LightLavender,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50)),
            )
            Text(
                text = "${(state.progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayText,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightLavender.copy(alpha = 0.7f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Purple,
                )
                Text(
                    text = "Feel free to keep using the app — we'll analyze your photos in the background.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkText,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun AddItemErrorContent(
    state: AddItemUiState.Error,
    onRetry: () -> Unit,
    onChooseAnother: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(text = "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(Res.drawable.arrow_left),
                            contentDescription = "Back",
                            tint = DarkText,
                        )
                    }
                },
            )
        },
        containerColor = Color.White,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(72.dp),
            )
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyLarge,
                color = DarkText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = "${state.failedPhotos.size} ${photoWord(state.failedPhotos.size)} ready to retry.",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple,
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
            ) {
                Text(text = "Retry")
            }
            OutlinedButton(
                onClick = onChooseAnother,
                border = BorderStroke(1.dp, Purple),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                Text(text = "Choose Another Photo", color = Purple)
            }
        }
    }
}
