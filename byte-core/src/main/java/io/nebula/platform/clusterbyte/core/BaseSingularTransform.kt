package io.nebula.platform.clusterbyte.core

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.Transform
import java.io.File

/**
 * @author xinghai.pan
 *
 * date : 2020-07-14 16:30
 */
abstract class BaseSingularTransform : Transform() {

    abstract fun traversalDir(dirInput: DirectoryInput)

    abstract fun traversalJar(jarInput: JarInput)

    abstract fun onClassVisited(classFile: File): Boolean

    abstract fun onJarVisited(jarFile: File): Boolean

    override fun isIncremental(): Boolean {
        return true
    }
}