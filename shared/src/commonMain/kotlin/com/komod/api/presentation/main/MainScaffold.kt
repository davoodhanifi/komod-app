package com.komod.api.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.komod.api.data.repository.AuthRepository
import com.komod.api.presentation.additem.AddItemScreen
import com.komod.api.presentation.home.HomeScreen
import com.komod.api.presentation.outfits.OutfitScreen
import com.komod.api.presentation.wardrobe.WardrobeItemDetailScreen
import com.komod.api.presentation.wardrobe.WardrobeScreen
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

private const val WardrobeItemIdArg = "wardrobeItemId"
private const val WardrobeRefreshArg = "wardrobe_refresh_required"
private const val SelectedOutfitKey = "selected_outfit"

private sealed class MainRoute(val route: String) {
    data object Home : MainRoute("home")
    data object Wardrobe : MainRoute("wardrobe")
    data object Outfits : MainRoute("outfits")
    data object Profile : MainRoute("profile")
    data object AddItem : MainRoute("wardrobe/add-item")
    data object WardrobeItemDetail : MainRoute("wardrobe/item/{$WardrobeItemIdArg}") {
        fun createRoute(itemId: String): String = "wardrobe/item/$itemId"
    }
    data object OutfitDetails : MainRoute("outfits/details")
}

private val MainTabs = listOf(
    BottomNavItemUi(
        route = MainRoute.Home.route,
        label = "Home",
        activeIcon = Icons.Filled.Home,
        inactiveIcon = Icons.Outlined.Home,
    ),
    BottomNavItemUi(
        route = MainRoute.Wardrobe.route,
        label = "My Komod",
        activeIcon = Icons.Filled.Checkroom,
        inactiveIcon = Icons.Outlined.Checkroom,
    ),
    BottomNavItemUi(
        route = MainRoute.Outfits.route,
        label = "Outfits",
        activeIcon = Icons.Filled.AutoAwesome,
        inactiveIcon = Icons.Outlined.AutoAwesome,
    ),
    BottomNavItemUi(
        route = MainRoute.Profile.route,
        label = "Profile",
        activeIcon = Icons.Filled.Person,
        inactiveIcon = Icons.Outlined.Person,
    ),
)

@Composable
fun MainScaffold(
    authRepository: AuthRepository = koinInject(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Hide bottom bar only for specific full-screen flows
    val hideBottomBarRoutes = setOf(
        MainRoute.AddItem.route,
        MainRoute.OutfitDetails.route,
    )
    val showBottomBar = currentRoute !in hideBottomBarRoutes
    
    // Determine which tab should be selected based on current route
    val selectedTabRoute = when {
        currentRoute == MainRoute.Home.route -> MainRoute.Home.route
        currentRoute == MainRoute.Wardrobe.route -> MainRoute.Wardrobe.route
        currentRoute?.startsWith("wardrobe/") == true -> MainRoute.Wardrobe.route
        currentRoute == MainRoute.Outfits.route -> MainRoute.Outfits.route
        currentRoute?.startsWith("outfits/") == true -> MainRoute.Outfits.route
        currentRoute == MainRoute.Profile.route -> MainRoute.Profile.route
        else -> null
    }
    
    var wardrobeRefreshKey by rememberSaveable { mutableIntStateOf(0) }

    val currentUser = authRepository.currentUserOrNull()

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    items = MainTabs,
                    selectedRoute = selectedTabRoute,
                    onItemSelected = { item ->
                        navController.navigate(item.route) {
                            // Pop up to the start destination (Home) and save state
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = MainRoute.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            composable(MainRoute.Home.route) {
                HomeScreen(
                    user = currentUser,
                    onGenerateOutfit = { occasion ->
                        // TODO: Navigate to outfit generation flow
                        // Navigate to Outfits using the same pattern as bottom nav
                        navController.navigate(MainRoute.Outfits.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onViewWardrobe = {
                        // Navigate to Wardrobe using the same pattern as bottom nav
                        navController.navigate(MainRoute.Wardrobe.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onItemClick = { itemId ->
                        navController.navigate(MainRoute.WardrobeItemDetail.createRoute(itemId))
                    },
                )
            }

            composable(MainRoute.Wardrobe.route) { backStackEntry ->
                val shouldRefresh by backStackEntry.savedStateHandle
                    .getStateFlow(WardrobeRefreshArg, false)
                    .collectAsState()

                LaunchedEffect(shouldRefresh) {
                    if (shouldRefresh) {
                        wardrobeRefreshKey += 1
                        backStackEntry.savedStateHandle[WardrobeRefreshArg] = false
                    }
                }

                WardrobeScreen(
                    refreshKey = wardrobeRefreshKey,
                    onAddItem = { navController.navigate(MainRoute.AddItem.route) },
                    onItemClick = { itemId ->
                        navController.navigate(MainRoute.WardrobeItemDetail.createRoute(itemId))
                    },
                )
            }

            composable(MainRoute.Outfits.route) {
                OutfitScreen(
                    onOpenDetails = { outfit ->
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            SelectedOutfitKey,
                            Json.encodeToString(outfit.toNavigationPayload()),
                        )
                        navController.navigate(MainRoute.OutfitDetails.route)
                    },
                )
            }

            composable(MainRoute.Profile.route) {
                MainTabPlaceholder(
                    title = "Profile",
                    subtitle = "Manage your account and preferences.",
                )
            }

            composable(MainRoute.AddItem.route) {
                AddItemScreen(
                    onNavigateBack = {
                        navController.previousBackStackEntry?.savedStateHandle?.set(WardrobeRefreshArg, true)
                        navController.popBackStack()
                    },
                )
            }

            composable(
                route = MainRoute.WardrobeItemDetail.route,
                arguments = listOf(
                    navArgument(WardrobeItemIdArg) { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val itemId = backStackEntry.savedStateHandle.get<String>(WardrobeItemIdArg).orEmpty()
                WardrobeItemDetailScreen(
                    wardrobeItemId = itemId,
                    onNavigateBack = { navController.navigateUp() },
                )
            }

            composable(MainRoute.OutfitDetails.route) { backStackEntry ->
                backStackEntry.savedStateHandle.get<String>(SelectedOutfitKey)
                OutfitDetailsPlaceholder()
            }
        }
    }
}

@Serializable
private data class OutfitNavigationPayload(
    val name: String,
    val reason: String,
    val matchScore: Int,
    val wardrobeItemIds: List<String>,
)

private fun com.komod.api.domain.model.Outfit.toNavigationPayload(): OutfitNavigationPayload {
    return OutfitNavigationPayload(
        name = name,
        reason = reason,
        matchScore = matchScore,
        wardrobeItemIds = wardrobeItemIds,
    )
}

@Composable
private fun MainTabPlaceholder(
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun OutfitDetailsPlaceholder() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Outfit Details",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Coming soon",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
