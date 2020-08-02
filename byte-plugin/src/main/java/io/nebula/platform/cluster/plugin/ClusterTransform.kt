package io.nebula.platform.cluster.plugin

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Status
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import io.nebula.platform.clusterbyte.converter.ClassConverter
import io.nebula.platform.clusterbyte.converter.ConverterFactory
import io.nebula.platform.clusterbyte.core.BaseSingularTransform
import io.nebula.platform.clusterbyte.core.BaseTransform
import io.nebula.platform.clusterbyte.core.ClusterExtension
import io.nebula.platform.clusterbyte.rope.ClassTraverse
import io.nebula.platform.clusterbyte.rope.FileVisitor
import org.apache.commons.io.FileUtils
import java.io.File
import java.lang.reflect.ParameterizedType

/**
 * @author xinghai.pan
 *
 * date : 2020-07-15 14:50
 */
class ClusterTransform(private val clusterExtension: ClusterExtension) : BaseTransform() {
    override fun transform(transformInvocation: TransformInvocation?) {
        transformInvocation ?: return
        val outputProvider = transformInvocation.outputProvider
        val isIncremental = transformInvocation.isIncremental
        if (isIncremental) {
            outputProvider.deleteAll()
        }
        clusterExtension.transforms.forEach {
            it.transform(transformInvocation)
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
                if (isIncremental) {
                    processIncremental(it, srcPath, destPath)
                } else {
                    processFullBuild(it, srcPath, destPath)
                }
            }
            input.jarInputs.parallelStream().forEach {
                val destFile = getOutputJar(outputProvider, it)
                if (isIncremental) {
                    clusterExtension.transforms.forEach { transform ->
                        transform.onJarVisited(it.status, it.file)
                    }
                    if (it.status == Status.REMOVED) {
                        if (destFile.exists()) {
                            FileUtils.forceDelete(destFile)
                        }
                    } else {
                        FileUtils.copyFile(it.file, destFile)
                    }
                } else {
                    clusterExtension.transforms.forEach { transform ->
                        transform.onJarVisited(Status.ADDED, it.file)
                    }
                    FileUtils.copyFile(it.file, destFile)
                }
            }
        }

    }

    private fun processFullBuild(
        it: DirectoryInput,
        srcPath: String,
        destPath: String
    ) {
        ClassTraverse.traverse(it.file, object : FileVisitor {
            override fun onFileVisitor(file: File) {
                transformClassVisitor(file, srcPath, destPath, Status.ADDED)
            }
        })
    }

    private fun processIncremental(
        it: DirectoryInput,
        srcPath: String,
        destPath: String
    ) {
        it.changedFiles.forEach { (file, status) ->
            transformClassVisitor(file, srcPath, destPath, status)
        }
    }

    private fun transformClassVisitor(
        file: File,
        srcPath: String,
        destPath: String,
        status: Status
    ) {
        val destClassFilePath = file.absolutePath.replace(srcPath, destPath)
        val destFile = File(destClassFilePath)

        val transforms = clusterExtension.transforms
        var bytes = file.readBytes()
        var preTemporary: Any? = null
        var consume = false

        for (i in transforms.indices) {
            val it = transforms[i]
            val transform = obtainTransform(it, it.acceptType().cast(null))
            val temporary = preTemporary ?: findClassConverter(it.acceptType()).convert(bytes)
            consume = transform.onClassVisited(status, file, temporary) or consume
            if (consume) {
                break
            }
            if (temporary == null) {
                throw RuntimeException("transform temporary entity is null, please check it")
            }
            if (i < transforms.size - 1 && transforms[i + 1].acceptType() == it.acceptType()) {
                preTemporary = temporary
            } else {
                preTemporary = null
                val converter = findTemporaryConverter(it.acceptType())

                val tempBytes = converter.convert(temporary)
                    ?: throw RuntimeException("file bytes is empty, please check.")

                bytes = tempBytes
            }
        }

        if (!destFile.exists()) {
            FileUtils.forceMkdir(destFile.parentFile)
        }
        destFile.writeBytes(bytes)
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

    @Suppress("UNCHECKED_CAST")
    private fun <T> obtainTransform(
        transform: BaseSingularTransform<*>,
        @Suppress("UNUSED_PARAMETER") t: T
    ): BaseSingularTransform<T> {
        return transform as BaseSingularTransform<T>
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> retrieveFactory(clazz: Class<T>): ConverterFactory<T> {
        clusterExtension.factories.forEach {
            val type =
                (it.javaClass.genericInterfaces[0] as ParameterizedType).actualTypeArguments[0]
            if (clazz == type) {
                return it as ConverterFactory<T>
            }
        }
        throw RuntimeException("cannot find factory for class:${clazz.name}.")
    }

    private fun <T> findClassConverter(clazz: Class<T>): ClassConverter<ByteArray, T> {
        return retrieveFactory(clazz).classConverter()
    }

    private fun <T> findTemporaryConverter(clazz: Class<T>): ClassConverter<Any, ByteArray?> {
        @Suppress("UNCHECKED_CAST")
        return retrieveFactory(clazz).tempConverter() as ClassConverter<Any, ByteArray?>
    }
}
