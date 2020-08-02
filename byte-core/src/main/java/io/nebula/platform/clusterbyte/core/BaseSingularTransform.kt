package io.nebula.platform.clusterbyte.core

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import java.io.File
import java.lang.RuntimeException

/**
 * @author xinghai.pan
 *
 * date : 2020-07-14 16:30
 */
abstract class BaseSingularTransform<T> : BaseTransform() {
    private var transformInvocation: TransformInvocation? = null
    private var customOutputDir: File? = null

    abstract fun traversalDir(dirInput: DirectoryInput)

    abstract fun traversalJar(jarInput: JarInput)

    abstract fun onClassVisited(
        status: Status,
        classFile: File,
        classEntity: T
    ): Boolean

    abstract fun onJarVisited(status: Status, jarFile: File): Boolean

    abstract fun acceptType(): Class<T>

    @Synchronized
    fun newOutputClass(fileName: String): File {
        val transformInvocation = this.transformInvocation
        transformInvocation
            ?: throw RuntimeException("please call newOutputClass() when transform() callee")
        if (customOutputDir == null) {
            customOutputDir = transformInvocation.outputProvider.getContentLocation(
                name,
                TransformManager.CONTENT_CLASS,
                TransformManager.PROJECT_ONLY,
                Format.DIRECTORY
            )
        }
        return File(customOutputDir, fileName)
    }

    override fun transform(transformInvocation: TransformInvocation?) {
        this.transformInvocation = transformInvocation
        super.transform(transformInvocation)
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_JARS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

}