package com.plugin.filters.plugin_filters

import android.content.Context
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class PluginFiltersViewFactory(private val messenger: BinaryMessenger?) :
    PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(p0: Context?, p1: Int, p2: Any?): PlatformView {
        return PluginFiltersView(p0!!, p1, p2, messenger, null)
    }
}