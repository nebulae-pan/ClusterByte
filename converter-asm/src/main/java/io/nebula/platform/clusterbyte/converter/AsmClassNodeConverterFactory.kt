package io.nebula.platform.clusterbyte.converter

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

/**
 * @author xinghai.pan
 *
 * date : 2020-07-29 09:45
 */
class AsmClassNodeConverterFactory : ConverterFactory<ClassNode> {
    override fun classConverter(): ClassConverter<ByteArray, ClassNode> {
        return object : ClassConverter<ByteArray, ClassNode> {
            override fun convert(t: ByteArray): ClassNode {
                val classReader = ClassReader(t)
                val classNode = ClassNode()
                classReader.accept(classNode, ClassReader.EXPAND_FRAMES)
                return classNode
            }

        }
    }

    override fun tempConverter(): ClassConverter<ClassNode, ByteArray?> {
        return object : ClassConverter<ClassNode, ByteArray?> {
            override fun convert(t: ClassNode): ByteArray? {
                val classWrite = ClassWriter(ClassWriter.COMPUTE_MAXS)
                t.accept(classWrite)
                return classWrite.toByteArray()
            }
        }
    }
}