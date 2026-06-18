package com.example.goforit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.goforit.data.AuthRepository
import com.example.goforit.data.RestorationRepository
import com.example.goforit.data.RouteRepository
import com.example.goforit.navigation.BottomNavBar
import com.example.goforit.navigation.Screen
import com.example.goforit.ui.account.AccountScreen
import com.example.goforit.ui.collection.CollectionScreen
import com.example.goforit.ui.common.SilverSaltHelpButton
import com.example.goforit.ui.home.HomeScreen
import com.example.goforit.ui.login.LoginScreen
import com.example.goforit.ui.run.RunScreen
import com.example.goforit.ui.theme.GoForItTheme
import com.google.android.libraries.places.api.Places

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val placesApiKey = getString(R.string.places_api_key)
        if (!Places.isInitialized() && placesApiKey.isNotBlank()) {
            Places.initializeWithNewPlacesApiEnabled(
                applicationContext,
                placesApiKey
            )
        }
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
    var autoStartRunRequest by remember { mutableIntStateOf(0) }

    // Scaffold：Material3 的頁面框架，幫我們安排 bottomBar 的位置
    Box(modifier = Modifier.fillMaxSize()) {
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
                composable(Screen.Home.route) {
                    // 切到「去探索」分頁的共用導航（restoreState 保留分頁狀態）
                    val goToExplore = {
                        navController.navigate(Screen.Run.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    val goToAccount = {
                        navController.navigate(Screen.Account.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    HomeScreen(
                        // 首頁「立即探索」：切分頁並自動開始跑步
                        onStartExplore = {
                            autoStartRunRequest++
                            goToExplore()
                        },
                        // 古蹟詳情導航鈕：只切到去探索分頁，不自動開始跑步
                        onOpenExplore = goToExplore,
                        // 地圖「找不到你要的地點？」：切到帳號頁的回饋區
                        onOpenAccount = goToAccount
                    )
                }
                composable(Screen.Run.route) {
                    RunScreen(autoStartRequest = autoStartRunRequest)
                }
                composable(Screen.Records.route) { CollectionScreen(navController) }
                composable(Screen.Account.route) { AccountScreen() }
            }
        }

        SilverSaltHelpButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 10.dp, end = 12.dp)
        )
    }
}
