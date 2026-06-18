package com.example.komod

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform