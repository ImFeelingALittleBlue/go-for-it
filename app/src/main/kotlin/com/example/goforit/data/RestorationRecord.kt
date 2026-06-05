package com.example.goforit.data

// 一筆「修復古蹟」的紀錄（存在 Firestore，也用來顯示在收藏頁）
// 所有欄位都有預設值：Firestore 把文件轉回物件時需要一個無參數建構子
data class RestorationRecord(
    val docId: String = "",        // Firestore 文件 id（刪除這筆時用）
    val heritageId: Int = 0,       // 對應的古蹟 id
    val name: String = "",         // 古蹟名稱
    val description: String = "",  // 描述
    val photoFile: String = "",    // 老照片檔名
    val restoredAt: Long = 0L      // 完成修復的時間（毫秒）
)
