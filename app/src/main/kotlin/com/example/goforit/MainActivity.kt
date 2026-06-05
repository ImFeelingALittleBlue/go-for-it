package com.example.goforit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.goforit.data.AuthRepository
import com.example.goforit.data.MapBuildRepository
import com.example.goforit.data.RestorationRepository
import com.example.goforit.data.RouteRepository
import com.example.goforit.navigation.BottomNavBar
import com.example.goforit.navigation.Screen
import com.example.goforit.ui.account.AccountScreen
import com.example.goforit.ui.collection.CollectionScreen
import com.example.goforit.ui.home.HomeScreen
import com.example.goforit.ui.login.LoginScreen
import com.example.goforit.ui.run.RunScreen
import com.example.goforit.ui.theme.GoForItTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GoForItTheme {
                // 觀察登入狀態：null 才是未登入；匿名帳號也算已登入（可訪客瀏覽）
                val user = AuthRepository.user
                val loggedIn = user != null

                if (!loggedIn) {
                    // 還沒登入 → 顯示登入頁
                    LoginScreen()
                } else {
                    // 已登入 → 啟動雲端監聽（用目前帳號的 uid），再進主畫面
                    LaunchedEffect(user!!.uid) {
                        RestorationRepository.start()
                        RouteRepository.start()
                        MapBuildRepository.start()
                    }
                    MainApp()
                }
            }
        }
    }
}

// 主畫面：底部導航列 + 四個分頁
@androidx.compose.runtime.Composable
private fun MainApp() {
    // NavController：整個 App 的導航管理員，記住現在在哪一頁
    val navController = rememberNavController()

    // Scaffold：Material3 的頁面框架，幫我們安排 bottomBar 的位置
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { BottomNavBar(navController = navController) }
    ) { innerPadding ->
        // NavHost：根據目前 route 決定顯示哪個畫面
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route)    { HomeScreen() }
            composable(Screen.Run.route)     { RunScreen() }
            composable(Screen.Records.route) { CollectionScreen() }
            composable(Screen.Account.route) { AccountScreen() }
        }
    }
}
