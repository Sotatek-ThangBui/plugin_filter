package com.plugin.filters.plugin_filters.extension

import com.plugin.filters.plugin_filters.model.Tag


fun Tag?.isMask(): Boolean {
    return !(this != null && type.isNotEmpty() && type.indexOfFirst { subTag -> subTag.name == "mask" } == -1)
}