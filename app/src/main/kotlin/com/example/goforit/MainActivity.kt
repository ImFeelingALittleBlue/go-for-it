package com.example.goforit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.goforit.navigation.BottomNavBar
import com.example.goforit.navigation.Screen
import com.example.goforit.ui.collection.CollectionScreen
import com.example.goforit.ui.home.HomeScreen
import com.example.goforit.ui.map.MapScreen
import com.example.goforit.ui.run.RunScreen
import com.example.goforit.ui.theme.GoForItTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoForItTheme {
                // NavController：整個 App 的導航管理員，記住現在在哪一頁
                val navController = rememberNavController()

                // Scaffold：Material3 的頁面框架，幫我們安排 bottomBar 的位置
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // 把 navController 傳給底部導航列，點擊才能切換頁面
                        BottomNavBar(navController = navController)
                    }
                ) { innerPadding ->
                    // NavHost：根據目前 route 決定顯示哪個畫面
                    // startDestination：App 啟動時預設顯示首頁
                    NavHost(
                        navController    = navController,
                        startDestination = Screen.Home.route,
                        modifier         = Modifier.padding(innerPadding)
                    ) {
                        // 每個 composable 對應一個頁面
                        composable(Screen.Home.route)       { HomeScreen() }
                        composable(Screen.Run.route)        { RunScreen() }
                        composable(Screen.Map.route)        { MapScreen() }
                        composable(Screen.Collection.route) { CollectionScreen() }
                    }
                }
            }
        }
    }
}
