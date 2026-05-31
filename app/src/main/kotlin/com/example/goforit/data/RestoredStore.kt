package com.example.goforit.data

import android.content.Context
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf

// 記住「已經修復（解鎖老照片）的古蹟 id」
// 一樣用 SharedPreferences 存，App 關掉再開仍然記得修復過哪些
object RestoredStore {
    private const val PREF_NAME = "restored_heritage"
    private const val KEY_IDS = "ids"

    // 已修復的古蹟 id 清單，做成可被 Compose 觀察的共用狀態
    private var idsState: SnapshotStateList<Int>? = null

    // 取得清單（第一次會從 SharedPreferences 讀進來）
    fun restored(context: Context): SnapshotStateList<Int> {
        if (idsState == null) {
            // SharedPreferences 只能存字串集合，所以存的是 id 的字串，讀回來轉成數字
            val saved = prefs(context).getStringSet(KEY_IDS, emptySet()) ?: emptySet()
            idsState = mutableStateListOf<Int>().apply {
                addAll(saved.mapNotNull { it.toIntOrNull() })
            }
        }
        return idsState!!
    }

    // 把某個古蹟標記為已修復
    fun restore(context: Context, id: Int) {
        val list = restored(context)
        if (!list.contains(id)) {
            list.add(id)
            // 存回去（轉成字串集合）
            prefs(context).edit()
                .putStringSet(KEY_IDS, list.map { it.toString() }.toSet())
                .apply()
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
