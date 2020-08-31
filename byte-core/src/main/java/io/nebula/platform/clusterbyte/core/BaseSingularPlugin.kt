package io.nebula.platform.clusterbyte.core

import com.android.build.gradle.BaseExtension
import com.google.common.reflect.TypeToken
import io.nebula.platform.clusterbyte.converter.ConverterFactory
import org.apache.http.util.TextUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import sun.reflect.generics.reflectiveObjects.NotImplementedException
import javax.inject.Inject

/**
 * @author xinghai.pan
 *
 * date : 2020-07-14 16:50
 */
abstract class BaseSingularPlugin<E : BaseSingularExtension> : Plugin<Project> {
    protected var androidExtension: BaseExtension? = null
    protected var clusterExtension: ClusterExtension? = null
    protected lateinit var extension: E

    private lateinit var project: Project

    override fun apply(p: Project) {
        androidExtension = p.extensions.findByType(BaseExtension::class.java)
        clusterExtension = p.extensions.findByType(ClusterExtension::class.java)
        project = p
        extension = createExtension(reflectForExtensionType())
        extension.setParams(p)
        if (!TextUtils.isEmpty(extension.closureName)) {
            project.extensions.add(extension.closureName, extension)
        }
        apply0(p)
    }

    abstract fun apply0(p: Project)

    abstract fun runAlone(): Boolean

    open fun createExtension(clazz: Class<E>): E {
        return getObjectFactory().newInstance(clazz)
    }

    protected fun registerTransform(transform: BaseSingularTransform<*>) {
        transform.setProject(project)
        if (runAlone() || clusterExtension == null) {
            androidExtension?.registerTransform(transform)
            return
        }
        clusterExtension?.registerRegularTransform(transform)
    }

    protected fun <T> registerConverterFactory(factory: ConverterFactory<T>) {
        clusterExtension?.registerClassConverterFactory(factory)
    }

    protected fun getTaskNameWithoutModule(name: String): String {
        return name.substring(name.lastIndexOf(':') + 1)
    }

    protected fun getTaskModuleName(name: String): String {
        val index = name.lastIndexOf(':')
        if (index == -1) {
            return ""
        }
        val str = name.substring(0, index)
        if (str[0] == ':') {
            return str.substring(1, str.length)
        }
        return str
    }

    @Inject
    open fun getObjectFactory(): ObjectFactory {
        throw NotImplementedException()
    }

    private fun reflectForExtensionType(): Class<E> {
        return object : TypeToken<E>(javaClass) {}.rawType as Class<E>
    }
}