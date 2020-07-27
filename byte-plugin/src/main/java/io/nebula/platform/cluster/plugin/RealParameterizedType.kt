package io.nebula.platform.cluster.plugin

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * @author xinghai.pan
 *
 * date : 2020-07-27 21:03
 */
class RealParameterizedType(
    private val raw: Class<*>,
    private val generic: Array<Type>
) : ParameterizedType {
    override fun getRawType(): Type {
        return raw
    }

    override fun getOwnerType(): Type? {
        return null
    }

    override fun getActualTypeArguments(): Array<Type> {
        return generic
    }
}