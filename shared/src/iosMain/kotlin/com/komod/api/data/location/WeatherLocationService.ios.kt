package com.komod.api.data.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

actual class WeatherLocationService {
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun getCurrentLocation(): WeatherLocationResult {
        return suspendCancellableCoroutine { continuation ->
            val manager = CLLocationManager()

            fun finish(result: WeatherLocationResult) {
                manager.delegate = null
                if (continuation.isActive) continuation.resume(result)
            }

            val delegate = WeatherLocationDelegate(
                onLocation = { location ->
                    location.coordinate.useContents {
                        finish(WeatherLocationResult.Success(latitude = latitude, longitude = longitude))
                    }
                },
                onError = { error ->
                    if (error.code.toInt() == 1) {
                        finish(WeatherLocationResult.PermissionRequired)
                    } else {
                        finish(WeatherLocationResult.Error(error.localizedDescription))
                    }
                },
                onAuthorized = {
                    manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
                    manager.requestLocation()
                },
                onDenied = {
                    finish(WeatherLocationResult.PermissionRequired)
                },
            )

            manager.delegate = delegate

            when (manager.authorizationStatus) {
                kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse -> {
                    manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
                    manager.requestLocation()
                }
                kCLAuthorizationStatusNotDetermined -> {
                    manager.requestWhenInUseAuthorization()
                }
                kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> {
                    finish(WeatherLocationResult.PermissionRequired)
                }
                else -> {
                    finish(WeatherLocationResult.Error("Unable to determine location permission."))
                }
            }

            continuation.invokeOnCancellation {
                manager.delegate = null
            }
        }
    }
}

private class WeatherLocationDelegate(
    private val onLocation: (CLLocation) -> Unit,
    private val onError: (NSError) -> Unit,
    private val onAuthorized: () -> Unit,
    private val onDenied: () -> Unit,
) : NSObject(), CLLocationManagerDelegateProtocol {
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        onLocation(location)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        onError(didFailWithError)
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse -> onAuthorized()
            kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted -> onDenied()
            else -> Unit
        }
    }
}
