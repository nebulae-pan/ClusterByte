package io.nebula.platform.clusterbyte.converter

import jdk.internal.org.objectweb.asm.ClassVisitor

/**
 * @author xinghai.pan
 *
 * date : 2020-07-17 14:24
 */
class ASMProcessConverter : ClassProcessConverter<ClassVisitor> {
    override fun convertFile(): ClassVisitor {
        TODO("Not yet implemented")
    }

    override fun writeBackFile(t: ClassVisitor) {

    }
}