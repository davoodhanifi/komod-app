package com.komod.api.presentation.wardrobe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.komod.api.domain.model.WardrobeItemDetail
import com.komod.api.data.api.model.WardrobeItemStatus
import komod.shared.generated.resources.Res
import komod.shared.generated.resources.arrow_left
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val EditBg = Color(0xFFF7F8FC)
private val EditPrimary = Color(0xFF7456FF)
private val EditDark = Color(0xFF101828)
private val EditMuted = Color(0xFF667085)
private val EditSurface = Color(0xFFFFFFFF)
private val EditBorder = Color(0xFFE7EAF0)
private val EditChipBg = Color(0xFFF4F1FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WardrobeItemEditScreen(
    wardrobeItemId: String,
    onNavigateBack: () -> Unit,
    onRefreshDetail: () -> Unit,
    onRefreshWardrobe: () -> Unit,
    onRefreshHome: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    viewModel: WardrobeItemEditViewModel = koinViewModel(parameters = { parametersOf(wardrobeItemId) }),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                WardrobeItemEditEffect.Saved -> {
                    onShowSnackbar("Item updated.")
                    onRefreshDetail()
                    onRefreshWardrobe()
                    onRefreshHome()
                    onNavigateBack()
                }
                is WardrobeItemEditEffect.SaveFailed -> onShowSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(text = "Edit Item", fontWeight = FontWeight.SemiBold, color = EditDark) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(Res.drawable.arrow_left),
                            contentDescription = "Back",
                            tint = EditDark,
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White,
                ),
            )
        },
        containerColor = Color.White,
        bottomBar = {
            val readyState = uiState as? WardrobeItemEditUiState.Ready
            if (readyState != null) {
                Surface(color = Color.White, tonalElevation = 2.dp) {
                    val isSaving = readyState.content.isSaving
                    Button(
                        onClick = viewModel::saveChanges,
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = EditPrimary),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                        }
                        Text(text = "Save Changes", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
    ) { padding ->
        when (val state = uiState) {
            WardrobeItemEditUiState.Loading -> EditLoadingContent(modifier = Modifier.padding(padding))
            is WardrobeItemEditUiState.Error -> EditErrorContent(
                message = state.message,
                onRetry = viewModel::loadItem,
                modifier = Modifier.padding(padding),
            )
            is WardrobeItemEditUiState.Ready -> EditFormContent(
                content = state.content,
                onCategoryChange = viewModel::updateCategory,
                onSubcategoryChange = viewModel::updateSubcategory,
                onPrimaryColorChange = viewModel::updatePrimaryColor,
                onFormalityChange = viewModel::updateFormality,
                onSeasonToggle = viewModel::toggleSeason,
                onOccasionToggle = viewModel::toggleOccasion,
                onStatusChange = viewModel::updateStatus,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EditFormContent(
    content: WardrobeItemEditContent,
    onCategoryChange: (String) -> Unit,
    onSubcategoryChange: (String) -> Unit,
    onPrimaryColorChange: (String) -> Unit,
    onFormalityChange: (String) -> Unit,
    onSeasonToggle: (String) -> Unit,
    onOccasionToggle: (String) -> Unit,
    onStatusChange: (WardrobeItemStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        EditHeroCard(item = content.item)
        EditSectionCard(title = "Classification") {
            DropdownField(
                label = "Category",
                value = content.form.category,
                options = content.form.category.optionsWithCurrent(WardrobeCategoryOptions),
                enabled = !content.isSaving,
                onSelected = onCategoryChange,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = content.form.subcategory,
                onValueChange = onSubcategoryChange,
                label = { Text("Subcategory") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !content.isSaving,
                shape = RoundedCornerShape(18.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = content.form.primaryColor,
                onValueChange = onPrimaryColorChange,
                label = { Text("Primary Color") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !content.isSaving,
                shape = RoundedCornerShape(18.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            DropdownField(
                label = "Formality",
                value = content.form.formality,
                options = content.form.formality.optionsWithCurrent(WardrobeFormalityOptions),
                enabled = !content.isSaving,
                onSelected = onFormalityChange,
            )
        }
        EditSectionCard(title = "Seasons") {
            MultiSelectChips(
                options = content.form.seasons.optionsWithCurrent(WardrobeSeasonOptions),
                selectedValues = content.form.seasons,
                enabled = !content.isSaving,
                onToggle = onSeasonToggle,
            )
        }
        EditSectionCard(title = "Occasions") {
            MultiSelectChips(
                options = content.form.occasions.optionsWithCurrent(WardrobeOccasionOptions),
                selectedValues = content.form.occasions,
                enabled = !content.isSaving,
                onToggle = onOccasionToggle,
            )
        }
        EditSectionCard(title = "Status") {
            DropdownField(
                label = "Status",
                value = content.form.status.toDisplayLabel(),
                options = WardrobeStatusOptions.map { it.toDisplayLabel() },
                enabled = !content.isSaving,
                onSelected = { selected ->
                    WardrobeStatusOptions.firstOrNull { it.toDisplayLabel() == selected }?.let(onStatusChange)
                },
                transformInput = { selected ->
                    WardrobeStatusOptions.firstOrNull { it.toDisplayLabel() == selected }?.toDisplayLabel()
                        ?: selected
                },
            )
        }
        Spacer(modifier = Modifier.height(96.dp))
    }
}

@Composable
private fun EditHeroCard(item: WardrobeItemDetail) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = EditSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(EditChipBg),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!item.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.itemName ?: item.category,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(text = item.category.toWardrobeLabel(), color = EditPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.itemName?.takeIf { it.isNotBlank() }?.toWardrobeLabel() ?: item.category.toWardrobeLabel(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = EditDark,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Update the fields below and save your changes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EditMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = EditSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = EditDark)
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean,
    onSelected: (String) -> Unit,
    transformInput: (String) -> String = { it },
) {
    val expanded = remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded.value,
        onExpandedChange = { if (enabled) expanded.value = !expanded.value },
    ) {
        OutlinedTextField(
            value = value.toWardrobeLabel(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        )
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.toWardrobeLabel()) },
                    onClick = {
                        expanded.value = false
                        onSelected(transformInput(option))
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiSelectChips(
    options: List<String>,
    selectedValues: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selectedValues.contains(option),
                onClick = { if (enabled) onToggle(option) },
                enabled = enabled,
                label = { Text(option.toWardrobeLabel()) },
                leadingIcon = if (selectedValues.contains(option)) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else {
                    null
                },
            )
        }
    }
}

private fun String.optionsWithCurrent(allowed: List<String>): List<String> {
    return if (isBlank() || allowed.contains(this)) allowed else listOf(this) + allowed
}

private fun Set<String>.optionsWithCurrent(allowed: List<String>): List<String> {
    val current = filter { it.isNotBlank() && it !in allowed }
    return current + allowed
}

@Composable
private fun EditLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(4) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = EditSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp))
            }
        }
    }
}

@Composable
private fun EditErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Couldn't load item", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = EditDark)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = message, color = EditMuted)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = EditPrimary)) {
            Text("Retry")
        }
    }
}
