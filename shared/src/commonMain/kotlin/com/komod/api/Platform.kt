package com.komod.api

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform