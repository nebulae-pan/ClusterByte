package io.nebula.platform.clusterbyte.core

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import com.android.build.gradle.internal.pipeline.TransformManager
import io.nebula.platform.clusterbyte.wrapper.ClusterVisitorChain
import java.io.File

/**
 * @author xinghai.pan
 *
 * date : 2020-07-14 16:30
 */
abstract class BaseSingularTransform : BaseTransform() {

    abstract fun traversalDir(dirInput: DirectoryInput)

    abstract fun traversalJar(jarInput: JarInput)

    abstract fun onClassVisited(
        status: Status,
        classFile: File,
        chain: ClusterVisitorChain
    ): Boolean

    abstract fun onJarVisited(jarFile: File, status: Status): Boolean

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_JARS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }
}