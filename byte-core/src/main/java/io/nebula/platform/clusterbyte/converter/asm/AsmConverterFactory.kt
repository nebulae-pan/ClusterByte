package io.nebula.platform.clusterbyte.converter.asm

import io.nebula.platform.clusterbyte.converter.ClassConverter
import io.nebula.platform.clusterbyte.converter.ConverterFactory
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

/**
 * @author xinghai.pan
 *
 * date : 2020-07-27 18:49
 */
class AsmConverterFactory : ConverterFactory<VisitorChain> {
    override fun classConverter(): ClassConverter<ByteArray, VisitorChain> {
        return object : ClassConverter<ByteArray, VisitorChain> {
            override fun convert(t: ByteArray): VisitorChain {
                val classReader = ClassReader(t)
                val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
                return VisitorChain(classReader, classWriter)
            }
        }
    }

    override fun tempConverter(): ClassConverter<VisitorChain, ByteArray?> {
        return object : ClassConverter<VisitorChain, ByteArray?> {
            override fun convert(t: VisitorChain): ByteArray? {
                return t.toByteArray()
            }
        }
    }
}