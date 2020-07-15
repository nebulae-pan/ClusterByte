package io.nebula.platform.clusterbyte.core

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformInvocationBuilder
import com.android.build.gradle.internal.pipeline.TransformManager

/**
 * @author xinghai.pan
 *
 * date : 2020-07-15 14:50
 */
class ClusterTransform(private val clusterExtension: ClusterExtension) : Transform() {
    override fun transform(transformInvocation: TransformInvocation?) {
        transformInvocation ?: return
        val inputs = transformInvocation.inputs
        clusterExtension.transforms.forEach { transform ->
            inputs.forEach { input ->
                input.directoryInputs.parallelStream().forEach {
                    transform.traversalDir(it)
                }
                input.jarInputs.parallelStream().forEach {
                    transform.traversalJar(it)
                }
            }

            inputs.forEach { input ->
                input.directoryInputs.parallelStream().forEach {
                }
                input.jarInputs.parallelStream().forEach {
                    transform.onJarVisited(it.file)
                }
            }
        }

    }

    override fun getName() = "cluster"

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        val transforms = clusterExtension.transforms
        val set = mutableSetOf<QualifiedContent.ContentType>()
        transforms.forEach {
            set.union(it.inputTypes)
        }
        if (set.isEmpty()) {
            return TransformManager.CONTENT_JARS
        }
        return set
    }

    override fun isIncremental(): Boolean {
        //mandatory require singular transform support incremental
        return true
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        //gather all clustered transform's configuration
        val transforms = clusterExtension.transforms
        val set = mutableSetOf<QualifiedContent.Scope>()
        transforms.forEach {
            set.union(it.scopes)
        }
        if (set.isEmpty()) {
            return TransformManager.SCOPE_FULL_PROJECT
        }
        return set
    }

    override fun getReferencedScopes(): MutableSet<in QualifiedContent.Scope> {
        val transforms = clusterExtension.transforms
        val set = mutableSetOf<QualifiedContent.Scope>()
        transforms.forEach {
            set.union(it.referencedScopes)
        }
        //can be empty
        return set
    }
}