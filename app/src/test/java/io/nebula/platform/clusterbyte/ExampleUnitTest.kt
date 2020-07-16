package io.nebula.platform.clusterbyte

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask
import java.util.concurrent.RecursiveAction
import java.util.concurrent.atomic.AtomicLong

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    var count = 0

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)

        val file = File("/Users")

        var start = System.currentTimeMillis()
        traversalFile(file)
        println("cost: ${System.currentTimeMillis() - start}, count: $count")

        start = System.currentTimeMillis()
//        val pool = ForkJoinPool()
        println(Runtime.getRuntime().availableProcessors())
        val pool = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors()) as ForkJoinPool
        val task = FileTraversalTask(file)
        pool.invoke(task)
//        task.join()
        println("cost ${System.currentTimeMillis() - start}, count: ${parallelCount.get()}")
    }

    private fun traversalFile(baseDir: File) {
        if (baseDir.isFile) {
            return
        }
        baseDir.listFiles()?.forEach {
            if (it.isDirectory) {
                traversalFile(it)
            } else {
                count++
            }
        }
    }


    class FileTraversalTask(private val baseDir: File) : RecursiveAction() {
        override fun compute() {
            val tasks = arrayListOf<FileTraversalTask>()
            baseDir.listFiles()?.forEach {
                if (it.isDirectory) {
                    val subTask = FileTraversalTask(it)
//                    subTask.fork()
                    tasks.add(subTask)
                } else {
                    parallelCount.incrementAndGet()
                }
            }
            ForkJoinTask.invokeAll(tasks)
//            tasks.forEach {
//                count += it.join()
//            }
        }
    }

}

@Volatile
internal var parallelCount = AtomicLong(0)
