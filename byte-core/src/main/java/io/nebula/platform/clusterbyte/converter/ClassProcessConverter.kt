package io.nebula.platform.clusterbyte.converter

/**
 * @author xinghai.pan
 *
 * date : 2020-07-17 10:41
 */
interface ClassProcessConverter<T> {
    fun convertFile(): T

    fun writeBackFile(t: T)
}