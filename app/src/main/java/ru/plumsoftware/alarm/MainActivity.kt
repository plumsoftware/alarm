package ru.plumsoftware.alarm

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Nullable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
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
            lightColor = Color.RED
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
            showOpenAds()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
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

        enableEdgeToEdge()
        setContent {
            AlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "alarm_list") {
                        composable("alarm_list") {
                            AlarmListScreen(navController = navController, context = this@MainActivity)
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
}