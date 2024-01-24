package com.plugin.filters.plugin_filters.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.plugin.filters.plugin_filters.enums.FilterType
import com.plugin.filters.plugin_filters.mlkit.processor.game.sticker.BlendConfig


@Entity(tableName = "pins")
data class Pin(
    val filterId: String? = null,
    val human_checked: String? = null,
    @PrimaryKey val id: Long,
    val image: String,
    val title: String,
    val topicId: String? = null,
    val type: String? = null,
    var topicName: String? = null,
    var isLock: Boolean = false,
    var face: FaceDataDetect? = null,
    var tags: Tag? = null,
    var timeSave: Long = 0,
    val filter: FilterType? = FilterType.MASK,
    var blendConfig: BlendConfig? = null
) : MemeObject(url = image, idRelated = id) {
    @Ignore
    val images: List<Image> = arrayListOf()

    @Ignore
    val annotations: List<String>? = null

    val maskType
        get() :String {
            if (filter != null && filter.isCustomFilter) return "Filter"
            return if (id == 0L) "Gallery"
            else "remote"
        }
}


