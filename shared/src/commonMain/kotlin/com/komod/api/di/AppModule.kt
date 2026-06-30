package com.komod.api.di

import com.komod.api.AppConfig
import com.komod.api.data.api.WardrobeApiService
import com.komod.api.data.api.provideKtorClient
import com.komod.api.data.auth.SupabaseAuthDataSource
import com.komod.api.data.repository.AddItemRepository
import com.komod.api.data.repository.AddItemRepositoryImpl
import com.komod.api.data.repository.AuthRepository
import com.komod.api.data.repository.AuthRepositoryImpl
import com.komod.api.data.upload.UploadService
import com.komod.api.presentation.additem.AddItemViewModel
import com.komod.api.presentation.auth.LoginViewModel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
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
        }
    }
    single { SupabaseAuthDataSource(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single { provideKtorClient(get()) }
    single { WardrobeApiService(get()) }
    single { UploadService(supabaseUrl = config.supabaseUrl, authDataSource = get()) }
    single<AddItemRepository> { AddItemRepositoryImpl(get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { AddItemViewModel(get()) }
}
