package ru.plumsoftware.alarm.data

import ru.plumsoftware.alarm.BuildConfig

sealed class AdsConfig(
    open val OPEN_MAIN_SCREEN_AD: String
) {
    data class RuStore(override val OPEN_MAIN_SCREEN_AD: String = if (BuildConfig.DEBUG) "demo-appopenad-yandex" else "R-M-17270777-1") :
        AdsConfig(OPEN_MAIN_SCREEN_AD = OPEN_MAIN_SCREEN_AD)

    data class HuaweiAppGallery(override val OPEN_MAIN_SCREEN_AD: String = if (BuildConfig.DEBUG) "demo-appopenad-yandex" else "R-M-17758620-1") :
        AdsConfig(OPEN_MAIN_SCREEN_AD = OPEN_MAIN_SCREEN_AD)
}