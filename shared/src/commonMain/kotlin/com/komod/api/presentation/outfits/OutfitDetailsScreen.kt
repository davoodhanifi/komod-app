package com.komod.api.presentation.outfits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.komod.api.domain.model.Outfit
import com.komod.api.domain.model.OutfitItem
import komod.shared.generated.resources.Res
import komod.shared.generated.resources.arrow_left
import komod.shared.generated.resources.arrow_right
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitDetailsScreen(
    outfit: Outfit,
    onNavigateBack: () -> Unit,
    viewModel: OutfitViewModel = koinViewModel(),
    onShowSnackbar: (String) -> Unit = {},
    onItemClick: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSaving = outfit.id in uiState.savingOutfitIds
    val isUnsaving = outfit.id in uiState.unsavingOutfitIds
    val isSaved = outfit.id in uiState.savedOutfitIds

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is OutfitEffect.ShowSnackbar -> onShowSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(text = "Outfit Details", fontWeight = FontWeight.SemiBold, color = OutfitText) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(Res.drawable.arrow_left),
                            contentDescription = "Back",
                            tint = OutfitText,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        containerColor = Color.White,
        bottomBar = {
            Surface(color = Color.White, tonalElevation = 2.dp) {
                Button(
                    onClick = { viewModel.toggleSaveOutfit(outfit) },
                    enabled = !isSaving && !isUnsaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OutfitPurple,
                        contentColor = Color.White,
                        disabledContainerColor = OutfitPurple.copy(alpha = 0.5f),
                        disabledContentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    when {
                        isSaving || isUnsaving -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = if (isSaving) "Saving..." else "Removing...", fontWeight = FontWeight.SemiBold)
                        }
                        isSaved -> {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Saved Outfit", fontWeight = FontWeight.SemiBold)
                        }
                        else -> {
                            Text(text = "Save Outfit", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                OutfitImageCollage(
                    wardrobeItems = outfit.items,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(360.dp),
                )
            }
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Text(
                        text = outfit.name,
                        color = OutfitText,
                        fontSize = 28.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutfitMatchBadge(score = outfit.matchScore)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = outfit.reason,
                        color = OutfitMuted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                }
            }
            item {
                Text(
                    text = "Items in this outfit",
                    color = OutfitText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            items(outfit.items.zip(outfit.wardrobeItemIds)) { (item, wardrobeItemId) ->
                OutfitDetailsItemRow(
                    item = item,
                    onClick = { onItemClick(wardrobeItemId) },
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun OutfitMatchBadge(score: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(OutfitPurpleSoft)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$score% match",
            color = OutfitPurple,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun OutfitDetailsItemRow(
    item: OutfitItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = OutfitSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White),
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.category?.toDisplayLabel() ?: "Item",
                    color = OutfitText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                item.subcategory?.toDisplayLabel()?.let { subcategory ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subcategory,
                        color = OutfitMuted,
                        fontSize = 13.sp,
                    )
                }
            }
            Icon(
                painter = painterResource(Res.drawable.arrow_right),
                contentDescription = null,
                tint = OutfitMuted,
            )
        }
    }
}

private fun String.toDisplayLabel(): String = split('_', '-', ' ')
    .filter { it.isNotBlank() }
    .joinToString(" ") { part -> part.replaceFirstChar { c -> c.uppercase() } }
