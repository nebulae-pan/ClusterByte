package io.nebula.platform.clusterbyte.concurrent

import java.util.concurrent.ForkJoinPool

/**
 * @author xinghai.pan
 *
 * date : 2020-07-16 16:33
 */
object AsyncHelper {
    private val parallel = Runtime.getRuntime().availableProcessors()
    private val forkJoinPool = ForkJoinPool()

    fun forkJoinPool(): ForkJoinPool {
        return forkJoinPool
    }
}