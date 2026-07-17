package com.komod.api.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import org.jetbrains.compose.resources.painterResource
import komod.shared.generated.resources.Res
import komod.shared.generated.resources.hanger
import komod.shared.generated.resources.hanger_filled
import komod.shared.generated.resources.home
import komod.shared.generated.resources.home_filled
import komod.shared.generated.resources.user
import komod.shared.generated.resources.user_filled
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
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
import com.komod.api.presentation.profile.ProfileScreen
import com.komod.api.presentation.wardrobe.WardrobeItemDetailScreen
import com.komod.api.presentation.wardrobe.WardrobeScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.compose.koinInject

private const val WardrobeItemIdArg = "wardrobeItemId"
private const val WardrobeRefreshArg = "wardrobe_refresh_required"
private const val HomeRefreshArg = "home_refresh_required"
private const val SelectedOutfitKey = "selected_outfit"
private const val OccasionArg = "occasion"

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

@Composable
private fun getMainTabs() = listOf(
    BottomNavItemUi(
        route = MainRoute.Home.route,
        label = "Home",
        activeIcon = painterResource(Res.drawable.home_filled),
        inactiveIcon = painterResource(Res.drawable.home),
    ),
    BottomNavItemUi(
        route = MainRoute.Wardrobe.route,
        label = "My Wardrobe",
        activeIcon = painterResource(Res.drawable.hanger_filled),
        inactiveIcon = painterResource(Res.drawable.hanger),
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
        activeIcon = painterResource(Res.drawable.user_filled),
        inactiveIcon = painterResource(Res.drawable.user),
    ),
)

@Composable
fun MainScaffold(
    authRepository: AuthRepository = koinInject(),
) {
    val uriHandler = LocalUriHandler.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Scroll states for each screen
    val homeScrollState = rememberScrollState()
    val wardrobeScrollState = rememberLazyGridState()
    val profileScrollState = rememberScrollState()
    
    // Bottom bar scroll behavior
    val bottomBarScrollState = remember { BottomBarScrollState() }
    
    // Track scroll for the current screen
    LaunchedEffect(currentRoute) {
        // Expand labels when navigating to a new screen
        bottomBarScrollState.expand()
    }
    
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
        currentRoute?.startsWith("outfits") == true -> MainRoute.Outfits.route  // Matches "outfits" and "outfits?..."
        currentRoute == MainRoute.Profile.route -> MainRoute.Profile.route
        else -> null
    }
    
    var wardrobeRefreshKey by rememberSaveable { mutableIntStateOf(0) }
    var homeRefreshKey by rememberSaveable { mutableIntStateOf(0) }

    fun requestRefresh(route: String, key: String) {
        runCatching { navController.getBackStackEntry(route) }
            .getOrNull()
            ?.savedStateHandle
            ?.set(key, true)
    }

    fun showSnackbar(message: String) {
        snackbarScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    val currentUser by authRepository.currentUser.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.White,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { contentPadding ->
            NavHost(
                navController = navController,
                startDestination = MainRoute.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(bottom = if (showBottomBar) 0.dp else 0.dp),
            ) {
                composable(MainRoute.Home.route) { backStackEntry ->
                    val shouldRefresh by backStackEntry.savedStateHandle
                        .getStateFlow(HomeRefreshArg, false)
                        .collectAsState()

                    LaunchedEffect(shouldRefresh) {
                        if (shouldRefresh) {
                            homeRefreshKey += 1
                            backStackEntry.savedStateHandle[HomeRefreshArg] = false
                        }
                    }

                    // Track scroll behavior - immediate response
                    LaunchedEffect(homeScrollState.value) {
                        bottomBarScrollState.updateScroll(homeScrollState.value.toFloat())
                    }
                    
                    HomeScreen(
                        user = currentUser,
                        scrollState = homeScrollState,
                        onGenerateOutfit = { occasion ->
                            // Navigate to Outfits with the selected occasion
                            navController.navigate("${MainRoute.Outfits.route}?$OccasionArg=$occasion") {
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
                        onProfileClick = {
                            navController.navigate(MainRoute.Profile.route) {
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
                        refreshKey = homeRefreshKey,
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
                    
                    // Track scroll behavior for wardrobe grid - immediate response
                    LaunchedEffect(wardrobeScrollState.firstVisibleItemIndex, wardrobeScrollState.firstVisibleItemScrollOffset) {
                        val firstVisibleItemIndex = wardrobeScrollState.firstVisibleItemIndex
                        val firstVisibleItemScrollOffset = wardrobeScrollState.firstVisibleItemScrollOffset
                        val currentScroll = (firstVisibleItemIndex * 1000f) + firstVisibleItemScrollOffset
                        bottomBarScrollState.updateScroll(currentScroll)
                    }

                    WardrobeScreen(
                        refreshKey = wardrobeRefreshKey,
                        lazyGridState = wardrobeScrollState,
                        onAddItem = { navController.navigate(MainRoute.AddItem.route) },
                        onItemClick = { itemId ->
                            navController.navigate(MainRoute.WardrobeItemDetail.createRoute(itemId))
                        },
                    )
                }

                composable(
                    route = "${MainRoute.Outfits.route}?$OccasionArg={$OccasionArg}",
                    arguments = listOf(
                        navArgument(OccasionArg) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val occasion = backStackEntry.savedStateHandle.get<String>(OccasionArg)
                    OutfitScreen(
                        initialOccasion = occasion,
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
                    LaunchedEffect(profileScrollState.value) {
                        bottomBarScrollState.updateScroll(profileScrollState.value.toFloat())
                    }

                    ProfileScreen(
                        user = currentUser,
                        scrollState = profileScrollState,
                        onHelpSupportClick = {
                            uriHandler.openUri("https://komod.app/support")
                        },
                        onAboutClick = {},
                        onSignOutConfirmed = {
                            snackbarScope.launch {
                                authRepository.signOut()
                            }
                        },
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
                        onRefreshWardrobe = {
                            requestRefresh(MainRoute.Wardrobe.route, WardrobeRefreshArg)
                        },
                        onRefreshHome = {
                            requestRefresh(MainRoute.Home.route, HomeRefreshArg)
                        },
                        onShowSnackbar = ::showSnackbar,
                    )
                }

                composable(MainRoute.OutfitDetails.route) { backStackEntry ->
                    backStackEntry.savedStateHandle.get<String>(SelectedOutfitKey)
                    OutfitDetailsPlaceholder()
                }
            }
        }
        
        // Floating bottom navigation bar
        if (showBottomBar) {
            BottomNavigationBar(
                items = getMainTabs(),
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
                showLabels = bottomBarScrollState.isExpanded > 0.5f,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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
