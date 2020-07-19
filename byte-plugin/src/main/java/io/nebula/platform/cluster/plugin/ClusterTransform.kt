package io.nebula.platform.cluster.plugin

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import io.nebula.platform.clusterbyte.core.BaseTransform
import io.nebula.platform.clusterbyte.core.ClusterExtension
import io.nebula.platform.clusterbyte.rope.ClassTraverse
import io.nebula.platform.clusterbyte.rope.FileVisitor
import io.nebula.platform.clusterbyte.wrapper.ClusterVisitorChain
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File

/**
 * @author xinghai.pan
 *
 * date : 2020-07-15 14:50
 */
class ClusterTransform(private val clusterExtension: ClusterExtension) : BaseTransform() {
    override fun transform(transformInvocation: TransformInvocation?) {
        transformInvocation ?: return
        val outputProvider = transformInvocation.outputProvider
        if (!transformInvocation.isIncremental) {
            outputProvider.deleteAll()
        }
        val inputs = transformInvocation.inputs
        inputs.forEach { input ->
            input.directoryInputs.parallelStream().forEach {
                clusterExtension.transforms.forEach { transform ->
                    transform.traversalDir(it)
                }
            }
            input.jarInputs.parallelStream().forEach {
                clusterExtension.transforms.forEach { transform ->
                    transform.traversalJar(it)
                }
            }
        }
        inputs.forEach { input ->
            input.directoryInputs.parallelStream().forEach {
                val srcPath = it.file.absolutePath
                val destPath = getOutputDir(outputProvider, it).absolutePath

                ClassTraverse.traverse(it.file, object : FileVisitor {
                    override fun onFileVisitor(file: File) {
                        val destClassFilePath = file.absolutePath.replace(srcPath, destPath)
                        val destFile = File(destClassFilePath)
                        val classReader = ClassReader(file.inputStream())
                        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
                        val chain = ClusterVisitorChain(classWriter)
                        clusterExtension.transforms.forEach trans@{ transform ->
                            if (transform.onClassVisited(file, chain)) {
                                return@trans
                            }
                        }
                        classReader.accept(chain.lastVisitor(), ClassReader.EXPAND_FRAMES)
                        if (!destFile.exists()) {
                            FileUtils.forceMkdir(destFile.parentFile)
                        }
                        destFile.writeBytes(classWriter.toByteArray())
                    }
                })
            }
            input.jarInputs.parallelStream().forEach {
                clusterExtension.transforms.forEach { transform ->
                    transform.onJarVisited(it.file)
                }
                val destFile = getOutputJar(outputProvider, it)
                FileUtils.copyFile(it.file, destFile)
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