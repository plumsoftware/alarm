package ru.plumsoftware.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.plumsoftware.alarm.ui.screen.AlarmListScreen
import ru.plumsoftware.alarm.ui.screen.EditAlarmScreen
import ru.plumsoftware.alarm.ui.theme.AlarmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        composable("edit_alarm/{alarmId}") { backStackEntry ->
                            val alarmId = backStackEntry.arguments?.getString("alarmId")?.toInt() ?: -1
                            EditAlarmScreen(navController = navController, alarmId = alarmId, context = this@MainActivity)
                        }
                        composable("new_alarm") {
                            EditAlarmScreen(navController = navController, alarmId = -1, context = this@MainActivity)
                        }
                    }
                }
            }
        }
    }
}