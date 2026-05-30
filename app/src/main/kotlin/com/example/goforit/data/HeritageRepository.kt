package com.example.goforit.data

import android.content.Context

// 負責「從 assets/metadata.csv 讀出古蹟資料」的地方
// object：Kotlin 的單例（整個 App 只有一份，直接用 HeritageRepository.xxx 呼叫）
//
// metadata.csv 欄位順序（0 起算）：
// 0 id, 1 picname, 2 latitude, 3 longitude, 4 heading, 5 pitch, 6 zoom,
// 7 title, 8 date, 9 rights, 10 creator, 11 description, 12 other,
// 13 xml_resource, 14 photo_file, ...
object HeritageRepository {

    // 讀取並解析整份 CSV，回傳古蹟列表
    fun loadHeritages(context: Context): List<Heritage> {
        val result = mutableListOf<Heritage>()

        // 打開 assets 裡的 CSV，用 bufferedReader 一行一行讀
        context.assets.open("metadata.csv").bufferedReader().useLines { lines ->
            lines.drop(1).forEach { line ->          // drop(1)：跳過第一行標題
                val cols = parseCsvLine(line)
                if (cols.size < 15) return@forEach    // 欄位不足，跳過這行

                // 緯度、經度轉成數字，轉失敗（空白）就跳過這筆
                val lat = cols[2].toDoubleOrNull()
                val lng = cols[3].toDoubleOrNull()
                if (lat == null || lng == null) return@forEach

                result.add(
                    Heritage(
                        id = cols[0].toIntOrNull() ?: 0,
                        name = cols[7],
                        year = cols[8],
                        description = cols[11],
                        lat = lat,
                        lng = lng,
                        // photo_file 是「tainan_old_photos/photos/001_xxx.png」這種路徑，
                        // 只取最後的檔名（substringAfterLast('/')）
                        photoFile = cols[14].substringAfterLast('/')
                    )
                )
            }
        }
        return result
    }

    // 把一行 CSV 拆成欄位
    // 重點：描述欄位裡可能有逗號，但被「雙引號」包住，那些逗號不能當分隔
    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false                    // 目前是否在雙引號內

        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes               // 遇到引號：切換內外狀態
                c == ',' && !inQuotes -> {                     // 只有「引號外的逗號」才是真正的分隔
                    fields.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(c)
            }
        }
        fields.add(sb.toString())               // 別忘了最後一欄
        return fields
    }
}
