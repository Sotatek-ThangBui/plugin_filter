package com.plugin.filters.plugin_filters.mlkit.processor.game

class PointF3D(
    var x:Float, var y:Float, var z:Float
) {

    operator fun plus(other: PointF3D) = PointF3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: PointF3D) = PointF3D(x - other.x, y - other.y, z - other.z)
    operator fun div(other: PointF3D) = PointF3D(x / other.x, y / other.y, z / other.z)
    operator fun div(other: Float) = PointF3D(x / other, y / other, z / other)
    operator fun times(other: PointF3D) = PointF3D(x * other.x, y * other.y, z * other.z)
}