package ru.plumsoftware.alarm.data

import ru.plumsoftware.alarm.BuildConfig

sealed class AdsConfig(
    open val OPEN_MAIN_SCREEN_AD: String,
    open val INTERSTITIAL_ADS: String
) {
    data class RuStore(override val OPEN_MAIN_SCREEN_AD: String = if (BuildConfig.DEBUG) "demo-appopenad-yandex" else "R-M-17270777-1",
                       override val INTERSTITIAL_ADS: String = if (BuildConfig.DEBUG) "demo-interstitial-yandex" else "R-M-17270777-2") :
        AdsConfig(OPEN_MAIN_SCREEN_AD = OPEN_MAIN_SCREEN_AD, INTERSTITIAL_ADS = INTERSTITIAL_ADS)

    data class HuaweiAppGallery(override val OPEN_MAIN_SCREEN_AD: String = if (BuildConfig.DEBUG) "demo-appopenad-yandex" else "R-M-17758620-1") :
        AdsConfig(OPEN_MAIN_SCREEN_AD = OPEN_MAIN_SCREEN_AD, INTERSTITIAL_ADS = "")
}