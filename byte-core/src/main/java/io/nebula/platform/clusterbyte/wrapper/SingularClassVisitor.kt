package io.nebula.platform.clusterbyte.wrapper

import io.nebula.platform.clusterbyte.rope.DEFAULT_ASM_CODE
import org.objectweb.asm.ClassVisitor

/**
 * Created by nebula on 2020-07-19
 */
class SingularClassVisitor : ClassVisitor {
    constructor() : super(DEFAULT_ASM_CODE)

    constructor(visitor: ClassVisitor) : super(DEFAULT_ASM_CODE, visitor)
}