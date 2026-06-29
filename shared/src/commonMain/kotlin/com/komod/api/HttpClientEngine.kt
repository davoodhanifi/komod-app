package com.komod.api

import io.ktor.client.engine.HttpClientEngine

expect fun httpClientEngine(): HttpClientEngine
