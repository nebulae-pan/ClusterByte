package io.nebula.platform.clusterbyte.converter.asm

import io.nebula.platform.clusterbyte.converter.ClassConverter
import io.nebula.platform.clusterbyte.converter.ConverterFactory
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

/**
 * @author xinghai.pan
 *
 * date : 2020-07-27 18:49
 */
class AsmConverterFactory : ConverterFactory<ClassVisitor> {
    private var classReader: ClassReader? = null
    private var classWriter: ClassWriter? = null
    override fun classConverter(): ClassConverter<ByteArray, ClassVisitor> {
        return object : ClassConverter<ByteArray, ClassVisitor> {
            override fun convert(t: ByteArray): ClassVisitor {
                classReader = ClassReader(t)
                classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
                return classWriter!!
            }
        }
    }

    override fun tempConverter(): ClassConverter<ClassVisitor, ByteArray?> {
        return object : ClassConverter<ClassVisitor, ByteArray?> {
            override fun convert(t: ClassVisitor): ByteArray? {
                classReader?.accept(t, ClassReader.EXPAND_FRAMES)
                if (classWriter == null) {
                    return null
                }
                return classWriter?.toByteArray()
            }
        }
    }
}