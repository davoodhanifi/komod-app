package com.komod.api.di

import com.komod.api.AppConfig
import com.komod.api.data.api.WardrobeApiService
import com.komod.api.data.api.provideKtorClient
import com.komod.api.data.auth.SupabaseAuthDataSource
import com.komod.api.data.repository.AddItemRepository
import com.komod.api.data.repository.AddItemRepositoryImpl
import com.komod.api.data.repository.AuthRepository
import com.komod.api.data.repository.AuthRepositoryImpl
import com.komod.api.data.repository.WardrobeItemRepository
import com.komod.api.data.repository.WardrobeItemRepositoryImpl
import com.komod.api.data.repository.WardrobeRepository
import com.komod.api.data.repository.WardrobeRepositoryImpl
import com.komod.api.data.storage.StorageService
import com.komod.api.presentation.additem.AddItemViewModel
import com.komod.api.presentation.auth.LoginViewModel
import com.komod.api.presentation.wardrobe.WardrobeItemDetailViewModel
import com.komod.api.presentation.wardrobe.WardrobeViewModel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModule(config: AppConfig) = module {
    single {
        createSupabaseClient(
            supabaseUrl = config.supabaseUrl,
            supabaseKey = config.supabasePublishableKey,
        ) {
            install(Auth) {
                scheme = "komod"
                host = "login-callback"
                // sessionManager defaults to SettingsSessionManager — sessions are persisted automatically
            }
            install(Storage)
        }
    }
    single { SupabaseAuthDataSource(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single { provideKtorClient(get()) }
    single { WardrobeApiService(get()) }
    single { StorageService(supabaseClient = get()) }
    single<AddItemRepository> { AddItemRepositoryImpl(get(), get()) }
    single<WardrobeRepository> { WardrobeRepositoryImpl(wardrobeApiService = get(), supabaseClient = get()) }
    single<WardrobeItemRepository> { WardrobeItemRepositoryImpl(wardrobeApiService = get(), supabaseClient = get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { AddItemViewModel(get()) }
    viewModel { WardrobeViewModel(get()) }
    viewModel { params -> WardrobeItemDetailViewModel(params.get(), get()) }
}
