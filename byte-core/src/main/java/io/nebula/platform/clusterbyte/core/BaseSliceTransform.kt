package io.nebula.platform.clusterbyte.core

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.reflect.TypeToken
import org.gradle.api.Project
import java.io.File
import java.lang.RuntimeException

/**
 * @author xinghai.pan
 *
 * date : 2020-07-14 16:30
 */
@Suppress("unused")
abstract class BaseSliceTransform<T> : BaseTransform() {
    private var customOutputDir: File? = null
    private var project: Project? = null

    abstract fun traversalDir(dirInput: DirectoryInput)

    abstract fun traversalJar(jarFile: File)

    abstract fun onClassVisited(
        status: Status,
        classEntity: T
    ): Boolean

    abstract fun onJarVisited(status: Status, jarFile: File): Boolean

    fun acceptType(): Class<T> {
        return object : TypeToken<T>(javaClass) {}.rawType as Class<T>
    }

    open fun preTransform(transformInvocation: TransformInvocation?) {

    }

    open fun postTransform(transformInvocation: TransformInvocation?) {

    }

    fun newOutputClass(fileName: String): File {
        return File(customOutputDir, fileName)
    }

    fun setProject(project: Project) {
        this.project = project
    }

    open fun buildDir(): File {
        val p = project ?: throw RuntimeException("project is null")
        return File(p.buildDir, "cluster/$name")
    }

    override fun transform(transformInvocation: TransformInvocation?) {
        if (transformInvocation == null) {
            super.transform(transformInvocation)
            return
        }
        customOutputDir = transformInvocation.outputProvider.getContentLocation(
            name,
            TransformManager.CONTENT_CLASS,
            TransformManager.PROJECT_ONLY,
            Format.DIRECTORY
        )
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_JARS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

}