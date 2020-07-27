package io.nebula.platform.clusterbyte.converter

/**
 * @author xinghai.pan
 *
 * date : 2020-07-27 17:46
 */
interface ConverterFactory<T> {
    fun classConverter(): ClassConverter<ByteArray, T>

    fun tempConverter(): ClassConverter<T, ByteArray?>
}