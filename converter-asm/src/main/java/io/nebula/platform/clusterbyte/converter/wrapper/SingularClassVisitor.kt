package io.nebula.platform.clusterbyte.converter.wrapper

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

/**
 * Created by nebula on 2020-07-19
 */
abstract class SingularClassVisitor : ClassVisitor {
    constructor() : super(Opcodes.ASM7)

    constructor(visitor: ClassVisitor) : super(Opcodes.ASM7, visitor)
}