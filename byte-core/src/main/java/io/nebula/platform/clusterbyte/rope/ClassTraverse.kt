package io.nebula.platform.clusterbyte.rope

import io.nebula.platform.clusterbyte.concurrent.AsyncHelper
import java.io.File

object ClassTraverse {
    fun traverse(baseDir: File, visitor: FileVisitor) {
        AsyncHelper.forkJoinPool().invoke(ClassTraverseTask(baseDir, visitor))
    }

    fun traverse(baseDir: File, visitor: (File) -> Unit) {
        AsyncHelper.forkJoinPool().invoke(ClassTraverseTask(baseDir, object : FileVisitor {
            override fun onFileVisitor(file: File) {
                visitor.invoke(file)
            }
        }))
    }
}