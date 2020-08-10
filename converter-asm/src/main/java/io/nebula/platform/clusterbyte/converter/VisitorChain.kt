package io.nebula.platform.clusterbyte.converter

import io.nebula.platform.clusterbyte.converter.wrapper.SingularClassVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

/**
 * Created by nebula on 2020-07-19
 */
class VisitorChain(
    private val reader: ClassReader,
    private val classWriter: ClassWriter
) {
    private var lastVisitor: ClassVisitor = classWriter

    fun lastVisitor(): ClassVisitor {
        return this.lastVisitor
    }

    fun processed(visitor: SingularClassVisitor) {
        lastVisitor = visitor
    }

    fun toByteArray(): ByteArray {
        reader.accept(lastVisitor, ClassReader.EXPAND_FRAMES)
        return classWriter.toByteArray()
    }
}