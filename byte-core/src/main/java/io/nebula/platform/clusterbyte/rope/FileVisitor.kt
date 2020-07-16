package io.nebula.platform.clusterbyte.rope

import java.io.File

/**
 * @author xinghai.pan
 *
 * date : 2020-07-16 14:36
 */
interface FileVisitor {
    fun onFileVisitor(file: File)
}