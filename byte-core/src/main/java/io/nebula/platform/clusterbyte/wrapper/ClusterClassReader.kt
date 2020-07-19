package io.nebula.platform.clusterbyte.wrapper

import org.objectweb.asm.ClassVisitor

/**
 * Created by nebula on 2020-07-19
 */
class ClusterVisitorChain(private var lastVisitor: ClassVisitor) {

    fun lastVisitor(): ClassVisitor {
        return this.lastVisitor
    }

    fun processed(visitor: SingularClassVisitor) {
        lastVisitor = visitor
    }
}