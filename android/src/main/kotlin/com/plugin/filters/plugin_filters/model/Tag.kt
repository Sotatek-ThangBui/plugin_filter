package com.plugin.filters.plugin_filters.model

data class Tag(val type: ArrayList<SubTag>, val topic: ArrayList<SubTag>)

data class SubTag(var name: String, val value: ArrayList<String>): java.io.Serializable