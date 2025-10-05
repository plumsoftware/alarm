package ru.plumsoftware.alarm

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Nullable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yandex.mobile.ads.appopenad.AppOpenAd
import com.yandex.mobile.ads.appopenad.AppOpenAdEventListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoadListener
import com.yandex.mobile.ads.appopenad.AppOpenAdLoader
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.common.MobileAds.initialize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.plumsoftware.alarm.data.AdsConfig
import ru.plumsoftware.alarm.ui.screen.AlarmListScreen
import ru.plumsoftware.alarm.ui.theme.AlarmTheme


class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            createAlarmNotificationChannel()
        } else {
            showPermissionRationaleDialog()
        }
    }

    private fun createAlarmNotificationChannel() {
        val channel = NotificationChannel(
            "alarm_channel",
            "Alarm Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for alarm triggers"
            enableLights(true)
            lightColor = android.graphics.Color.RED
            enableVibration(true)
            vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000)
        }

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Разрешение на уведомления необходимо")
            .setMessage("Чтобы будильник звонил, приложению нужно разрешение на отправку уведомлений. Пожалуйста, дайте разрешение в настройках.")
            .setPositiveButton("Настройки") { dialog, _ ->
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = "package:$packageName".toUri()
                }
                startActivity(intent)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initialize(this) {
            CoroutineScope(Dispatchers.IO).launch {
                val sp = getSharedPreferences("alarm_settings", MODE_PRIVATE)
                val count = sp.getInt("count_of_launches", 0)

                if (count <= 2) {
                    sp.edit {
                        putInt("count_of_launches", (count + 1))
                    }
                    return@launch
                } else {
                    withContext(Dispatchers.Main) {
                        showOpenAds()
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Запрашиваем разрешение
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                createAlarmNotificationChannel() // Уже есть разрешение — создаём канал
            }
        } else {
            // На Android < 13 — канал создаём без запроса
            createAlarmNotificationChannel()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setupEdgeToEdge()

        setContent {
            AlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "alarm_list") {
                        composable("alarm_list") {
                            setupEdgeToEdge()
                            AlarmListScreen(
                                navController = navController,
                                context = this@MainActivity
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showOpenAds() {
        var mAppOpenAd: AppOpenAd?
        val appOpenAdLoader = AppOpenAdLoader(this)
        val AD_UNIT_ID: String = AdsConfig.OPEN_MAIN_SCREEN_AD
        val adRequestConfiguration = AdRequestConfiguration.Builder(AD_UNIT_ID).build()

        val appOpenAdEventListener: AppOpenAdEventListener = object : AppOpenAdEventListener {
            override fun onAdShown() {}

            override fun onAdDismissed() {}

            override fun onAdFailedToShow(adError: AdError) {}

            override fun onAdClicked() {}

            override fun onAdImpression(@Nullable impressionData: ImpressionData?) {}
        }

        val appOpenAdLoadListener: AppOpenAdLoadListener = object : AppOpenAdLoadListener {

            override fun onAdFailedToLoad(error: AdRequestError) {

            }

            override fun onAdLoaded(appOpenAd: AppOpenAd) {
                mAppOpenAd = appOpenAd
                mAppOpenAd.setAdEventListener(appOpenAdEventListener)
                mAppOpenAd.show(this@MainActivity)
            }
        }

        appOpenAdLoader.setAdLoadListener(appOpenAdLoadListener)
        appOpenAdLoader.loadAd(adRequestConfiguration)
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val window = window

        // Определяем текущую тему (светлая/темная)
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDarkTheme = true

        var systemUiVisibilityFlags = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )

        // Делаем статус бар и нав бар прозрачными
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Настройка цвета иконок для Android 5–10
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isDarkTheme) {
                    // СВЕТЛАЯ ТЕМА — ТЁМНЫЕ ИКОНКИ
                    systemUiVisibilityFlags = systemUiVisibilityFlags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
                // Для тёмной темы оставляем светлые иконки (по умолчанию)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!isDarkTheme) {
                    // СВЕТЛАЯ ТЕМА — ТЁМНЫЕ ИКОНКИ НАВИГАЦИИ
                    systemUiVisibilityFlags = systemUiVisibilityFlags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                }
                // Для тёмной темы оставляем светлые иконки (по умолчанию)
            }

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = systemUiVisibilityFlags
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }

        // Для Android 10+ убираем затемнение под нав баром
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            window.isNavigationBarContrastEnforced = false
        }

        // Для Android 11+ используем новый API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)

            val controller = window.insetsController
            controller?.let {
                // Убеждаемся, что нав бар остаётся видимым
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                // Настройка цвета иконок для Android 11+
                if (!isDarkTheme) {
                    // СВЕТЛАЯ ТЕМА — ТЁМНЫЕ ИКОНКИ
                    it.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                } else {
                    // ТЁМНАЯ ТЕМА — СВЕТЛЫЕ ИКОНКИ (убираем флаги светлых иконок)
                    it.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                    )
                }
            }
        }
    }
}