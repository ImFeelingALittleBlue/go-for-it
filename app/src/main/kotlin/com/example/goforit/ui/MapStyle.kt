package com.example.goforit.ui

import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.generated.BackgroundLayer
import com.mapbox.maps.extension.style.layers.generated.FillLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.getLayer

// 把 MAPBOX_STREETS 的預設灰藍色調改成設計稿的暖米色系
// getLayer() 取得現有圖層後直接改顏色；圖層不存在時 as? 回傳 null，?.呼叫直接略過
fun applyWarmMapStyle(style: Style) {
    (style.getLayer("background")       as? BackgroundLayer)?.backgroundColor("#F3EEE6") // 底色→暖米白
    (style.getLayer("landcover")        as? FillLayer)      ?.fillColor("#EFE9DE")       // 陸地覆蓋
    (style.getLayer("national-park")    as? FillLayer)      ?.fillColor("#E8E0D2")       // 公園綠地
    (style.getLayer("landuse")          as? FillLayer)      ?.fillColor("#F0EADF")       // 土地利用
    (style.getLayer("water")            as? FillLayer)      ?.fillColor("#B8D6DE")       // 水體→淡藍
    (style.getLayer("waterway")         as? LineLayer)      ?.lineColor("#B8D6DE")       // 河流/水道
    (style.getLayer("building")         as? FillLayer)      ?.fillColor("#D9D0C0")       // 建築物
    (style.getLayer("building-outline") as? LineLayer)      ?.lineColor("#BDB3A4")       // 建築輪廓

    listOf(
        "road-primary",
        "road-secondary-tertiary",
        "road-street",
        "road-minor",
        "road-path",
        "road-pedestrian"
    ).forEach { id ->
        (style.getLayer(id) as? LineLayer)?.lineColor("#5A544C")
    }
}
