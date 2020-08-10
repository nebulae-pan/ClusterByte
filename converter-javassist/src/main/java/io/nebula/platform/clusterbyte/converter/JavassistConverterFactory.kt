package io.nebula.platform.clusterbyte.converter

import javassist.ClassPool
import javassist.CtClass
import javassist.bytecode.ClassFile
import java.io.ByteArrayInputStream
import java.io.DataInputStream

/**
 * @author xinghai.pan
 *
 * date : 2020-08-03 11:10
 */
class JavassistConverterFactory(private val classPool: ClassPool) : ConverterFactory<CtClass> {

    override fun classConverter(): ClassConverter<ByteArray, CtClass> {
        return object : ClassConverter<ByteArray, CtClass> {
            override fun convert(t: ByteArray): CtClass {
                val classFile = ClassFile(DataInputStream(ByteArrayInputStream(t)))
                classPool.getOrNull(classFile.name)?.detach()
                return classPool.makeClass(classFile)
            }
        }
    }

    override fun tempConverter(): ClassConverter<CtClass, ByteArray?> {
        return object : ClassConverter<CtClass, ByteArray?> {
            override fun convert(t: CtClass): ByteArray? {
                return t.toBytecode()
            }
        }
    }
}