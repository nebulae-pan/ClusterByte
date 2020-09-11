package io.nebula.platform.clusterbyte.core

import io.nebula.platform.clusterbyte.converter.ConverterFactory

open class ClusterExtension {
    val transforms = arrayListOf<BaseSliceTransform<*>>()
    val factories = hashSetOf<ConverterFactory<*>>()

    fun registerRegularTransform(transform: BaseSliceTransform<*>) {
        transforms.add(transform)
    }

    fun <T> registerClassConverterFactory(factory: ConverterFactory<T>) {
        factories.add(factory)
    }
}