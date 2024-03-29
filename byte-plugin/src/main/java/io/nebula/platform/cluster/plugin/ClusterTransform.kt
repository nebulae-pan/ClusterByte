package io.nebula.platform.cluster.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import io.nebula.platform.clusterbyte.converter.ClassConverter
import io.nebula.platform.clusterbyte.converter.ConverterFactory
import io.nebula.platform.clusterbyte.core.BaseSliceTransform
import io.nebula.platform.clusterbyte.core.BaseTransform
import io.nebula.platform.clusterbyte.core.ClusterExtension
import io.nebula.platform.clusterbyte.rope.ClassTraverse
import io.nebula.platform.clusterbyte.rope.FileVisitor
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import java.io.File
import java.lang.reflect.ParameterizedType
import java.util.zip.ZipFile

/**
 * @author xinghai.pan
 *
 * date : 2020-07-15 14:50
 */
class ClusterTransform(
    private val project: Project,
    private val clusterExtension: ClusterExtension
) : BaseTransform() {
    override fun transform(transformInvocation: TransformInvocation?) {
        transformInvocation ?: return
        val outputProvider = transformInvocation.outputProvider
        val isIncremental = transformInvocation.isIncremental
        if (!isIncremental) {
            outputProvider.deleteAll()
        }
        val transforms = clusterExtension.transforms
        transforms.forEach {
            it.preTransform(transformInvocation)
            it.transform(transformInvocation)
        }
        val inputs = transformInvocation.inputs
        val androidExtension = project.extensions.getByName("android") as BaseExtension
        val androidJar = File(
            androidExtension.sdkDirectory,
            "platforms/${androidExtension.compileSdkVersion}/android.jar"
        )
        val isApplication = androidExtension is AppExtension
        transforms.forEach {
            it.traversalJar(androidJar)
        }
        inputs.forEach { input ->
            input.directoryInputs.parallelStream().forEach {
                transforms.forEach { transform ->
                    transform.traversalDir(it)
                }
            }
            input.jarInputs.parallelStream().forEach {
                transforms.forEach { transform ->
                    transform.traversalJar(it.file)
                }
            }
        }

        inputs.forEach { input ->
            input.directoryInputs.parallelStream().forEach {
                val srcPath = it.file.absolutePath
                val destPath = getOutputDir(outputProvider, it).absolutePath
                if (isIncremental) {
                    processIncrementalDir(it, srcPath, destPath)
                } else {
                    processFullBuildDir(it, srcPath, destPath)
                }
            }
            input.jarInputs.parallelStream().forEach {
                val destJar = getOutputJar(outputProvider, it)
                val destDir = outputProvider.getContentLocation(
                    it.name + "expanded",
                    it.contentTypes,
                    it.scopes,
                    Format.DIRECTORY
                )
                if (isIncremental) {
                    processIncrementalJar(isApplication, transforms, it, destJar, destDir)
                } else {
                    processFullBuildJar(isApplication, it, transforms, destJar, destDir)
                }
            }
        }
        //after transform execution
        transforms.forEach {
            it.postTransform(transformInvocation)
        }
    }

    private fun processFullBuildJar(
        isApplication: Boolean,
        jarInput: JarInput,
        transforms: ArrayList<BaseSliceTransform<*>>,
        destJar: File,
        destDir: File
    ) {
        if (isApplication
            && jarInput.scopes.size == 1
            && jarInput.scopes.contains(QualifiedContent.Scope.SUB_PROJECTS)
        ) {
            unzipAndTraverseClasses(jarInput, Status.ADDED, destDir)
        } else {
            var consume = false
            transforms.forEach { transform ->
                consume = transform.onJarVisited(Status.ADDED, jarInput.file)
                if (consume) {
                    return@forEach
                }
            }
            if (!consume) {
                FileUtils.copyFile(jarInput.file, destJar)
            }
        }
    }

    private fun processIncrementalJar(
        isApplication: Boolean,
        transforms: ArrayList<BaseSliceTransform<*>>,
        jarInput: JarInput,
        destJar: File,
        destDir: File
    ) {
        if (isApplication
            && jarInput.scopes.size == 1
            && jarInput.scopes.contains(QualifiedContent.Scope.SUB_PROJECTS)
        ) {
            unzipAndTraverseClasses(jarInput, jarInput.status, destDir)
        } else {
            var consume = false
            transforms.forEach { transform ->
                consume = transform.onJarVisited(jarInput.status, jarInput.file)
                if (consume) {
                    return@forEach
                }
            }
            if (consume || jarInput.status == Status.REMOVED) {
                if (destJar.exists()) {
                    FileUtils.forceDelete(destJar)
                }
            } else {
                FileUtils.copyFile(jarInput.file, destJar)
            }
        }
    }

    private fun unzipAndTraverseClasses(
        jarInput: JarInput,
        status: Status,
        destDir: File
    ) {

        val zip = ZipFile(jarInput.file)
        zip.entries().iterator().forEach {
            if (!it.name.endsWith(".class")) {
                return@forEach
            }
            val classFile = File(destDir, it.name)
            zip.getInputStream(it).use { input ->
                val bytes = transformsDeliverBytes(input.readBytes(), status)
                //bytes is null, transform consume it, shouldn't deliver this file to next transform flow
                if (bytes == null) {
                    if (classFile.exists()) {
                        FileUtils.forceDelete(classFile)
                    }
                } else {
                    if (!classFile.exists()) {
                        FileUtils.forceMkdir(classFile.parentFile)
                    }
                    classFile.writeBytes(bytes)
                }
            }
        }
        zip.close()
    }

    private fun processFullBuildDir(
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

    private fun processIncrementalDir(
        it: DirectoryInput,
        srcPath: String,
        destPath: String
    ) {
        it.changedFiles.forEach { (file, status) ->
            if (status != Status.REMOVED && file.isFile) {
                transformClassVisitor(file, srcPath, destPath, status)
            }
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

        var bytes: ByteArray? = file.readBytes()
        bytes = transformsDeliverBytes(bytes, status)
        //bytes is null, transform consume it, shouldn't deliver this file to next transform flow
        if (bytes == null) {
            if (destFile.exists()) {
                FileUtils.forceDelete(destFile)
            }
        } else {
            if (!destFile.exists()) {
                FileUtils.forceMkdir(destFile.parentFile)
            }
            destFile.writeBytes(bytes)
        }
    }

    private fun transformsDeliverBytes(
        bytes: ByteArray?,
        status: Status
    ): ByteArray? {
        if (bytes == null) {
            return null
        }
        var resultBytes: ByteArray? = bytes
        val transforms = clusterExtension.transforms
        var preTemporary: Any? = null
        var consume = false

        for (i in transforms.indices) {
            if (resultBytes == null) {
                return null
            }
            val it = transforms[i]
            val transform = obtainTransform(it, it.acceptType().cast(null))
            val temporary = preTemporary ?: findClassConverter(it.acceptType()).convert(resultBytes)
            consume = transform.onClassVisited(status, temporary) or consume
            if (consume) {
                resultBytes = null
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

                resultBytes = tempBytes
            }
        }
        return resultBytes
    }

    override fun getName() = "cluster"

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        val transforms = clusterExtension.transforms
        val set = mutableSetOf<QualifiedContent.ContentType>()
        transforms.forEach {
            set.addAll(it.inputTypes)
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
        transforms.forEach { transform ->
            set.addAll(transform.scopes.map { it as QualifiedContent.Scope })
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
        transform: BaseSliceTransform<*>,
        @Suppress("UNUSED_PARAMETER") t: T
    ): BaseSliceTransform<T> {
        return transform as BaseSliceTransform<T>
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
