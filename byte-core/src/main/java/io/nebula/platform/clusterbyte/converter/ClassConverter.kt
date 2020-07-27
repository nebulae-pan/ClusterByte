package io.nebula.platform.clusterbyte.converter

/**
 * @author xinghai.pan
 *
 * date : 2020-07-17 10:41
 */
interface ClassConverter<T, R> {
    fun convert(t: T): R
}