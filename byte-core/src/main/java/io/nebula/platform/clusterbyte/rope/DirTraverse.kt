package io.nebula.platform.clusterbyte.rope

import io.nebula.platform.clusterbyte.concurrent.AsyncHelper
import java.io.File

object DirTraverse {
    fun traverse(baseDir: File, visitor: FileVisitor) {
        AsyncHelper.forkJoinPool().invoke(FileTraverseTask(baseDir, visitor))
    }

    fun traverse(baseDir: File, visitor: (File) -> Unit) {
        AsyncHelper.forkJoinPool().invoke(FileTraverseTask(baseDir, object : FileVisitor {
            override fun onFileVisitor(file: File) {
                visitor.invoke(file)
            }
        }))
    }
}