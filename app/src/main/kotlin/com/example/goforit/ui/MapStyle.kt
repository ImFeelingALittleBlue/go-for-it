package com.example.goforit.ui

import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.generated.BackgroundLayer
import com.mapbox.maps.extension.style.layers.generated.FillLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.getLayer

// 把 MAPBOX_STREETS 的預設灰藍色調改成設計稿的暖米色系
// getLayer() 取得現有圖層後直接改顏色；圖層不存在時 as? 回傳 null，?.呼叫直接略過
fun applyWarmMapStyle(style: Style) {
    (style.getLayer("background")       as? BackgroundLayer)?.backgroundColor("#F5EDD8") // 底色→暖米白
    (style.getLayer("landcover")        as? FillLayer)      ?.fillColor("#EEE5CC")       // 陸地覆蓋
    (style.getLayer("national-park")    as? FillLayer)      ?.fillColor("#E4D9BE")       // 公園綠地
    (style.getLayer("landuse")          as? FillLayer)      ?.fillColor("#EDE3CC")       // 土地利用
    (style.getLayer("water")            as? FillLayer)      ?.fillColor("#C2D8E8")       // 水體→柔藍
    (style.getLayer("waterway")         as? LineLayer)      ?.lineColor("#C2D8E8")       // 河流/水道
    (style.getLayer("building")         as? FillLayer)      ?.fillColor("#DDD4BA")       // 建築物
    (style.getLayer("building-outline") as? LineLayer)      ?.lineColor("#C8BCA8")       // 建築輪廓
}
