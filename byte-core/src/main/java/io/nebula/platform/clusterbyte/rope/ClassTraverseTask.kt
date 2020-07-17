package io.nebula.platform.clusterbyte.rope

import java.io.File
import java.util.concurrent.ForkJoinTask
import java.util.concurrent.RecursiveAction

/**
 * @author xinghai.pan
 *
 * date : 2020-07-16 14:41
 */
class ClassTraverseTask(
    private val baseDir: File,
    private val visitor: FileVisitor
) : RecursiveAction() {
    override fun compute() {
        val tasks = arrayListOf<RecursiveAction>()
        baseDir.listFiles()?.forEach {
            if (it.isDirectory) {
                ClassTraverseTask(it, visitor).apply {
                    tasks.add(this)
                }
            } else if (it.name.endsWith(".class")) {
                visitor.onFileVisitor(it)
            }
        }
        ForkJoinTask.invokeAll(tasks)
    }
}