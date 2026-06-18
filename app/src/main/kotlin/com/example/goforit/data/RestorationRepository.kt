package com.example.goforit.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// 修復紀錄的雲端存取中心（Firebase）
// - 匿名登入：每支手機自動拿到一個隱形帳號，紀錄存在自己帳號底下
// - Firestore：即時監聽，新增/刪除都會自動反映到收藏頁
//
// 資料路徑：users/{匿名uid}/restorations/{自動id}
object RestorationRepository {
    private const val DEBUG_PREFS = "restoration_debug_prefs"
    private const val LEGACY_KEY_DEBUG_RESTORED_SEED = "debug_restored_seed_enabled"
    private const val KEY_DEBUG_SILVER_SALT_READY = "debug_silver_salt_ready_enabled"

    private val debugSilverSaltReadyIds = setOf(1, 2, 3, 14, 66)

    var debugSilverSaltReadyEnabled by mutableStateOf(false)
        private set

    // 收藏頁觀察這份清單；snapshotListener 一有變動就更新它，畫面自動重畫
    private val records = mutableStateListOf<RestorationRecord>()
    private val cloudRecords = mutableListOf<RestorationRecord>()
    private var started = false   // 避免重複啟動監聽

    fun records(): SnapshotStateList<RestorationRecord> = records

    fun initDebugSeed(context: Context) {
        val prefs = context.applicationContext
            .getSharedPreferences(DEBUG_PREFS, Context.MODE_PRIVATE)
        debugSilverSaltReadyEnabled = prefs.getBoolean(KEY_DEBUG_SILVER_SALT_READY, false) ||
            prefs.getBoolean(LEGACY_KEY_DEBUG_RESTORED_SEED, false)
        rebuildRecords()
    }

    fun setDebugSeedEnabled(context: Context, enabled: Boolean) {
        context.applicationContext
            .getSharedPreferences(DEBUG_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DEBUG_SILVER_SALT_READY, enabled)
            .putBoolean(LEGACY_KEY_DEBUG_RESTORED_SEED, false)
            .apply()

        debugSilverSaltReadyEnabled = enabled
        rebuildRecords()
    }

    fun isDebugSilverSaltReady(heritageId: Int): Boolean =
        debugSilverSaltReadyEnabled && heritageId in debugSilverSaltReadyIds

    // App 啟動時呼叫：先確保匿名登入，再開始監聽自己的紀錄
    fun start() {
        if (started) return
        started = true
        val auth = Firebase.auth
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnSuccessListener { listen() }
        } else {
            listen()
        }
    }

    // 監聽自己帳號底下的修復紀錄（依完成時間新到舊排序）
    private fun listen() {
        collection()?.orderBy("restoredAt", Query.Direction.DESCENDING)
            ?.addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                cloudRecords.clear()
                for (doc in snapshot.documents) {
                    // 把 Firestore 文件轉回 RestorationRecord，並補上文件 id
                    doc.toObject(RestorationRecord::class.java)
                        ?.copy(docId = doc.id)
                        ?.let { cloudRecords.add(it) }
                }
                rebuildRecords()
            }
    }

    private fun rebuildRecords() {
        records.clear()
        records.addAll(cloudRecords)
    }

    // 修復古蹟成功時呼叫：寫一筆紀錄到雲端
    fun add(heritage: Heritage) {
        val data = hashMapOf(
            "heritageId" to heritage.id,
            "name" to heritage.name,
            "description" to heritage.description,
            "photoFile" to heritage.photoFile,
            "restoredAt" to System.currentTimeMillis()
        )
        collection()?.add(data)
    }

    // 刪除某一筆紀錄
    fun delete(record: RestorationRecord) {
        record.docId
            .takeIf(String::isNotBlank)
            ?.let { docId ->
                cloudRecords.removeAll { it.docId == docId }
                collection()?.document(docId)?.delete()
                rebuildRecords()
            }
    }

    // 某個古蹟是否已修復（清單裡有就是已修復）
    fun isRestored(heritageId: Int): Boolean =
        records.any { it.heritageId == heritageId }

    // 取得「目前帳號的 restorations 集合」；還沒登入就回 null
    private fun collection(): CollectionReference? {
        val uid = Firebase.auth.currentUser?.uid ?: return null
        return Firebase.firestore
            .collection("users").document(uid)
            .collection("restorations")
    }
}
