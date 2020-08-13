package io.nebula.platform.clusterbyte.converter.wrapper

import io.nebula.platform.clusterbyte.converter.DEFAULT_ASM_CODE
import org.objectweb.asm.ClassVisitor

/**
 * Created by nebula on 2020-07-19
 */
abstract class SingularClassVisitor : ClassVisitor {
    constructor() : super(DEFAULT_ASM_CODE)

    constructor(visitor: ClassVisitor) : super(DEFAULT_ASM_CODE, visitor)
}