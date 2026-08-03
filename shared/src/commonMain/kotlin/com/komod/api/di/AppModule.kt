package com.komod.api.di

import com.komod.api.BuildKonfig
import com.komod.api.data.api.WardrobeApiService
import com.komod.api.data.api.OutfitApiService
import com.komod.api.data.api.WeatherApi
import com.komod.api.data.api.provideKtorClient
import com.komod.api.data.auth.SupabaseAuthDataSource
import com.komod.api.data.repository.AddItemRepository
import com.komod.api.data.repository.AddItemRepositoryImpl
import com.komod.api.data.repository.AuthRepository
import com.komod.api.data.repository.AuthRepositoryImpl
import com.komod.api.data.repository.HomeRepository
import com.komod.api.data.repository.HomeRepositoryImpl
import com.komod.api.data.repository.OutfitRepository
import com.komod.api.data.repository.OutfitRepositoryImpl
import com.komod.api.data.repository.WeatherRepository
import com.komod.api.data.repository.WeatherRepositoryImpl
import com.komod.api.data.repository.UploadedImageStore
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
import com.komod.api.presentation.home.HomeViewModel
import com.komod.api.presentation.outfits.OutfitViewModel
import com.komod.api.presentation.wardrobe.WardrobeItemEditViewModel
import com.komod.api.presentation.wardrobe.WardrobeItemDetailViewModel
import com.komod.api.presentation.wardrobe.WardrobeViewModel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

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
    single { WardrobeApiService(get()) }
    single { OutfitApiService(get()) }
    single { WeatherApi(get()) }
    single { WardrobeItemCache() }
    single { UploadedImageStore() }
    single { WeatherPreferences() }
    single { WeatherLocationService() }
    single { com.komod.api.platform.AppSettingsOpener() }
    single { StorageService(supabaseClient = get()) }
    single<AddItemRepository> { AddItemRepositoryImpl(get(), get(), get()) }
    single<WardrobeRepository> { WardrobeRepositoryImpl(wardrobeApiService = get(), supabaseClient = get(), wardrobeItemCache = get()) }
    single<WardrobeItemRepository> { WardrobeItemRepositoryImpl(wardrobeApiService = get(), supabaseClient = get(), wardrobeItemCache = get()) }
    single<WeatherRepository> { WeatherRepositoryImpl(weatherApi = get()) }
    single<OutfitRepository> { OutfitRepositoryImpl(outfitApiService = get(), supabaseClient = get(), wardrobeItemRepository = get(), wardrobeItemCache = get()) }
    single<HomeRepository> { HomeRepositoryImpl(wardrobeApiService = get(), supabaseClient = get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { AddItemViewModel(get()) }
    viewModel { OutfitViewModel(get(), get(), get(), get(), get()) }
    viewModel { WardrobeViewModel(get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { params -> WardrobeItemDetailViewModel(params.get(), get()) }
    viewModel { params -> WardrobeItemEditViewModel(params.get(), get()) }
}
