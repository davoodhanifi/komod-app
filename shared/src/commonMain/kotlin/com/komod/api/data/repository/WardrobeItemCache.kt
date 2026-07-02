package com.komod.api.data.repository

import com.komod.api.domain.model.WardrobeItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WardrobeItemCache {
    private val mutex = Mutex()
    private val items = mutableMapOf<String, WardrobeItem>()
    private val inFlight = mutableMapOf<String, CompletableDeferred<WardrobeItem>>()

    suspend fun putAll(values: List<WardrobeItem>) {
        mutex.withLock {
            values.forEach { item ->
                items[item.id] = item
            }
        }
    }

    suspend fun get(id: String): WardrobeItem? {
        return mutex.withLock { items[id] }
    }

    suspend fun snapshot(): List<WardrobeItem> {
        return mutex.withLock { items.values.toList() }
    }

    suspend fun getOrLoad(
        id: String,
        loader: suspend () -> WardrobeItem,
    ): WardrobeItem {
        get(id)?.let { return it }

        val existingDeferred = mutex.withLock { inFlight[id] }
        if (existingDeferred != null) {
            return existingDeferred.await()
        }

        val deferred = CompletableDeferred<WardrobeItem>()
        mutex.withLock {
            items[id]?.let {
                deferred.complete(it)
                return@withLock
            }
            inFlight[id] = deferred
        }

        if (deferred.isCompleted) {
            return deferred.await()
        }

        return try {
            val loaded = loader()
            mutex.withLock {
                items[id] = loaded
                inFlight.remove(id)?.complete(loaded)
            }
            loaded
        } catch (throwable: Throwable) {
            mutex.withLock {
                inFlight.remove(id)?.completeExceptionally(throwable)
            }
            throw throwable
        }
    }
}
