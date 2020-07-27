package io.nebula.platform.clusterbyte.core

import io.nebula.platform.clusterbyte.converter.ConverterFactory

open class ClusterExtension {
    val transforms = arrayListOf<BaseSingularTransform<*>>()
    var factories = arrayListOf<ConverterFactory<*>>()

    fun registerRegularTransform(transform: BaseSingularTransform<*>) {
        transforms.add(transform)
    }

    fun <T> registerClassConverterFactory(factory: ConverterFactory<T>) {
        factories.add(factory)
    }
}