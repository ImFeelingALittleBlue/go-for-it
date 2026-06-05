package com.example.goforit.data

import androidx.compose.runtime.mutableStateListOf
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

    // 收藏頁觀察這份清單；snapshotListener 一有變動就更新它，畫面自動重畫
    private val records = mutableStateListOf<RestorationRecord>()
    private var started = false   // 避免重複啟動監聽

    fun records(): SnapshotStateList<RestorationRecord> = records

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
                records.clear()
                for (doc in snapshot.documents) {
                    // 把 Firestore 文件轉回 RestorationRecord，並補上文件 id
                    doc.toObject(RestorationRecord::class.java)
                        ?.copy(docId = doc.id)
                        ?.let { records.add(it) }
                }
            }
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
    fun delete(docId: String) {
        collection()?.document(docId)?.delete()
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
