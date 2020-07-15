package io.nebula.platform.clusterbyte.core

class ClusterExtension {
    val transforms = arrayListOf<BaseSingularTransform>()

    fun registerRegularTransform(transform: BaseSingularTransform) {
        transforms.add(transform)
    }
}