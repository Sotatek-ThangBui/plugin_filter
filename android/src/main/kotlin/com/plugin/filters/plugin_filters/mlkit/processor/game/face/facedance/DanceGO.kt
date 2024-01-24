package com.skymeta.arface.mlkit.processor.game.face.facedance

import com.plugin.filters.plugin_filters.mlkit.processor.game.GameObject


class DanceGO : GameObject {
    var matchState = 0f
    var matchScore = 0f

    constructor(): super() {

    }

    constructor(other: GameObject) : super(other) {

    }

    constructor(matchState: Float, matchScore: Float, other: GameObject) : super(other) {

    }

}