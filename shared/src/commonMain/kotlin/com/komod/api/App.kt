package com.komod.api

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.komod.api.data.repository.AuthRepository
import com.komod.api.presentation.additem.AddItemScreen
import com.komod.api.presentation.auth.LoginScreen
import com.komod.api.presentation.wardrobe.WardrobeItemDetailScreen
import com.komod.api.presentation.wardrobe.WardrobeScreen
import io.github.jan.supabase.auth.status.SessionStatus
import org.koin.compose.koinInject

private val KomodLightColorScheme = lightColorScheme(
    primary = Color(0xFF111827),
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = Color(0xFF6B7280)
)

private sealed class AppDestination {
    data object Wardrobe : AppDestination()
    data object AddItem : AppDestination()
    data class WardrobeItemDetail(val wardrobeItemId: String) : AppDestination()
}

@Composable
fun App(
    authRepository: AuthRepository = koinInject(),
) {
    val sessionStatus by authRepository.sessionStatus.collectAsState()

    MaterialTheme(colorScheme = KomodLightColorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (sessionStatus) {
                SessionStatus.Initializing -> LoadingScreen()
                is SessionStatus.Authenticated -> {
                    var destination by remember { mutableStateOf<AppDestination>(AppDestination.Wardrobe) }
                    var wardrobeRefreshKey by remember { mutableIntStateOf(0) }

                    when (val dest = destination) {
                        AppDestination.Wardrobe -> WardrobeScreen(
                            refreshKey = wardrobeRefreshKey,
                            onAddItem = { destination = AppDestination.AddItem },
                            onItemClick = { id -> destination = AppDestination.WardrobeItemDetail(id) },
                        )

                        AppDestination.AddItem -> AddItemScreen(
                            onNavigateBack = {
                                wardrobeRefreshKey++
                                destination = AppDestination.Wardrobe
                            },
                        )

                        is AppDestination.WardrobeItemDetail -> WardrobeItemDetailScreen(
                            wardrobeItemId = dest.wardrobeItemId,
                            onNavigateBack = { destination = AppDestination.Wardrobe },
                        )
                    }
                }

                else -> LoginScreen()
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(
            text = "Checking your session…",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}
