package com.example.goforit.data

import android.content.Context
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf

// 「時光銀鹽」點數的存放與讀取
// 用 SharedPreferences 存一個整數：App 關掉再打開，點數還在
// （SharedPreferences = Android 內建的小型資料儲存，適合分數、設定這種簡單資料）
object SilverSaltStore {
    private const val PREF_NAME = "silver_salt"   // 儲存檔名
    private const val KEY_POINTS = "points"       // 資料的鍵

    // 把點數放在「可被 Compose 觀察的狀態」裡，整個 App 共用同一份。
    // 這樣不管哪個畫面改了點數，其他畫面都會自動更新。
    private var pointsState: MutableIntState? = null

    // 取得點數狀態（第一次呼叫會從 SharedPreferences 讀進來）
    fun points(context: Context): MutableIntState {
        if (pointsState == null) {
            val saved = prefs(context).getInt(KEY_POINTS, 0)   // 沒存過就預設 0
            pointsState = mutableIntStateOf(saved)
        }
        return pointsState!!
    }

    // 增加點數（跑步、答對題目、探索古蹟時呼叫）
    fun add(context: Context, amount: Int) {
        val state = points(context)
        state.intValue += amount
        save(context, state.intValue)
    }

    // 花費點數：點數夠 → 扣掉並回傳 true；不夠 → 不動作回傳 false
    fun spend(context: Context, amount: Int): Boolean {
        val state = points(context)
        if (state.intValue < amount) return false
        state.intValue -= amount
        save(context, state.intValue)
        return true
    }

    // 真正把數字寫進 SharedPreferences（apply 是非同步寫入，不卡畫面）
    private fun save(context: Context, value: Int) {
        prefs(context).edit().putInt(KEY_POINTS, value).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
