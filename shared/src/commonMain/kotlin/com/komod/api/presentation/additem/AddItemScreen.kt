package com.komod.api.presentation.additem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.komod.api.platform.ImagePickerLauncher
import com.komod.api.platform.rememberImagePickerLauncher
import com.komod.api.platform.toImageBitmap
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

private val Purple = Color(0xFF7C5CFC)
private val LightLavender = Color(0xFFEDE9FF)
private val DarkText = Color(0xFF111827)
private val GrayText = Color(0xFF6B7280)
private val BorderColor = Color(0xFFE5E7EB)

@Composable
fun AddItemScreen(
    viewModel: AddItemViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val imagePickerLauncher = rememberImagePickerLauncher(
        onResult = { bytes, mimeType -> viewModel.onImageSelected(bytes, mimeType) },
    )

    when (val state = uiState) {
        AddItemUiState.Initial -> AddItemInitialContent(
            launcher = imagePickerLauncher,
            onNavigateBack = onNavigateBack,
        )

        is AddItemUiState.Uploading -> AddItemUploadingContent(
            state = state,
            onNavigateBack = onNavigateBack,
        )

        AddItemUiState.Success -> {
            LaunchedEffect(Unit) {
                delay(2_000)
                onNavigateBack()
            }
            AddItemSuccessContent()
        }

        is AddItemUiState.Error -> AddItemErrorContent(
            state = state,
            onRetry = { viewModel.retry() },
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                text = "Add a photo of your clothing item and let AI analyze it",
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
                        imageVector = Icons.Filled.CameraAlt,
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
                        imageVector = Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = Purple,
                    )
                },
                title = "Choose from Gallery",
                subtitle = "Select an existing photo from your gallery",
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
                    imageVector = Icons.Outlined.Checkroom,
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
            imageVector = Icons.Filled.ChevronRight,
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
    val bitmap = remember(state.imageThumbnail) { state.imageThumbnail.toImageBitmap() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                Image(
                    bitmap = bitmap,
                    contentDescription = "Selected clothing item",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = "Uploading image...",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                    )
                    Text(
                        text = state.storagePath.ifBlank { "Preparing upload..." },
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayText,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            LinearProgressIndicator(
                progress = { state.overallProgress },
                color = Purple,
                trackColor = LightLavender,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50)),
            )
            Text(
                text = "${(state.overallProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = GrayText,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp),
            )
            Column(
                modifier = Modifier.padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.steps.forEach { step ->
                    UploadStepRow(step = step)
                }
            }
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
                    text = "This may take a few moments. We'll notify you when it's ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkText,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun UploadStepRow(step: UploadStep) {
    val statusLabel = when (step.status) {
        StepStatus.Completed -> "Completed"
        StepStatus.InProgress -> "In progress..."
        StepStatus.Pending -> "Pending"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (step.status) {
            StepStatus.Completed -> Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Purple,
            )

            StepStatus.InProgress -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Purple,
                strokeWidth = 2.dp,
            )

            StepStatus.Pending -> Icon(
                imageVector = Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = GrayText,
            )
        }
        Text(
            text = step.label,
            style = MaterialTheme.typography.bodyLarge,
            color = DarkText,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = GrayText,
        )
    }
}

@Composable
private fun AddItemSuccessContent() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(80.dp),
            )
            Text(
                text = "Items Added!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DarkText,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = "Your wardrobe has been updated",
                style = MaterialTheme.typography.bodyLarge,
                color = GrayText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
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
                title = { Text(text = "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
