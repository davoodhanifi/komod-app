package com.komod.api.di

import com.komod.api.BuildKonfig
import com.komod.api.core.navigation.PlanLimitNavigator
import com.komod.api.core.navigation.PlanLimitNavigatorImpl
import com.komod.api.data.billing.PurchasesService
import com.komod.api.data.billing.RevenueCatIdentitySync
import com.komod.api.data.billing.RevenueCatPurchasesService
import com.komod.api.data.api.WardrobeApiService
import com.komod.api.data.api.OutfitApiService
import com.komod.api.data.api.SubscriptionApiService
import com.komod.api.data.api.WeatherApi
import com.komod.api.data.api.provideKtorClient
import com.komod.api.data.api.providePlainHttpClient
import com.komod.api.data.auth.SupabaseAuthDataSource
import com.komod.api.data.repository.AddItemRepository
import com.komod.api.data.repository.AddItemRepositoryImpl
import com.komod.api.data.repository.AuthRepository
import com.komod.api.data.repository.AuthRepositoryImpl
import com.komod.api.data.repository.BillingRepository
import com.komod.api.data.repository.BillingRepositoryImpl
import com.komod.api.data.repository.HomeRepository
import com.komod.api.data.repository.HomeRepositoryImpl
import com.komod.api.data.repository.OutfitRepository
import com.komod.api.data.repository.OutfitRepositoryImpl
import com.komod.api.data.repository.SubscriptionRepository
import com.komod.api.data.repository.SubscriptionRepositoryImpl
import com.komod.api.data.repository.SubscriptionSyncRepository
import com.komod.api.data.repository.SubscriptionSyncRepositoryImpl
import com.komod.api.data.repository.WeatherRepository
import com.komod.api.data.repository.WeatherRepositoryImpl
import com.komod.api.data.repository.UploadedImageStore
import com.komod.api.data.repository.UploadReviewRepository
import com.komod.api.data.repository.UploadReviewRepositoryImpl
import com.komod.api.data.repository.WardrobeItemCache
import com.komod.api.data.repository.WardrobeItemRepository
import com.komod.api.data.repository.WardrobeItemRepositoryImpl
import com.komod.api.data.repository.WardrobeRepository
import com.komod.api.data.repository.WardrobeRepositoryImpl
import com.komod.api.data.location.WeatherLocationService
import com.komod.api.data.preferences.WeatherPreferences
import com.komod.api.data.storage.StorageService
import com.komod.api.presentation.additem.AddItemViewModel
import com.komod.api.presentation.auth.LoginViewModel
import com.komod.api.presentation.cropeditor.CropEditorViewModel
import com.komod.api.presentation.home.HomeViewModel
import com.komod.api.presentation.outfits.OutfitViewModel
import com.komod.api.presentation.paywall.PaywallViewModel
import com.komod.api.presentation.profile.ProfileViewModel
import com.komod.api.presentation.uploadreview.UploadReviewViewModel
import com.komod.api.presentation.wardrobe.WardrobeItemEditViewModel
import com.komod.api.presentation.wardrobe.WardrobeItemDetailViewModel
import com.komod.api.presentation.wardrobe.WardrobeViewModel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val PlainHttpClientQualifier = named("plainHttpClient")

fun appModule() = module {
    single {
        createSupabaseClient(
            supabaseUrl = BuildKonfig.SUPABASE_URL,
            supabaseKey = BuildKonfig.SUPABASE_PUBLISHABLE_KEY,
        ) {
            install(Auth) {
                scheme = "komod"
                host = "login-callback"
            }
            install(Storage)
        }
    }
    single { SupabaseAuthDataSource(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single { provideKtorClient(get()) }
    single(PlainHttpClientQualifier) { providePlainHttpClient() }
    single { WardrobeApiService(get()) }
    single { OutfitApiService(get()) }
    single { SubscriptionApiService(get()) }
    single { WeatherApi(get()) }
    single { WardrobeItemCache() }
    single { UploadedImageStore() }
    single { WeatherPreferences() }
    single { WeatherLocationService() }
    single { com.komod.api.platform.AppSettingsOpener() }
    single<PlanLimitNavigator> { PlanLimitNavigatorImpl() }
    single { StorageService(supabaseClient = get()) }
    single<PurchasesService> { RevenueCatPurchasesService() }
    // Also resolved explicitly and eagerly from iOS's initKoin() right after
    // RevenueCatPurchasesService.configure() — see MainViewController.kt — so identity syncing
    // starts at app launch rather than waiting for BillingRepository's first use (Paywall).
    single { RevenueCatIdentitySync(authRepository = get(), purchasesService = get()) }
    single<BillingRepository> { BillingRepositoryImpl(purchasesService = get(), revenueCatIdentitySync = get()) }
    single<SubscriptionSyncRepository> {
        SubscriptionSyncRepositoryImpl(subscriptionApiService = get(), revenueCatIdentitySync = get())
    }
    single<AddItemRepository> { AddItemRepositoryImpl(get(), get(), get()) }
    single<WardrobeRepository> { WardrobeRepositoryImpl(wardrobeApiService = get(), supabaseClient = get(), wardrobeItemCache = get()) }
    single<WardrobeItemRepository> {
        WardrobeItemRepositoryImpl(
            wardrobeApiService = get(),
            supabaseClient = get(),
            wardrobeItemCache = get(),
            plainHttpClient = get(PlainHttpClientQualifier),
        )
    }
    single<WeatherRepository> { WeatherRepositoryImpl(weatherApi = get()) }
    single<OutfitRepository> { OutfitRepositoryImpl(outfitApiService = get(), supabaseClient = get(), wardrobeItemRepository = get(), wardrobeItemCache = get()) }
    single<HomeRepository> {
        HomeRepositoryImpl(
            wardrobeApiService = get(),
            outfitApiService = get(),
            supabaseClient = get(),
            wardrobeItemCache = get(),
        )
    }
    single<UploadReviewRepository> { UploadReviewRepositoryImpl(wardrobeApiService = get(), storageService = get()) }
    single<SubscriptionRepository> { SubscriptionRepositoryImpl(subscriptionApiService = get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { AddItemViewModel(get(), get()) }
    viewModel { OutfitViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { WardrobeViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { ProfileViewModel(get()) }
    viewModel { PaywallViewModel(get(), get()) }
    viewModel { params -> WardrobeItemDetailViewModel(params.get(), get()) }
    viewModel { params -> WardrobeItemEditViewModel(params.get(), get()) }
    viewModel { params -> UploadReviewViewModel(params.get(), get(), get(), get(), get()) }
    viewModel { params -> CropEditorViewModel(params.get(), params.get(), get(), get()) }
}
